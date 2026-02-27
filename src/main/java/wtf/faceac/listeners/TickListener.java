package wtf.faceac.listeners;

import wtf.faceac.checks.AICheck;
import wtf.faceac.compat.EventCompat;
import wtf.faceac.scheduler.ScheduledTask;
import wtf.faceac.scheduler.SchedulerManager;
import wtf.faceac.session.ISessionManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TickListener {
    private final ISessionManager sessionManager;
    private final AICheck aiCheck;
    private final wtf.faceac.hologram.NametagManager nametagManager;
    private final EventCompat.TickHandler tickHandler;
    private final Map<UUID, ScheduledTask> playerTasks = new ConcurrentHashMap<>();
    private HitListener hitListener;

    public TickListener(JavaPlugin plugin, ISessionManager sessionManager, AICheck aiCheck,
            wtf.faceac.hologram.NametagManager nametagManager) {
        this.sessionManager = sessionManager;
        this.aiCheck = aiCheck;
        this.nametagManager = nametagManager;
        this.tickHandler = EventCompat.createTickHandler(plugin, this::onTick);
    }

    public void start() {
        tickHandler.start();
    }

    public void stop() {
        tickHandler.stop();
        for (ScheduledTask task : playerTasks.values()) {
            task.cancel();
        }
        playerTasks.clear();
    }

    public void setHitListener(HitListener hitListener) {
        this.hitListener = hitListener;
    }

    private void onTick() {
        int currentTick = tickHandler.getCurrentTick();
        if (hitListener != null) {
            hitListener.setCurrentTick(currentTick);
        }
    }

    public void startPlayerTask(Player player) {
        if (playerTasks.containsKey(player.getUniqueId())) {
            return;
        }

        try {
            ScheduledTask task = SchedulerManager.getAdapter().runEntitySyncRepeating(player, () -> {
                if (aiCheck != null) {
                    aiCheck.onTick(player);
                }
            }, 1L, 1L);
            playerTasks.put(player.getUniqueId(), task);
        } catch (Exception ignored) {
        }
    }

    public void stopPlayerTask(Player player) {
        ScheduledTask task = playerTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    public int getCurrentTick() {
        return tickHandler.getCurrentTick();
    }
}