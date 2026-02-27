

package wtf.faceac.scheduler;
import org.bukkit.scheduler.BukkitTask;
public class BukkitScheduledTask implements ScheduledTask {
    private final BukkitTask task;
    private volatile boolean cancelled = false;
    public BukkitScheduledTask(BukkitTask task) {
        this.task = task;
    }
    @Override
    public void cancel() {
        if (!cancelled) {
            cancelled = true;
            task.cancel();
        }
    }
    @Override
    public boolean isCancelled() {
        return cancelled || task.isCancelled();
    }
    @Override
    public boolean isRunning() {
        return !isCancelled();
    }
}