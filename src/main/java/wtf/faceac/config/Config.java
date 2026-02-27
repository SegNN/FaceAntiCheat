

package wtf.faceac.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public class Config {
    private final boolean debug;
    private final int preHitTicks;
    private final int postHitTicks;
    private final double hitLockThreshold;
    private final int postHitTimeoutTicks;
    private final String outputDirectory;
    private final boolean aiEnabled;
    private final String aiApiKey;
    private final double aiAlertThreshold;
    private final double aiChatAlertThreshold;
    private final boolean aiConsoleAlerts;
    private final double aiBufferFlag;
    private final double aiBufferResetOnFlag;
    private final double aiBufferMultiplier;
    private final double aiBufferDecrease;
    private final int aiSequence;
    private final int aiStep;
    private final double aiPunishmentMinProbability;
    private final Map<Integer, String> punishmentCommands;
    private final boolean animationEnabled;

    private final boolean liteBansEnabled;
    private final String liteBansDbHost;
    private final int liteBansDbPort;
    private final String liteBansDbName;
    private final String liteBansDbUsername;
    private final String liteBansDbPassword;
    private final String liteBansTablePrefix;
    private final int liteBansLookbackDays;
    private final Set<String> liteBansCheatReasons;
    private final boolean autostartEnabled;
    private final String autostartLabel;
    private final String autostartComment;
    private final String detectionServerType;
    private final String detectionEndpoint;
    private final String serverAddress;
    private final int detectionTimeoutMs;
    private final int detectionRetryAttempts;
    private final int detectionRetryBackoffMs;
    private final boolean detectionHealthcheckEnabled;
    private final String detectionHealthcheckPath;
    private final boolean detectionAllowHttp;
    private final int reportStatsIntervalSeconds;
    private final boolean vlDecayEnabled;
    private final int vlDecayIntervalSeconds;
    private final int vlDecayAmount;
    private final boolean worldGuardEnabled;
    private final List<String> worldGuardDisabledRegions;
    private final boolean foliaEnabled;
    private final int foliaThreadPoolSize;
    private final boolean foliaEntitySchedulerEnabled;
    private final boolean foliaRegionSchedulerEnabled;
    private final Map<String, String> modelNames;
    private final Map<String, Boolean> modelOnlyAlert;
    public static final boolean DEFAULT_DEBUG = false;
    public static final String DEFAULT_OUTPUT_DIRECTORY = "plugins/FaceAC/data";
    public static final int PRE_HIT_TICKS = 5;
    public static final int POST_HIT_TICKS = 3;
    public static final double HIT_LOCK_THRESHOLD = 5.0;
    public static final int POST_HIT_TIMEOUT_TICKS = 40;
    public static final boolean DEFAULT_AI_ENABLED = false;
    public static final String DEFAULT_AI_API_KEY = "";
    public static final double DEFAULT_AI_ALERT_THRESHOLD = 0.5;
    public static final double DEFAULT_AI_CHAT_ALERT_THRESHOLD = 0.2;
    public static final boolean DEFAULT_AI_CONSOLE_ALERTS = true;
    public static final double DEFAULT_AI_BUFFER_FLAG = 50.0;
    public static final double DEFAULT_AI_BUFFER_RESET_ON_FLAG = 25.0;
    public static final double DEFAULT_AI_BUFFER_MULTIPLIER = 100.0;
    public static final double DEFAULT_AI_BUFFER_DECREASE = 0.25;
    public static final double DEFAULT_AI_PUNISHMENT_MIN_PROBABILITY = 0.85;
    public static final boolean DEFAULT_ANIMATION_ENABLED = true;
    public static final int DEFAULT_AI_SEQUENCE = 40;
    public static final int DEFAULT_AI_STEP = 10;

    public static final boolean DEFAULT_LITEBANS_ENABLED = false;
    public static final String DEFAULT_LITEBANS_DB_HOST = "localhost";
    public static final int DEFAULT_LITEBANS_DB_PORT = 3306;
    public static final String DEFAULT_LITEBANS_DB_NAME = "litebans";
    public static final String DEFAULT_LITEBANS_DB_USERNAME = "";
    public static final String DEFAULT_LITEBANS_DB_PASSWORD = "";
    public static final String DEFAULT_LITEBANS_TABLE_PREFIX = "litebans_";
    public static final int DEFAULT_LITEBANS_LOOKBACK_DAYS = 7;
    public static final boolean DEFAULT_AUTOSTART_ENABLED = false;
    public static final String DEFAULT_AUTOSTART_LABEL = "UNLABELED";
    public static final String DEFAULT_AUTOSTART_COMMENT = "";
    public static final String DEFAULT_DETECTION_SERVER_TYPE = "face-backend";
    public static final String DEFAULT_SERVER_ADDRESS = "https://svo-host.ru";
    public static final int DEFAULT_DETECTION_TIMEOUT_MS = 30000;
    public static final int DEFAULT_DETECTION_RETRY_ATTEMPTS = 2;
    public static final int DEFAULT_DETECTION_RETRY_BACKOFF_MS = 500;
    public static final boolean DEFAULT_DETECTION_HEALTHCHECK_ENABLED = false;
    public static final String DEFAULT_DETECTION_HEALTHCHECK_PATH = "/health";
    public static final boolean DEFAULT_DETECTION_ALLOW_HTTP = false;
    public static final int DEFAULT_REPORT_STATS_INTERVAL_SECONDS = 30;
    public static final boolean DEFAULT_VL_DECAY_ENABLED = true;
    public static final int DEFAULT_VL_DECAY_INTERVAL_SECONDS = 60;
    public static final int DEFAULT_VL_DECAY_AMOUNT = 1;
    public static final boolean DEFAULT_WORLDGUARD_ENABLED = true;
    public static final List<String> DEFAULT_WORLDGUARD_DISABLED_REGIONS = new ArrayList<>();
    public static final boolean DEFAULT_FOLIA_ENABLED = true;
    public static final int DEFAULT_FOLIA_THREAD_POOL_SIZE = 0;
    public static final boolean DEFAULT_FOLIA_ENTITY_SCHEDULER_ENABLED = true;
    public static final boolean DEFAULT_FOLIA_REGION_SCHEDULER_ENABLED = true;
    private static final AtomicBoolean LEGACY_WARNING_LOGGED = new AtomicBoolean(false);

    public Config() {
        this.debug = DEFAULT_DEBUG;
        this.preHitTicks = PRE_HIT_TICKS;
        this.postHitTicks = POST_HIT_TICKS;
        this.hitLockThreshold = HIT_LOCK_THRESHOLD;
        this.postHitTimeoutTicks = POST_HIT_TIMEOUT_TICKS;
        this.outputDirectory = DEFAULT_OUTPUT_DIRECTORY;
        this.aiEnabled = DEFAULT_AI_ENABLED;
        this.aiApiKey = DEFAULT_AI_API_KEY;
        this.aiAlertThreshold = DEFAULT_AI_ALERT_THRESHOLD;
        this.aiChatAlertThreshold = DEFAULT_AI_CHAT_ALERT_THRESHOLD;
        this.aiConsoleAlerts = DEFAULT_AI_CONSOLE_ALERTS;
        this.aiBufferFlag = DEFAULT_AI_BUFFER_FLAG;
        this.aiBufferResetOnFlag = DEFAULT_AI_BUFFER_RESET_ON_FLAG;
        this.aiBufferMultiplier = DEFAULT_AI_BUFFER_MULTIPLIER;
        this.aiBufferDecrease = DEFAULT_AI_BUFFER_DECREASE;
        this.aiSequence = DEFAULT_AI_SEQUENCE;
        this.aiStep = DEFAULT_AI_STEP;
        this.aiPunishmentMinProbability = DEFAULT_AI_PUNISHMENT_MIN_PROBABILITY;
        this.punishmentCommands = new HashMap<>();
        this.animationEnabled = DEFAULT_ANIMATION_ENABLED;

        this.liteBansEnabled = DEFAULT_LITEBANS_ENABLED;
        this.liteBansDbHost = DEFAULT_LITEBANS_DB_HOST;
        this.liteBansDbPort = DEFAULT_LITEBANS_DB_PORT;
        this.liteBansDbName = DEFAULT_LITEBANS_DB_NAME;
        this.liteBansDbUsername = DEFAULT_LITEBANS_DB_USERNAME;
        this.liteBansDbPassword = DEFAULT_LITEBANS_DB_PASSWORD;
        this.liteBansTablePrefix = DEFAULT_LITEBANS_TABLE_PREFIX;
        this.liteBansLookbackDays = DEFAULT_LITEBANS_LOOKBACK_DAYS;
        this.liteBansCheatReasons = createDefaultCheatReasons();
        this.autostartEnabled = DEFAULT_AUTOSTART_ENABLED;
        this.autostartLabel = DEFAULT_AUTOSTART_LABEL;
        this.autostartComment = DEFAULT_AUTOSTART_COMMENT;
        this.detectionServerType = DEFAULT_DETECTION_SERVER_TYPE;
        this.detectionEndpoint = DEFAULT_SERVER_ADDRESS;
        this.serverAddress = DEFAULT_SERVER_ADDRESS;
        this.detectionTimeoutMs = DEFAULT_DETECTION_TIMEOUT_MS;
        this.detectionRetryAttempts = DEFAULT_DETECTION_RETRY_ATTEMPTS;
        this.detectionRetryBackoffMs = DEFAULT_DETECTION_RETRY_BACKOFF_MS;
        this.detectionHealthcheckEnabled = DEFAULT_DETECTION_HEALTHCHECK_ENABLED;
        this.detectionHealthcheckPath = DEFAULT_DETECTION_HEALTHCHECK_PATH;
        this.detectionAllowHttp = DEFAULT_DETECTION_ALLOW_HTTP;
        this.reportStatsIntervalSeconds = DEFAULT_REPORT_STATS_INTERVAL_SECONDS;
        this.vlDecayEnabled = DEFAULT_VL_DECAY_ENABLED;
        this.vlDecayIntervalSeconds = DEFAULT_VL_DECAY_INTERVAL_SECONDS;
        this.vlDecayAmount = DEFAULT_VL_DECAY_AMOUNT;
        this.worldGuardEnabled = DEFAULT_WORLDGUARD_ENABLED;
        this.worldGuardDisabledRegions = new ArrayList<>(DEFAULT_WORLDGUARD_DISABLED_REGIONS);
        this.foliaEnabled = DEFAULT_FOLIA_ENABLED;
        this.foliaThreadPoolSize = DEFAULT_FOLIA_THREAD_POOL_SIZE;
        this.foliaEntitySchedulerEnabled = DEFAULT_FOLIA_ENTITY_SCHEDULER_ENABLED;
        this.foliaRegionSchedulerEnabled = DEFAULT_FOLIA_REGION_SCHEDULER_ENABLED;
        this.modelNames = new HashMap<>();
        this.modelOnlyAlert = new HashMap<>();
    }

    private static Set<String> createDefaultCheatReasons() {
        Set<String> reasons = new HashSet<>();
        reasons.add("killaura");
        reasons.add("cheat");
        reasons.add("hack");
        return reasons;
    }

    public Config(JavaPlugin plugin) {
        this(plugin, null);
    }

    public Config(JavaPlugin plugin, Logger logger) {
        plugin.saveDefaultConfig();
        FileConfiguration config = plugin.getConfig();
        this.debug = config.getBoolean("debug", DEFAULT_DEBUG);
        this.preHitTicks = PRE_HIT_TICKS;
        this.postHitTicks = POST_HIT_TICKS;
        this.hitLockThreshold = HIT_LOCK_THRESHOLD;
        this.postHitTimeoutTicks = POST_HIT_TIMEOUT_TICKS;
        this.outputDirectory = config.getString("outputDirectory", DEFAULT_OUTPUT_DIRECTORY);
        this.aiEnabled = config.getBoolean("detection.enabled",
                config.getBoolean("ai.enabled", DEFAULT_AI_ENABLED));
        this.aiApiKey = config.getString("detection.api-key",
                config.getString("ai.api-key", DEFAULT_AI_API_KEY));
        double alertThreshold = config.getDouble("alerts.threshold",
                config.getDouble("ai.alert.threshold", DEFAULT_AI_ALERT_THRESHOLD));
        this.aiAlertThreshold = clampThreshold(alertThreshold, "alerts.threshold", logger);
        double chatAlertThreshold = config.getDouble("alerts.chat-threshold", DEFAULT_AI_CHAT_ALERT_THRESHOLD);
        this.aiChatAlertThreshold = clampThreshold(chatAlertThreshold, "alerts.chat-threshold", logger);
        this.aiConsoleAlerts = config.getBoolean("alerts.console",
                config.getBoolean("ai.alert.console", DEFAULT_AI_CONSOLE_ALERTS));
        this.aiBufferFlag = config.getDouble("violation.threshold",
                config.getDouble("ai.buffer.flag", DEFAULT_AI_BUFFER_FLAG));
        this.aiBufferResetOnFlag = config.getDouble("violation.reset-value",
                config.getDouble("ai.buffer.reset-on-flag", DEFAULT_AI_BUFFER_RESET_ON_FLAG));
        this.aiBufferMultiplier = config.getDouble("violation.multiplier",
                config.getDouble("ai.buffer.multiplier", DEFAULT_AI_BUFFER_MULTIPLIER));
        this.aiBufferDecrease = config.getDouble("violation.decay",
                config.getDouble("ai.buffer.decrease", DEFAULT_AI_BUFFER_DECREASE));
        this.aiSequence = config.getInt("detection.sample-size",
                config.getInt("ai.sequence", DEFAULT_AI_SEQUENCE));
        this.aiStep = config.getInt("detection.sample-interval",
                config.getInt("ai.step", DEFAULT_AI_STEP));
        double punishmentMinProb = config.getDouble("penalties.min-probability",
                config.getDouble("ai.punishment.min-probability", DEFAULT_AI_PUNISHMENT_MIN_PROBABILITY));
        this.aiPunishmentMinProbability = clampThreshold(punishmentMinProb, "penalties.min-probability", logger);
        this.animationEnabled = config.getBoolean("penalties.animation.enabled", DEFAULT_ANIMATION_ENABLED);
        this.punishmentCommands = new HashMap<>();
        ConfigurationSection cmdSection = config.getConfigurationSection("penalties.actions");
        if (cmdSection == null) {
            cmdSection = config.getConfigurationSection("ai.punishment.commands");
        }
        if (cmdSection != null) {
            for (String key : cmdSection.getKeys(false)) {
                try {
                    int vl = Integer.parseInt(key);
                    String cmd = cmdSection.getString(key);
                    if (cmd != null && !cmd.isEmpty()) {
                        punishmentCommands.put(vl, cmd);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        this.liteBansEnabled = config.getBoolean("litebans.enabled", DEFAULT_LITEBANS_ENABLED);
        this.liteBansDbHost = config.getString("litebans.database.host", DEFAULT_LITEBANS_DB_HOST);
        this.liteBansDbPort = config.getInt("litebans.database.port", DEFAULT_LITEBANS_DB_PORT);
        this.liteBansDbName = config.getString("litebans.database.name", DEFAULT_LITEBANS_DB_NAME);
        this.liteBansDbUsername = config.getString("litebans.database.username", DEFAULT_LITEBANS_DB_USERNAME);
        this.liteBansDbPassword = config.getString("litebans.database.password", DEFAULT_LITEBANS_DB_PASSWORD);
        this.liteBansTablePrefix = config.getString("litebans.table-prefix", DEFAULT_LITEBANS_TABLE_PREFIX);
        this.liteBansLookbackDays = config.getInt("litebans.lookback-days", DEFAULT_LITEBANS_LOOKBACK_DAYS);
        this.liteBansCheatReasons = new HashSet<>();

        List<String> reasonsList = config.getStringList("litebans.cheat-reasons");
        if (reasonsList.isEmpty()) {
            this.liteBansCheatReasons.addAll(createDefaultCheatReasons());
        } else {
            this.liteBansCheatReasons.addAll(reasonsList);
        }

        this.autostartEnabled = config.getBoolean("autostart.enabled", DEFAULT_AUTOSTART_ENABLED);
        this.autostartLabel = config.getString("autostart.label", DEFAULT_AUTOSTART_LABEL);
        this.autostartComment = config.getString("autostart.comment", DEFAULT_AUTOSTART_COMMENT);

        boolean usesLegacyEndpoint = !config.contains("detection.endpoint") && config.contains("ai.server");
        boolean usesLegacyApiKey = !config.contains("detection.api-key") && config.contains("ai.api-key");
        boolean usesLegacyServerType = !config.contains("detection.server-type") && config.contains("ai.server-type");
        if ((usesLegacyEndpoint || usesLegacyApiKey || usesLegacyServerType) && logger != null
                && LEGACY_WARNING_LOGGED.compareAndSet(false, true)) {
            logger.warning("[Config] Legacy AI keys (ai.server/ai.api-key/ai.server-type) are deprecated. "
                    + "Please migrate to detection.* keys.");
        }

        this.detectionServerType = normalizeServerType(
                config.getString("detection.server-type",
                        config.getString("ai.server-type", DEFAULT_DETECTION_SERVER_TYPE)),
                logger);
        this.detectionAllowHttp = config.getBoolean("detection.allow-http", DEFAULT_DETECTION_ALLOW_HTTP);
        this.detectionEndpoint = normalizeEndpointInput(
                config.getString("detection.endpoint",
                        config.getString("ai.server", DEFAULT_SERVER_ADDRESS)),
                logger);
        this.serverAddress = resolveEndpoint(this.detectionEndpoint, detectionAllowHttp, logger);
        this.detectionTimeoutMs = clampInt(
                config.getInt("detection.timeout-ms", DEFAULT_DETECTION_TIMEOUT_MS),
                1000,
                120000,
                "detection.timeout-ms",
                logger);
        this.detectionRetryAttempts = clampInt(
                config.getInt("detection.retry.attempts", DEFAULT_DETECTION_RETRY_ATTEMPTS),
                0,
                5,
                "detection.retry.attempts",
                logger);
        this.detectionRetryBackoffMs = clampInt(
                config.getInt("detection.retry.backoff-ms", DEFAULT_DETECTION_RETRY_BACKOFF_MS),
                0,
                10000,
                "detection.retry.backoff-ms",
                logger);
        this.detectionHealthcheckEnabled = config.getBoolean(
                "detection.healthcheck.enabled",
                DEFAULT_DETECTION_HEALTHCHECK_ENABLED);
        this.detectionHealthcheckPath = normalizeHealthcheckPath(
                config.getString("detection.healthcheck.path", DEFAULT_DETECTION_HEALTHCHECK_PATH));
        this.reportStatsIntervalSeconds = DEFAULT_REPORT_STATS_INTERVAL_SECONDS;
        this.vlDecayEnabled = config.getBoolean("violation.vl-decay.enabled", DEFAULT_VL_DECAY_ENABLED);
        this.vlDecayIntervalSeconds = config.getInt("violation.vl-decay.interval", DEFAULT_VL_DECAY_INTERVAL_SECONDS);
        this.vlDecayAmount = config.getInt("violation.vl-decay.amount", DEFAULT_VL_DECAY_AMOUNT);
        this.worldGuardEnabled = config.getBoolean("detection.worldguard.enabled", DEFAULT_WORLDGUARD_ENABLED);
        this.worldGuardDisabledRegions = config.getStringList("detection.worldguard.disabled-regions");
        this.foliaEnabled = config.getBoolean("folia.enabled", DEFAULT_FOLIA_ENABLED);
        this.foliaThreadPoolSize = config.getInt("folia.thread-pool-size", DEFAULT_FOLIA_THREAD_POOL_SIZE);
        this.foliaEntitySchedulerEnabled = config.getBoolean("folia.entity-scheduler.enabled",
                DEFAULT_FOLIA_ENTITY_SCHEDULER_ENABLED);
        this.foliaRegionSchedulerEnabled = config.getBoolean("folia.region-scheduler.enabled",
                DEFAULT_FOLIA_REGION_SCHEDULER_ENABLED);

        this.modelNames = new HashMap<>();
        this.modelOnlyAlert = new HashMap<>();
        ConfigurationSection modelsSection = config.getConfigurationSection("detection.models");
        if (modelsSection != null) {
            for (String modelKey : modelsSection.getKeys(false)) {
                ConfigurationSection modelSection = modelsSection.getConfigurationSection(modelKey);
                if (modelSection != null) {
                    String displayName = modelSection.getString("name", modelKey);
                    boolean onlyAlertForModel = modelSection.getBoolean("only-alert", false);
                    modelNames.put(modelKey, displayName);
                    modelOnlyAlert.put(modelKey, onlyAlertForModel);
                } else {
                    String displayName = modelsSection.getString(modelKey);
                    if (displayName != null && !displayName.isEmpty()) {
                        modelNames.put(modelKey, displayName);
                        modelOnlyAlert.put(modelKey, false);
                    }
                }
            }
        }
    }

    private double clampThreshold(double value, String configPath, Logger logger) {
        if (value < 0.0 || value > 1.0) {
            double clamped = Math.max(0.0, Math.min(1.0, value));
            if (logger != null) {
                logger.warning("[Config] " + configPath + " value " + value +
                        " is outside valid range [0.0, 1.0], clamped to " + clamped);
            }
            return clamped;
        }
        return value;
    }

    private String normalizeServerType(String rawValue, Logger logger) {
        String value = rawValue == null ? "" : rawValue.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) {
            return DEFAULT_DETECTION_SERVER_TYPE;
        }
        if ("face-backend".equals(value) || "huggingface".equals(value)) {
            return value;
        }
        if (logger != null) {
            logger.warning("[Config] detection.server-type '" + rawValue + "' is invalid, using "
                    + DEFAULT_DETECTION_SERVER_TYPE);
        }
        return DEFAULT_DETECTION_SERVER_TYPE;
    }

    private String normalizeEndpointInput(String endpoint, Logger logger) {
        String value = endpoint == null ? "" : endpoint.trim();
        if (!value.isEmpty()) {
            return value;
        }
        if (logger != null) {
            logger.warning("[Config] detection.endpoint is empty, using default: "
                    + DEFAULT_SERVER_ADDRESS);
        }
        return DEFAULT_SERVER_ADDRESS;
    }

    private String resolveEndpoint(String endpoint, boolean allowHttp, Logger logger) {
        String value = endpoint.trim();
        if (value.startsWith("http://") || value.startsWith("https://")) {
            if (isValidHttpEndpoint(value, allowHttp, logger)) {
                return value;
            }
            return DEFAULT_SERVER_ADDRESS;
        }
        if (logger != null) {
            logger.warning("[Config] detection.endpoint '" + value + "' is not a valid URL, using default: "
                    + DEFAULT_SERVER_ADDRESS);
        }
        return DEFAULT_SERVER_ADDRESS;
    }

    private boolean isValidHttpEndpoint(String endpoint, boolean allowHttp, Logger logger) {
        try {
            URI uri = URI.create(endpoint);
            String scheme = uri.getScheme();
            if (uri.getHost() == null) {
                if (logger != null) {
                    logger.warning("[Config] detection.endpoint has no host: " + endpoint);
                }
                return false;
            }
            if ("https".equalsIgnoreCase(scheme)) {
                return true;
            }
            if ("http".equalsIgnoreCase(scheme)) {
                if (allowHttp) {
                    return true;
                }
                if (logger != null) {
                    logger.warning("[Config] HTTP endpoint is disabled. Set detection.allow-http=true if needed.");
                }
                return false;
            }
            if (logger != null) {
                logger.warning("[Config] Unsupported endpoint scheme: " + scheme);
            }
            return false;
        } catch (IllegalArgumentException ex) {
            if (logger != null) {
                logger.warning("[Config] Invalid endpoint URL: " + endpoint);
            }
            return false;
        }
    }

    private int clampInt(int value, int minimum, int maximum, String configPath, Logger logger) {
        if (value < minimum || value > maximum) {
            int clamped = Math.max(minimum, Math.min(maximum, value));
            if (logger != null) {
                logger.warning("[Config] " + configPath + " value " + value +
                        " is outside range [" + minimum + ", " + maximum + "], clamped to " + clamped);
            }
            return clamped;
        }
        return value;
    }

    private String normalizeHealthcheckPath(String path) {
        String value = path == null ? "" : path.trim();
        if (value.isEmpty()) {
            return DEFAULT_DETECTION_HEALTHCHECK_PATH;
        }
        return value.startsWith("/") ? value : "/" + value;
    }

    public boolean isDebug() {
        return debug;
    }

    public int getPreHitTicks() {
        return preHitTicks;
    }

    public int getPostHitTicks() {
        return postHitTicks;
    }

    public double getHitLockThreshold() {
        return hitLockThreshold;
    }

    public int getPostHitTimeoutTicks() {
        return postHitTimeoutTicks;
    }

    public String getOutputDirectory() {
        return outputDirectory;
    }

    public boolean isAiEnabled() {
        return aiEnabled;
    }

    public String getAiApiKey() {
        return aiApiKey;
    }

    public double getAiAlertThreshold() {
        return aiAlertThreshold;
    }

    public double getAiChatAlertThreshold() {
        return aiChatAlertThreshold;
    }

    public boolean isAiConsoleAlerts() {
        return aiConsoleAlerts;
    }

    public double getAiBufferFlag() {
        return aiBufferFlag;
    }

    public double getAiBufferResetOnFlag() {
        return aiBufferResetOnFlag;
    }

    public double getAiBufferMultiplier() {
        return aiBufferMultiplier;
    }

    public double getAiBufferDecrease() {
        return aiBufferDecrease;
    }

    public int getAiSequence() {
        return aiSequence;
    }

    public int getAiStep() {
        return aiStep;
    }

    public double getAiPunishmentMinProbability() {
        return aiPunishmentMinProbability;
    }

    public boolean isAnimationEnabled() {
        return animationEnabled;
    }

    public String getPunishmentCommand(int vl) {
        return punishmentCommands.get(vl);
    }

    public Map<Integer, String> getPunishmentCommands() {
        return punishmentCommands;
    }

    public boolean isLiteBansEnabled() {
        return liteBansEnabled;
    }

    public String getLiteBansDbHost() {
        return liteBansDbHost;
    }

    public int getLiteBansDbPort() {
        return liteBansDbPort;
    }

    public String getLiteBansDbName() {
        return liteBansDbName;
    }

    public String getLiteBansDbUsername() {
        return liteBansDbUsername;
    }

    public String getLiteBansDbPassword() {
        return liteBansDbPassword;
    }

    public String getLiteBansTablePrefix() {
        return liteBansTablePrefix;
    }

    public int getLiteBansLookbackDays() {
        return liteBansLookbackDays;
    }

    public Set<String> getLiteBansCheatReasons() {
        return liteBansCheatReasons;
    }

    public boolean isAutostartEnabled() {
        return autostartEnabled;
    }

    public String getAutostartLabel() {
        return autostartLabel;
    }

    public String getAutostartComment() {
        return autostartComment;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public String getDetectionEndpoint() {
        return detectionEndpoint;
    }

    public String getDetectionServerType() {
        return detectionServerType;
    }

    public int getDetectionTimeoutMs() {
        return detectionTimeoutMs;
    }

    public int getDetectionRetryAttempts() {
        return detectionRetryAttempts;
    }

    public int getDetectionRetryBackoffMs() {
        return detectionRetryBackoffMs;
    }

    public boolean isDetectionHealthcheckEnabled() {
        return detectionHealthcheckEnabled;
    }

    public String getDetectionHealthcheckPath() {
        return detectionHealthcheckPath;
    }

    public boolean isDetectionAllowHttp() {
        return detectionAllowHttp;
    }

    public int getReportStatsIntervalSeconds() {
        return reportStatsIntervalSeconds;
    }

    public String getServerHost() {
        try {
            URI uri = URI.create(serverAddress);
            if (uri.getHost() != null) {
                return uri.getHost();
            }
        } catch (IllegalArgumentException ignored) {
        }
        return serverAddress;
    }

    public int getServerPort() {
        try {
            URI uri = URI.create(serverAddress);
            if (uri.getPort() > 0) {
                return uri.getPort();
            }
            if ("https".equalsIgnoreCase(uri.getScheme())) {
                return 443;
            }
            if ("http".equalsIgnoreCase(uri.getScheme())) {
                return 80;
            }
        } catch (IllegalArgumentException ignored) {
        }
        return 5000;
    }

    public boolean isVlDecayEnabled() {
        return vlDecayEnabled;
    }

    public int getVlDecayIntervalSeconds() {
        return vlDecayIntervalSeconds;
    }

    public int getVlDecayAmount() {
        return vlDecayAmount;
    }

    public boolean isWorldGuardEnabled() {
        return worldGuardEnabled;
    }

    public List<String> getWorldGuardDisabledRegions() {
        return worldGuardDisabledRegions;
    }

    public boolean isFoliaEnabled() {
        return foliaEnabled;
    }

    public int getFoliaThreadPoolSize() {
        return foliaThreadPoolSize;
    }

    public boolean isFoliaEntitySchedulerEnabled() {
        return foliaEntitySchedulerEnabled;
    }

    public boolean isFoliaRegionSchedulerEnabled() {
        return foliaRegionSchedulerEnabled;
    }

    public boolean isOnlyAlertForModel(String modelKey) {
        if (modelKey == null) {
            return false;
        }
        return modelOnlyAlert.getOrDefault(modelKey, false);
    }

    public String getModelDisplayName(String modelKey) {
        if (modelKey == null) {
            return "Unknown";
        }
        return modelNames.getOrDefault(modelKey, modelKey);
    }

    public Map<String, String> getModelNames() {
        return modelNames;
    }

    public Map<String, Boolean> getModelOnlyAlert() {
        return modelOnlyAlert;
    }
}
