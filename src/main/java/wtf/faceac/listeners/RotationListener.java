

package wtf.faceac.listeners;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import wtf.faceac.checks.AICheck;
import wtf.faceac.session.ISessionManager;
import org.bukkit.entity.Player;

public class RotationListener extends PacketListenerAbstract {
    private final ISessionManager sessionManager;
    private final AICheck aiCheck;

    public RotationListener(ISessionManager sessionManager, AICheck aiCheck) {
        super(PacketListenerPriority.NORMAL);
        this.sessionManager = sessionManager;
        this.aiCheck = aiCheck;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        try {
            if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
                return;
            }
            Player player = (Player) event.getPlayer();
            if (player == null) {
                return;
            }
            WrapperPlayClientPlayerFlying packet = new WrapperPlayClientPlayerFlying(event);
            if (!packet.hasRotationChanged()) {
                return;
            }
            float yaw = packet.getLocation().getYaw();
            float pitch = packet.getLocation().getPitch();
            if (aiCheck != null) {
                aiCheck.onRotationPacket(player, yaw, pitch);
            }
            if (sessionManager.hasActiveSession(player)) {
                sessionManager.onTick(player, yaw, pitch);
            }
        } catch (Exception e) {
        }
    }
}