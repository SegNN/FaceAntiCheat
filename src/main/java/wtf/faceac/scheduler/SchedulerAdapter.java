

package wtf.faceac.scheduler;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
public interface SchedulerAdapter {
    ScheduledTask runSync(Runnable task);
    ScheduledTask runSyncDelayed(Runnable task, long delayTicks);
    ScheduledTask runSyncRepeating(Runnable task, long delayTicks, long periodTicks);
    ScheduledTask runAsync(Runnable task);
    ScheduledTask runAsyncDelayed(Runnable task, long delayTicks);
    ScheduledTask runAsyncRepeating(Runnable task, long delayTicks, long periodTicks);
    ScheduledTask runEntitySync(Entity entity, Runnable task);
    ScheduledTask runEntitySyncDelayed(Entity entity, Runnable task, long delayTicks);
    ScheduledTask runEntitySyncRepeating(Entity entity, Runnable task, long delayTicks, long periodTicks);
    ScheduledTask runRegionSync(Location location, Runnable task);
    ScheduledTask runRegionSyncDelayed(Location location, Runnable task, long delayTicks);
    ScheduledTask runRegionSyncRepeating(Location location, Runnable task, long delayTicks, long periodTicks);
    ServerType getServerType();
}