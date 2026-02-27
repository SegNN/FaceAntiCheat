


package wtf.faceac.datacollector;
import wtf.faceac.Main;
import wtf.faceac.session.ISessionManager;
public class DataCollectorFactory {
    public static ISessionManager createSessionManager(Main plugin) {
        return new wtf.faceac.session.SessionManager(plugin);
    }
}