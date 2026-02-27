

package wtf.faceac;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import io.github.retrooper.packetevents.util.SpigotReflectionUtil;
import wtf.faceac.alert.AlertManager;
import wtf.faceac.checks.AICheck;
import wtf.faceac.commands.CommandHandler;
import wtf.faceac.compat.VersionAdapter;
import wtf.faceac.config.Config;
import wtf.faceac.config.HologramConfig;
import wtf.faceac.config.MenuConfig;
import wtf.faceac.config.MessagesConfig;
import wtf.faceac.datacollector.DataCollectorFactory;
import wtf.faceac.hologram.NametagManager;
import wtf.faceac.listeners.HitListener;
import wtf.faceac.listeners.PlayerListener;
import wtf.faceac.listeners.RotationListener;
import wtf.faceac.listeners.TeleportListener;
import wtf.faceac.listeners.TickListener;
import wtf.faceac.scheduler.SchedulerManager;
import wtf.faceac.server.AIClientProvider;
import wtf.faceac.session.ISessionManager;
import wtf.faceac.session.SessionManager;
import wtf.faceac.violation.ViolationManager;
import wtf.faceac.util.FeatureCalculator;
import wtf.faceac.util.UpdateChecker;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

public final class Main extends JavaPlugin {
    private Config config;
    private MenuConfig menuConfig;
    private MessagesConfig messagesConfig;
    private HologramConfig hologramConfig;
    private ISessionManager sessionManager;
    private FeatureCalculator featureCalculator;
    private TickListener tickListener;
    private HitListener hitListener;
    private RotationListener rotationListener;
    private PlayerListener playerListener;
    private TeleportListener teleportListener;
    private CommandHandler commandHandler;
    private AIClientProvider aiClientProvider;
    private AlertManager alertManager;
    private ViolationManager violationManager;
    private NametagManager nametagManager;
    private AICheck aiCheck;
    private UpdateChecker updateChecker;

    @Override
    public void onLoad() {
        VersionAdapter.init(getLogger());
        // PacketEvents loading moved to onEnable to avoid ClassLoader issues with
        // PlugMan
    }

    @Override
    public void onEnable() {
        try {
            SchedulerManager.reset();
            SchedulerManager.initialize(this);
            getLogger().info("SchedulerManager initialized for " + SchedulerManager.getServerType());
        } catch (Throwable e) {
            getLogger().severe("Failed to initialize SchedulerManager: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (!initializePacketEvents()) {
            getLogger().severe(
                    "PacketEvents initialization failed. Load this plugin only on full server startup (not via PlugMan reload/load).");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        migrateLegacyDataFolderIfNeeded();

        VersionAdapter.get().logCompatibilityInfo();
        saveDefaultConfig();
        this.config = new Config(this, getLogger());
        this.menuConfig = new MenuConfig(this);
        this.menuConfig.load();
        this.messagesConfig = new MessagesConfig(this);
        this.messagesConfig.load();
        this.hologramConfig = new HologramConfig(this);
        this.hologramConfig.load();

        File outputDir = new File(config.getOutputDirectory());
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        this.featureCalculator = new FeatureCalculator();
        this.sessionManager = DataCollectorFactory.createSessionManager(this);
        this.aiClientProvider = new AIClientProvider(this, config);
        this.alertManager = new AlertManager(this, config);
        this.violationManager = new ViolationManager(this, config, alertManager);
        this.aiCheck = new AICheck(this, config, aiClientProvider, alertManager, violationManager);
        this.violationManager.setAICheck(aiCheck);

        this.nametagManager = new NametagManager(this, aiCheck);
        this.nametagManager.start();

        if (config.isAiEnabled()) {
            aiClientProvider.initialize().thenAccept(success -> {
                if (success) {
                    getLogger().info("[AI] Connected via " + aiClientProvider.getClientType() + " to "
                            + config.getServerAddress());
                } else {
                    getLogger().warning("[AI] Failed to initialize inference backend");
                }
            });
        }
        this.tickListener = new TickListener(this, sessionManager, aiCheck, nametagManager);
        this.hitListener = new HitListener(sessionManager, aiCheck);
        this.rotationListener = new RotationListener(sessionManager, aiCheck);
        this.playerListener = new PlayerListener(this, aiCheck, alertManager, violationManager,
                sessionManager instanceof SessionManager ? (SessionManager) sessionManager : null, tickListener,
                nametagManager);
        this.teleportListener = new TeleportListener(aiCheck);
        this.tickListener.setHitListener(hitListener);
        this.playerListener.setHitListener(hitListener);
        this.hitListener.cacheOnlinePlayers();
        this.tickListener.start();
        for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
            this.tickListener.startPlayerTask(p);
        }
        getServer().getPluginManager().registerEvents(playerListener, this);
        getServer().getPluginManager().registerEvents(teleportListener, this);
        PacketEvents.getAPI().getEventManager().registerListener(hitListener);
        PacketEvents.getAPI().getEventManager().registerListener(rotationListener);
        this.commandHandler = new CommandHandler(sessionManager, alertManager, aiCheck, this);
        PluginCommand command = getCommand("faceac");
        if (command != null) {
            command.setExecutor(commandHandler);
            command.setTabCompleter(commandHandler);
        }
        getLogger().info("FaceAC enabled successfully!");
        getLogger().info("Data collector: ENABLED (output: " + config.getOutputDirectory() + ")");
        if (config.isAiEnabled()) {
            getLogger().info("AI detection: ENABLED (buffer-threshold: " + config.getAiAlertThreshold()
                    + ", chat-alert-threshold: " + config.getAiChatAlertThreshold() + ")");
        } else {
            getLogger().info("AI detection: DISABLED");
        }

        this.updateChecker = new UpdateChecker(this);
        updateChecker.checkForUpdates().thenAccept(available -> {
            if (available) {
                getLogger().warning("=================================================");
                getLogger().warning("A NEW UPDATE IS AVAILABLE: " + updateChecker.getLatestVersion());
                getLogger().warning("Get it from GitHub: none");
                getLogger().warning("=================================================");
            }
        });
    }

    @Override
    public void onDisable() {
        if (tickListener != null) {
            tickListener.stop();
        }
        if (nametagManager != null) {
            nametagManager.stop();
        }
        if (sessionManager != null) {
            getLogger().info("Stopping all active sessions...");
            sessionManager.stopAllSessions();
        }
        if (aiCheck != null) {
            aiCheck.clearAll();
        }
        if (commandHandler != null) {
            commandHandler.cleanup();
        }
        if (aiClientProvider != null) {
            getLogger().info("Shutting down AI client...");
            try {
                aiClientProvider.shutdown().get(5, java.util.concurrent.TimeUnit.SECONDS);
            } catch (Exception e) {
                if (e.getMessage() != null) {
                    getLogger().warning("Error shutting down AI client: " + e.getMessage());
                } else {
                    getLogger().warning("Error shutting down AI client during disable:");
                    e.printStackTrace();
                }
            }
        }
        if (PacketEvents.getAPI() != null) {
            try {
                PacketEvents.getAPI().terminate();
            } catch (Throwable ignored) {
            }
        }
        cleanupStalePacketEventsHandlers();
        SpigotPacketEventsBuilder.clearBuildCache();
        SchedulerManager.reset();

        getLogger().info("FaceAC disabled successfully!");
    }

    public void reloadPluginConfig() {
        SchedulerManager.getAdapter().runSync(() -> {
            try {
                reloadConfig();
                this.config = new Config(this, getLogger());
                if (menuConfig != null)
                    menuConfig.reload();
                if (messagesConfig != null)
                    messagesConfig.reload();
                if (hologramConfig != null)
                    hologramConfig.reload();

                if (nametagManager != null) {
                    nametagManager.stop();
                    nametagManager.start();
                }

                alertManager.setConfig(config);
                violationManager.setConfig(config);
                aiCheck.setConfig(config);
                if (aiClientProvider != null) {
                    aiClientProvider.setConfig(config);
                    if (config.isAiEnabled()) {
                        aiClientProvider.reload().thenAccept(success -> {
                            if (success) {
                                getLogger().info("[AI] Reconnected via " + aiClientProvider.getClientType() + " to "
                                        + config.getServerAddress());
                            }
                        });
                    } else {
                        aiClientProvider.shutdown();
                    }
                }
                getLogger().info("Configuration reloaded!");
            } catch (Exception e) {
                getLogger().severe("Failed to reload configuration: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public MenuConfig getMenuConfig() {
        return menuConfig;
    }

    public MessagesConfig getMessagesConfig() {
        return messagesConfig;
    }

    public HologramConfig getHologramConfig() {
        return hologramConfig;
    }

    public NametagManager getNametagManager() {
        return nametagManager;
    }

    public Config getPluginConfig() {
        return config;
    }

    public ISessionManager getSessionManager() {
        return sessionManager;
    }

    public FeatureCalculator getFeatureCalculator() {
        return featureCalculator;
    }

    public AICheck getAiCheck() {
        return aiCheck;
    }

    public AlertManager getAlertManager() {
        return alertManager;
    }

    public ViolationManager getViolationManager() {
        return violationManager;
    }

    public AIClientProvider getAiClientProvider() {
        return aiClientProvider;
    }

    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }

    public void debug(String message) {
        if (config != null && config.isDebug()) {
            getLogger().info("[Debug] " + message);
        }
    }

    private boolean initializePacketEvents() {
        Throwable lastFailure = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                cleanupStalePacketEventsHandlers();
                if (PacketEvents.getAPI() != null) {
                    try {
                        PacketEvents.getAPI().terminate();
                    } catch (Throwable ignored) {
                    }
                }

                SpigotPacketEventsBuilder.clearBuildCache();
                PacketEvents.setAPI(SpigotPacketEventsBuilder.buildNoCache(this));
                PacketEvents.getAPI().getSettings()
                        .reEncodeByDefault(false)
                        .checkForUpdates(false)
                        .bStats(false)
                        .debug(false);
                PacketEvents.getAPI().load();
                PacketEvents.getAPI().init();
                if (PacketEvents.getAPI().isInitialized()) {
                    return true;
                }
                throw new IllegalStateException("PacketEvents API is not initialized after init()");
            } catch (Throwable t) {
                lastFailure = t;
                getLogger().warning("[PacketEvents] Init attempt " + attempt + " failed: " + t.getMessage());
            }
        }
        if (lastFailure != null) {
            lastFailure.printStackTrace();
        }
        return false;
    }

    private void cleanupStalePacketEventsHandlers() {
        try {
            Set<String> ids = new LinkedHashSet<>();
            ids.add(getName().toLowerCase(Locale.ROOT));
            ids.add("faceac");
            ids.add("mlsac");

            List<String> handlerNames = new ArrayList<>();
            for (String id : ids) {
                handlerNames.add("pe-decoder-" + id);
                handlerNames.add("pe-encoder-" + id);
                handlerNames.add("pe-connection-handler-" + id);
                handlerNames.add("pe-connection-initializer-" + id);
                handlerNames.add("pe-timeout-handler-" + id);
            }

            int removed = 0;
            for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
                Object channelObj = SpigotReflectionUtil.getChannel(player);
                if (channelObj == null) {
                    continue;
                }
                Object pipeline = invokeMethod(channelObj, "pipeline");
                if (pipeline == null) {
                    continue;
                }
                synchronized (channelObj) {
                    for (String handlerName : handlerNames) {
                        Object existing = invokeMethod(pipeline, "get", String.class, handlerName);
                        if (existing != null) {
                            invokeMethod(pipeline, "remove", String.class, handlerName);
                            removed++;
                        }
                    }
                }
            }

            if (removed > 0) {
                getLogger().warning("[PacketEvents] Removed " + removed + " stale Netty handlers before init");
            }
        } catch (Throwable t) {
            getLogger().warning("[PacketEvents] Failed to cleanup stale handlers: " + t.getMessage());
        }
    }

    private Object invokeMethod(Object target, String methodName) {
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private Object invokeMethod(Object target, String methodName, Class<?> argType, Object argValue) {
        try {
            return target.getClass().getMethod(methodName, argType).invoke(target, argValue);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private void migrateLegacyDataFolderIfNeeded() {
        File currentDataFolder = getDataFolder();
        if (currentDataFolder.exists()) {
            return;
        }

        File pluginsDir = currentDataFolder.getParentFile();
        if (pluginsDir == null) {
            return;
        }

        File legacyFolder = new File(pluginsDir, "MLSAC");
        if (!legacyFolder.exists() || !legacyFolder.isDirectory()) {
            return;
        }

        try {
            copyDirectory(legacyFolder.toPath(), currentDataFolder.toPath());
            getLogger().info("[Migration] Imported configs from legacy folder: " + legacyFolder.getPath());
        } catch (IOException e) {
            getLogger().warning("[Migration] Failed to import legacy configs: " + e.getMessage());
        }
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        try (Stream<Path> stream = Files.walk(source)) {
            stream.forEach(path -> {
                try {
                    Path relativePath = source.relativize(path);
                    Path destination = target.resolve(relativePath);
                    if (Files.isDirectory(path)) {
                        Files.createDirectories(destination);
                    } else {
                        Path parent = destination.getParent();
                        if (parent != null) {
                            Files.createDirectories(parent);
                        }
                        Files.copy(path, destination);
                    }
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            });
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw e;
        }
    }
}
