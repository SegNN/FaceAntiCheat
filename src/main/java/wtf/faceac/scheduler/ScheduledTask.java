

package wtf.faceac.scheduler;
public interface ScheduledTask {
    void cancel();
    boolean isCancelled();
    boolean isRunning();
}