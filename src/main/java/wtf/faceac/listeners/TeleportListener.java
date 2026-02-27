


package wtf.faceac.listeners;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import wtf.faceac.checks.AICheck;
public class TeleportListener implements Listener {
    private final AICheck aiCheck;
    public TeleportListener(AICheck aiCheck) {
        this.aiCheck = aiCheck;
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (aiCheck != null) {
            aiCheck.onTeleport(event.getPlayer());
        }
    }
}