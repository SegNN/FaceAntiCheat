

package wtf.faceac.listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import wtf.faceac.Main;
import wtf.faceac.Permissions;
import wtf.faceac.alert.AlertManager;
import wtf.faceac.checks.AICheck;
import wtf.faceac.scheduler.SchedulerManager;
import wtf.faceac.session.SessionManager;
import wtf.faceac.violation.ViolationManager;

public class PlayerListener implements Listener {
    private final JavaPlugin plugin;
    private final AICheck aiCheck;
    private final AlertManager alertManager;
    private final ViolationManager violationManager;
    private final SessionManager sessionManager;
    private final TickListener tickListener;
    private final wtf.faceac.hologram.NametagManager nametagManager;
    private HitListener hitListener;

    public PlayerListener(JavaPlugin plugin, AICheck aiCheck, AlertManager alertManager,
            ViolationManager violationManager, SessionManager sessionManager,
            TickListener tickListener, wtf.faceac.hologram.NametagManager nametagManager) {
        this.plugin = plugin;
        this.aiCheck = aiCheck;
        this.alertManager = alertManager;
        this.violationManager = violationManager;
        this.sessionManager = sessionManager;
        this.tickListener = tickListener;
        this.nametagManager = nametagManager;
    }

    public void setHitListener(HitListener hitListener) {
        this.hitListener = hitListener;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (hitListener != null) {
            hitListener.cacheEntity(player);
        }
        if (tickListener != null) {
            tickListener.startPlayerTask(player);
        }

        try {
            SchedulerManager.getAdapter().runSyncDelayed(() -> {
                if (player.isOnline()) {
                    if (player.hasPermission(Permissions.ALERTS) || player.hasPermission(Permissions.ADMIN)) {
                        alertManager.enableAlerts(player);

                        if (plugin instanceof Main) {
                            Main main = (Main) plugin;
                            if (main.getUpdateChecker() != null && main.getUpdateChecker().isUpdateAvailable()) {
                                player.sendMessage(
                                        ChatColor.GOLD + "=================================================");
                                player.sendMessage(ChatColor.YELLOW + "A NEW FaceAC UPDATE IS AVAILABLE: "
                                        + ChatColor.WHITE + main.getUpdateChecker().getLatestVersion());
                                player.sendMessage(ChatColor.YELLOW + "Get it from GitHub: " + ChatColor.AQUA
                                        + "none");
                                player.sendMessage(
                                        ChatColor.GOLD + "=================================================");
                            }
                        }
                    }
                }
            }, 20L);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to schedule player join task: " + e.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        handlePlayerLeave(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerKick(PlayerKickEvent event) {
        handlePlayerLeave(event.getPlayer());
    }

    private void handlePlayerLeave(Player player) {
        if (hitListener != null) {
            hitListener.uncachePlayer(player);
        }
        if (tickListener != null) {
            tickListener.stopPlayerTask(player);
        }
        if (aiCheck != null) {
            aiCheck.handlePlayerQuit(player);
        }
        if (alertManager != null) {
            alertManager.handlePlayerQuit(player);
        }
        if (violationManager != null) {
            violationManager.handlePlayerQuit(player);
        }
        if (sessionManager != null) {
            sessionManager.removeAimProcessor(player.getUniqueId());
        }
        if (nametagManager != null) {
            nametagManager.handlePlayerQuit(player);
        }
    }
}
