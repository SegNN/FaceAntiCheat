


package wtf.faceac.penalty.handlers;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import wtf.faceac.penalty.ActionHandler;
import wtf.faceac.penalty.ActionType;
import wtf.faceac.penalty.PenaltyContext;
import wtf.faceac.scheduler.SchedulerManager;
public class RawHandler implements ActionHandler {
    private final JavaPlugin plugin;
    public RawHandler(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    @Override
    public void handle(String command, PenaltyContext context) {
        if (command == null || command.isEmpty()) {
            return;
        }
        SchedulerManager.getAdapter().runSync(() -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        });
    }
    @Override
    public ActionType getActionType() {
        return ActionType.RAW;
    }
}