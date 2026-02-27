

package wtf.faceac.server;

import wtf.faceac.Main;
import wtf.faceac.config.Config;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class AIClientProvider {
    private final Main plugin;
    private final Logger logger;
    private IAIClient currentClient;
    private Config config;
    private volatile boolean connecting = false;
    private volatile String clientType = "none";

    public AIClientProvider(Main plugin, Config config) {
        this.plugin = plugin;
        this.config = config;
        this.logger = plugin.getLogger();
    }

    public CompletableFuture<Boolean> initialize() {
        if (!config.isAiEnabled()) {
            plugin.debug("[AI] AI is disabled, skipping client initialization");
            return CompletableFuture.completedFuture(false);
        }

        return shutdown().thenCompose(v -> {
            String serverAddress = config.getServerAddress();
            String apiKey = config.getAiApiKey();

            if (serverAddress == null || serverAddress.isEmpty()) {
                logger.warning("[AI] Server address is not configured!");
                return CompletableFuture.completedFuture(false);
            }
            connecting = true;

            String serverType = config.getDetectionServerType();
            if ("face-backend".equalsIgnoreCase(serverType)) {
                return initializeFaceBackend(serverAddress, apiKey);
            }
            logger.warning("[AI] Unsupported detection.server-type '" + serverType +
                    "'. Use 'face-backend'. Falling back to face-backend.");
            return initializeFaceBackend(serverAddress, apiKey);
        });
    }

    private CompletableFuture<Boolean> initializeFaceBackend(String serverAddress, String apiKey) {
        FaceBackendClient faceClient = new FaceBackendClient(
                plugin,
                serverAddress,
                apiKey,
                config.getDetectionTimeoutMs(),
                config.getDetectionRetryAttempts(),
                config.getDetectionRetryBackoffMs(),
                config.isDetectionHealthcheckEnabled(),
                config.isDebug());
        this.currentClient = faceClient;
        this.clientType = "FaceBackend";
        logger.info("[FACE] Initializing FACE Dashboard client for " + serverAddress);
        return faceClient.connectWithRetry()
                .thenApply(success -> {
                    connecting = false;
                    if (success) {
                        logger.info("[FACE] Client initialized successfully");
                    } else {
                        logger.warning("[FACE] Client initialization failed");
                    }
                    return success;
                })
                .exceptionally(e -> {
                    connecting = false;
                    logger.severe("[FACE] Initialization error: " + e.getMessage());
                    return false;
                });
    }

    public CompletableFuture<Void> shutdown() {
        if (currentClient != null) {
            logger.info("[AI] Shutting down " + clientType + " client...");
            return currentClient.disconnect()
                    .thenRun(() -> {
                        currentClient = null;
                        clientType = "none";
                        logger.info("[AI] Client shutdown complete");
                    });
        }
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<Boolean> reload() {
        return shutdown().thenCompose(v -> initialize());
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public IAIClient get() {
        return currentClient;
    }

    public boolean isAvailable() {
        return currentClient != null && currentClient.isConnected();
    }

    public boolean isEnabled() {
        return config.isAiEnabled();
    }

    public boolean isConnecting() {
        return connecting;
    }

    public boolean isLimitExceeded() {
        return currentClient != null && currentClient.isLimitExceeded();
    }

    public String getClientType() {
        return clientType;
    }
}
