

package wtf.faceac.scheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
public class FoliaScheduledTask implements wtf.faceac.scheduler.ScheduledTask {
    private final ScheduledTask task;
    private volatile boolean cancelled = false;
    public FoliaScheduledTask(ScheduledTask task) {
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