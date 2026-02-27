package wtf.faceac.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.reactivex.rxjava3.core.Observable;
import okhttp3.*;
import wtf.faceac.Main;
import wtf.faceac.data.TickData;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.logging.Logger;

/**
 * HTTP client for the FACE Dashboard backend ({@code /api/checks/submit}).
 * <p>
 * Authenticates via {@code X-API-Key} header (server API key from the dashboard).
 * Sends player movement features as a JSON 2-D array and receives a probability + action verdict.
 */
public class FaceBackendClient implements IAIClient {

    /* ───────── Constants ───────── */
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");
    private static final int MAX_INFLIGHT_GLOBAL = 256;
    private static final int MAX_PAYLOAD_TICKS = 200;
    private static final int RETRY_JITTER_MS = 100;
    private static final int CIRCUIT_OPEN_AFTER = 20;
    private static final int CIRCUIT_HALF_OPEN_AFTER_MS = 15_000;
    private static final int CIRCUIT_CLOSE_AFTER = 3;
    private static final long ERROR_LOG_WINDOW_MS = 30_000L;
    private static final Set<Integer> RETRYABLE_HTTP = Set.of(408, 429, 500, 502, 503, 504);

    private static final String OK             = "FACE_OK";
    private static final String AUTH_FAILED    = "FACE_AUTH_FAILED";
    private static final String EXPIRED        = "FACE_EXPIRED";
    private static final String RATE_LIMITED   = "FACE_RATE_LIMITED";
    private static final String TIMEOUT        = "FACE_TIMEOUT";
    private static final String SERVER_ERROR   = "FACE_SERVER_ERROR";
    private static final String NETWORK_ERROR  = "FACE_NETWORK_ERROR";
    private static final String INVALID_RESP   = "FACE_INVALID_RESPONSE";
    private static final String CIRCUIT_OPEN   = "FACE_CIRCUIT_OPEN";
    private static final String UNKNOWN_ERROR  = "FACE_UNKNOWN_ERROR";

    private enum CircuitState { CLOSED, OPEN, HALF_OPEN }

    /* ───────── Fields ───────── */
    private final Main plugin;
    private final Logger logger;
    private final String submitUrl;       // e.g. https://svo-host.ru/api/checks/submit
    private final String healthUrl;       // e.g. https://svo-host.ru/health
    private final String apiKey;
    private final String apiKeyMasked;
    private final String userAgent;
    private final int retryAttempts;
    private final int retryBackoffMs;
    private final boolean debug;
    private final boolean healthcheckEnabled;
    private final OkHttpClient httpClient;

    private final Semaphore globalInflight = new Semaphore(MAX_INFLIGHT_GLOBAL);
    private final Set<String> playerInflight = ConcurrentHashMap.newKeySet();
    private final Map<String, Long> errorLogThrottle = new ConcurrentHashMap<>();

    private final AtomicBoolean connected    = new AtomicBoolean(false);
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);
    private final AtomicBoolean authFailed   = new AtomicBoolean(false);
    private final AtomicBoolean rateLimited  = new AtomicBoolean(false);
    private final AtomicInteger inflightNow  = new AtomicInteger(0);

    private volatile CircuitState circuitState = CircuitState.CLOSED;
    private volatile long circuitOpenUntilMs = 0L;
    private final AtomicInteger consecutiveFails  = new AtomicInteger(0);
    private final AtomicInteger halfOpenSuccesses = new AtomicInteger(0);

    private final LongAdder metricTotal    = new LongAdder();
    private final LongAdder metricSuccess  = new LongAdder();
    private final LongAdder metricFail     = new LongAdder();
    private final LongAdder metricRetries  = new LongAdder();

    /* ───────── Constructor ───────── */
    public FaceBackendClient(Main plugin, String baseUrl, String apiKey,
                             int timeoutMs, int retryAttempts, int retryBackoffMs,
                             boolean healthcheckEnabled, boolean debug) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();

        // Normalise base URL and build endpoints
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.submitUrl = base + "/api/checks/submit";
        this.healthUrl = base + "/health";

        this.apiKey       = apiKey == null ? "" : apiKey.trim();
        this.apiKeyMasked = maskToken(this.apiKey);
        this.userAgent    = "faceac/" + plugin.getDescription().getVersion();
        this.retryAttempts   = Math.max(0, retryAttempts);
        this.retryBackoffMs  = Math.max(0, retryBackoffMs);
        this.debug           = debug;
        this.healthcheckEnabled = healthcheckEnabled;

        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .writeTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .callTimeout((long) timeoutMs * (Math.max(1, this.retryAttempts + 1)), TimeUnit.MILLISECONDS)
                .build();

        logger.info("[FACE] Backend submit URL: " + this.submitUrl);
    }

    /* ═══════════════ IAIClient implementation ═══════════════ */

    @Override
    public CompletableFuture<Boolean> connect() {
        shuttingDown.set(false);
        connected.set(false);
        authFailed.set(false);
        rateLimited.set(false);
        resetCircuit();

        if (!healthcheckEnabled) {
            connected.set(true);
            return CompletableFuture.completedFuture(true);
        }
        return performHealthcheck().thenApply(ok -> { connected.set(ok); return ok; });
    }

    @Override
    public CompletableFuture<Boolean> connectWithRetry() {
        return connect();
    }

    @Override
    public CompletableFuture<Void> disconnect() {
        shuttingDown.set(true);
        connected.set(false);
        playerInflight.clear();
        inflightNow.set(0);
        return CompletableFuture.runAsync(() -> httpClient.dispatcher().cancelAll());
    }

    @Override
    public Observable<AIResponse> predict(byte[] playerData, String playerUuid, String playerName) {
        return Observable.fromFuture(predictAsync(playerData, playerUuid, playerName));
    }

    /**
     * {@code playerData} here is <b>NOT</b> the binary matrix any more.
     * Instead we receive the raw byte-array that is actually a UTF-8 encoded JSON features array
     * produced by {@link FlatBufferSerializer#serializeJsonFeatures(List, int)}.
     */
    @Override
    public CompletableFuture<AIResponse> predictAsync(byte[] playerData, String playerUuid, String playerName) {
        if (shuttingDown.get() || !connected.get())
            return done(errResp(NETWORK_ERROR, "client not connected"));
        if (authFailed.get())
            return done(errResp(AUTH_FAILED, "auth failed — check API key"));
        if (!circuitAllows())
            return done(errResp(CIRCUIT_OPEN, "circuit breaker open"));
        if (playerData == null || playerData.length == 0)
            return done(errResp(INVALID_RESP, "empty payload"));

        String normalized = playerName == null || playerName.isBlank() ? "unknown" : playerName;
        if (!playerInflight.add(normalized))
            return done(errResp(RATE_LIMITED, "already in-flight for " + normalized));

        if (!globalInflight.tryAcquire()) {
            playerInflight.remove(normalized);
            return done(errResp(RATE_LIMITED, "global inflight limit"));
        }
        inflightNow.incrementAndGet();
        metricTotal.increment();

        // Build JSON body with online player count from Bukkit
        int onlineCount = plugin.getServer().getOnlinePlayers().size();
        String featuresJson = new String(playerData, java.nio.charset.StandardCharsets.UTF_8);
        String body = "{\"player\":\"" + escapeJson(normalized) + "\","
                    + "\"features\":" + featuresJson + ","
                    + "\"suspected_cheat\":\"killaura\","
                    + "\"online_count\":" + onlineCount + "}";

        CompletableFuture<AIResponse> future = new CompletableFuture<>();
        executeWithRetry(body, normalized, 0, System.nanoTime(), future);
        future.whenComplete((r, t) -> {
            playerInflight.remove(normalized);
            globalInflight.release();
            inflightNow.decrementAndGet();
        });
        return future;
    }

    @Override public boolean isConnected()      { return connected.get() && !shuttingDown.get() && !authFailed.get(); }
    @Override public boolean isLimitExceeded()   { return rateLimited.get(); }
    @Override public String  getSessionId()      { return null; }
    @Override public String  getServerAddress()  { return submitUrl; }

    /* ═══════════════ HTTP execution with retry ═══════════════ */

    private void executeWithRetry(String bodyJson, String player, int attempt, long startNs, CompletableFuture<AIResponse> future) {
        Request request = new Request.Builder()
                .url(submitUrl)
                .post(RequestBody.create(bodyJson, JSON_MEDIA_TYPE))
                .header("X-API-Key", apiKey)
                .header("User-Agent", userAgent)
                .header("X-Request-Id", UUID.randomUUID().toString())
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                NormalizedError ne = classifyIOError(e);
                if (ne.retryable && attempt < retryAttempts) {
                    metricRetries.increment();
                    long delay = (long) retryBackoffMs * (attempt + 1)
                               + ThreadLocalRandom.current().nextInt(0, RETRY_JITTER_MS + 1);
                    CompletableFuture.delayedExecutor(delay, TimeUnit.MILLISECONDS)
                            .execute(() -> executeWithRetry(bodyJson, player, attempt + 1, startNs, future));
                    return;
                }
                recordFailure();
                metricFail.increment();
                throttledLog(ne.code, "[FACE] " + ne.code + " | player=" + player + " | " + ne.message);
                future.complete(errResp(ne.code, ne.message));
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                try (response) {
                    int code = response.code();
                    String responseBody = response.body() != null ? response.body().string() : "";

                    if (code == 401) {
                        authFailed.set(true);
                        metricFail.increment();
                        recordFailure();
                        throttledLog(AUTH_FAILED, "[FACE] Invalid API key (401)");
                        future.complete(errResp(AUTH_FAILED, "Invalid API key"));
                        return;
                    }
                    if (code == 403) {
                        metricFail.increment();
                        recordFailure();
                        throttledLog(EXPIRED, "[FACE] Server license expired (403)");
                        future.complete(errResp(EXPIRED, "Server license expired"));
                        return;
                    }
                    if (code == 429) {
                        rateLimited.set(true);
                        metricFail.increment();
                        recordFailure();
                        future.complete(errResp(RATE_LIMITED, "Rate limited (429)"));
                        return;
                    }
                    if (RETRYABLE_HTTP.contains(code) && attempt < retryAttempts) {
                        metricRetries.increment();
                        long delay = (long) retryBackoffMs * (attempt + 1)
                                   + ThreadLocalRandom.current().nextInt(0, RETRY_JITTER_MS + 1);
                        CompletableFuture.delayedExecutor(delay, TimeUnit.MILLISECONDS)
                                .execute(() -> executeWithRetry(bodyJson, player, attempt + 1, startNs, future));
                        return;
                    }
                    if (code != 200 && code != 201) {
                        metricFail.increment();
                        recordFailure();
                        future.complete(errResp(SERVER_ERROR, "HTTP " + code + ": " + truncate(responseBody, 120)));
                        return;
                    }

                    // Success — parse {"probability": 0.95, "cheat_type": "killaura", "action": "banned"}
                    AIResponse parsed = parseResponse(responseBody);
                    if (parsed == null) {
                        metricFail.increment();
                        recordFailure();
                        future.complete(errResp(INVALID_RESP, "unparseable response: " + truncate(responseBody, 120)));
                        return;
                    }
                    metricSuccess.increment();
                    recordSuccess();
                    if (debug) {
                        long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);
                        logger.info("[FACE] OK | player=" + player + " | prob=" + String.format("%.3f", parsed.getProbability())
                                + " | action=" + parsed.getAction() + " | " + ms + "ms");
                    }
                    future.complete(parsed);
                }
            }
        });
    }

    /* ═══════════════ Response parsing ═══════════════ */

    private AIResponse parseResponse(String json) {
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            double probability = obj.has("probability") ? obj.get("probability").getAsDouble() : 0.0;
            String cheatType   = obj.has("cheat_type")  ? obj.get("cheat_type").getAsString()  : "unknown";
            String action      = obj.has("action")      ? obj.get("action").getAsString()      : "watching";
            return new AIResponse(probability, null, cheatType, action);
        } catch (Exception e) {
            return null;
        }
    }

    /* ═══════════════ Healthcheck ═══════════════ */

    private CompletableFuture<Boolean> performHealthcheck() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Request req = new Request.Builder()
                .url(healthUrl)
                .get()
                .header("User-Agent", userAgent)
                .build();
        httpClient.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call c, IOException e) {
                logger.warning("[FACE] Healthcheck failed: " + e.getMessage());
                future.complete(false);
            }
            @Override public void onResponse(Call c, Response r) {
                boolean ok = r.isSuccessful();
                r.close();
                if (!ok) logger.warning("[FACE] Healthcheck returned HTTP " + r.code());
                future.complete(ok);
            }
        });
        return future;
    }

    /* ═══════════════ Circuit Breaker ═══════════════ */

    private boolean circuitAllows() {
        switch (circuitState) {
            case CLOSED:   return true;
            case OPEN:
                if (System.currentTimeMillis() >= circuitOpenUntilMs) {
                    circuitState = CircuitState.HALF_OPEN;
                    halfOpenSuccesses.set(0);
                    return true;
                }
                return false;
            case HALF_OPEN: return true;
            default: return true;
        }
    }

    private void recordSuccess() {
        consecutiveFails.set(0);
        if (circuitState == CircuitState.HALF_OPEN) {
            if (halfOpenSuccesses.incrementAndGet() >= CIRCUIT_CLOSE_AFTER) {
                circuitState = CircuitState.CLOSED;
                logger.info("[FACE] Circuit breaker CLOSED after recovery");
            }
        }
    }

    private void recordFailure() {
        int fails = consecutiveFails.incrementAndGet();
        if (circuitState == CircuitState.HALF_OPEN) {
            circuitState = CircuitState.OPEN;
            circuitOpenUntilMs = System.currentTimeMillis() + CIRCUIT_HALF_OPEN_AFTER_MS;
            return;
        }
        if (fails >= CIRCUIT_OPEN_AFTER && circuitState == CircuitState.CLOSED) {
            circuitState = CircuitState.OPEN;
            circuitOpenUntilMs = System.currentTimeMillis() + CIRCUIT_HALF_OPEN_AFTER_MS;
            logger.warning("[FACE] Circuit breaker OPEN after " + fails + " consecutive failures");
        }
    }

    private void resetCircuit() {
        circuitState = CircuitState.CLOSED;
        consecutiveFails.set(0);
        halfOpenSuccesses.set(0);
    }

    /* ═══════════════ Helpers ═══════════════ */

    private NormalizedError classifyIOError(IOException e) {
        if (e instanceof SocketTimeoutException || e instanceof InterruptedIOException)
            return new NormalizedError(TIMEOUT, -1, "timeout: " + e.getMessage(), true);
        if (e instanceof ConnectException || e instanceof UnknownHostException)
            return new NormalizedError(NETWORK_ERROR, -1, "network: " + e.getMessage(), true);
        return new NormalizedError(UNKNOWN_ERROR, -1, e.getMessage(), true);
    }

    private void throttledLog(String code, String message) {
        long now = System.currentTimeMillis();
        Long last = errorLogThrottle.get(code);
        if (last != null && now - last < ERROR_LOG_WINDOW_MS) return;
        errorLogThrottle.put(code, now);
        if (AUTH_FAILED.equals(code) || EXPIRED.equals(code)) {
            logger.severe(message);
        } else {
            logger.warning(message);
        }
    }

    private static CompletableFuture<AIResponse> done(AIResponse r) {
        return CompletableFuture.completedFuture(r);
    }

    private static AIResponse errResp(String code, String message) {
        return new AIResponse(0.0, code + ": " + message, null, null);
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    private static String maskToken(String token) {
        if (token == null || token.isBlank()) return "***";
        if (token.length() <= 8) return token.substring(0, 4) + "***";
        return token.substring(0, 8) + "***";
    }

    /* Inner helpers */
    private static class NormalizedError {
        final String code; final int httpCode; final String message; final boolean retryable;
        NormalizedError(String code, int httpCode, String message, boolean retryable) {
            this.code = code; this.httpCode = httpCode; this.message = message; this.retryable = retryable;
        }
    }
}
