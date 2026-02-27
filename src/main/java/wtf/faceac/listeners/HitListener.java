

package wtf.faceac.listeners;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import wtf.faceac.checks.AICheck;
import wtf.faceac.session.ISessionManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HitListener extends PacketListenerAbstract {
    private final ISessionManager sessionManager;
    private final AICheck aiCheck;
    private final Map<Integer, UUID> playerIdCache = new ConcurrentHashMap<>();

    public HitListener(ISessionManager sessionManager, AICheck aiCheck) {
        super(PacketListenerPriority.NORMAL);
        this.sessionManager = sessionManager;
        this.aiCheck = aiCheck;
    }

    public void setCurrentTick(int tick) {
        if (tick % 200 == 0) {
            playerIdCache.entrySet().removeIf(entry -> Bukkit.getPlayer(entry.getValue()) == null);
        }
    }

    public void cachePlayer(Player player) {
        if (player != null) {
            playerIdCache.put(player.getEntityId(), player.getUniqueId());
        }
    }

    public void uncachePlayer(Player player) {
        if (player != null) {
            playerIdCache.remove(player.getEntityId());
        }
    }

    public void cacheOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            cachePlayer(player);
        }
    }

    public void cacheEntity(org.bukkit.entity.Entity entity) {
        if (entity instanceof Player) {
            cachePlayer((Player) entity);
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        try {
            if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) {
                return;
            }
            WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
            if (packet.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                return;
            }
            Player attacker = (Player) event.getPlayer();
            if (attacker == null) {
                return;
            }
            int targetId = packet.getEntityId();
            Player target = getPlayerById(targetId);
            if (target == null) {
                return;
            }
            if (aiCheck != null) {
                aiCheck.onAttack(attacker, target);
            }
            sessionManager.onAttack(attacker);
        } catch (Exception e) {
        }
    }

    private Player getPlayerById(int entityId) {
        UUID uuid = playerIdCache.get(entityId);
        if (uuid != null) {
            return Bukkit.getPlayer(uuid);
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getEntityId() == entityId) {
                playerIdCache.put(entityId, player.getUniqueId());
                return player;
            }
        }
        return null;
    }
}