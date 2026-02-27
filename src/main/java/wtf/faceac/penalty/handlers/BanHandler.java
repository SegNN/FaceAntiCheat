

package wtf.faceac.penalty.handlers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import wtf.faceac.penalty.ActionHandler;
import wtf.faceac.penalty.ActionType;
import wtf.faceac.penalty.BanAnimation;
import wtf.faceac.penalty.PenaltyContext;
import wtf.faceac.scheduler.SchedulerManager;

public class BanHandler implements ActionHandler {
    private final JavaPlugin plugin;
    private final BanAnimation animation;
    private boolean animationEnabled = true;

    public BanHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        this.animation = new BanAnimation(plugin);
    }

    @Override
    public void handle(String command, PenaltyContext context) {
        if (command == null || command.isEmpty()) {
            return;
        }
        Player player = null;
        if (context != null && context.getPlayerName() != null) {
            player = Bukkit.getPlayer(context.getPlayerName());
        }
        if (animationEnabled && player != null && player.isOnline()) {
            animation.playAnimation(player, command, context);
        } else {
            SchedulerManager.getAdapter().runSync(() -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            });
        }
    }

    public void setAnimationEnabled(boolean enabled) {
        this.animationEnabled = enabled;
    }

    public boolean isAnimationEnabled() {
        return animationEnabled;
    }

    public BanAnimation getAnimation() {
        return animation;
    }

    public void shutdown() {
        animation.shutdown();
    }

    @Override
    public ActionType getActionType() {
        return ActionType.BAN;
    }
}