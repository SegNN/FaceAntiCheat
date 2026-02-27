

package wtf.faceac.scheduler;

import org.bukkit.plugin.Plugin;

public class SchedulerManager {
    private static SchedulerAdapter adapter;
    private static ServerType serverType;
    private static boolean initialized = false;

    public static void initialize(Plugin plugin) {
        if (initialized) {
            throw new IllegalStateException("SchedulerManager is already initialized");
        }
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }
        try {
            serverType = detectServerType();
            if (serverType == ServerType.FOLIA) {
                // Use reflection to avoid NoClassDefFoundError on non-Folia servers when
                // verifying the class
                adapter = (SchedulerAdapter) Class.forName("wtf.faceac.scheduler.FoliaSchedulerAdapter")
                        .getConstructor(Plugin.class)
                        .newInstance(plugin);
            } else {
                adapter = new BukkitSchedulerAdapter(plugin);
            }
            initialized = true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize SchedulerManager", e);
        }
    }

    public static SchedulerAdapter getAdapter() {
        if (!initialized || adapter == null) {
            throw new IllegalStateException(
                    "SchedulerManager has not been initialized. Call initialize(plugin) first.");
        }
        return adapter;
    }

    public static ServerType getServerType() {
        if (!initialized) {
            throw new IllegalStateException(
                    "SchedulerManager has not been initialized. Call initialize(plugin) first.");
        }
        return serverType;
    }

    public static boolean isInitialized() {
        return initialized;
    }

    private static ServerType detectServerType() {
        try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.RegionScheduler");
            Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler");
            Class.forName("io.papermc.paper.threadedregions.scheduler.AsyncScheduler");
            return ServerType.FOLIA;
        } catch (Throwable e) {
            return ServerType.BUKKIT;
        }
    }

    public static void reset() {
        adapter = null;
        serverType = null;
        initialized = false;
    }
}