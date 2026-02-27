

package wtf.faceac.checks;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import wtf.faceac.Main;
import wtf.faceac.alert.AlertManager;
import wtf.faceac.compat.WorldGuardCompat;
import wtf.faceac.config.Config;
import wtf.faceac.data.AIPlayerData;
import wtf.faceac.data.TickData;
import wtf.faceac.scheduler.SchedulerAdapter;
import wtf.faceac.scheduler.SchedulerManager;
import wtf.faceac.server.AIClientProvider;
import wtf.faceac.server.AIResponse;
import wtf.faceac.server.FlatBufferSerializer;
import wtf.faceac.server.IAIClient;
import wtf.faceac.violation.ViolationManager;
import wtf.faceac.util.GeyserUtil;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class AICheck {
    private final Main plugin;
    private final AIClientProvider clientProvider;
    private final AlertManager alertManager;
    private final ViolationManager violationManager;
    private final Logger logger;
    private final SchedulerAdapter schedulerAdapter;
    private final Map<UUID, AIPlayerData> playerData;
    private Config config;
    private WorldGuardCompat worldGuardCompat;
    private int sequence;
    private int step;

    public AICheck(Main plugin, Config config,
            AIClientProvider clientProvider,
            AlertManager alertManager, ViolationManager violationManager) {
        this.plugin = plugin;
        this.config = config;
        this.clientProvider = clientProvider;
        this.alertManager = alertManager;
        this.violationManager = violationManager;
        this.logger = plugin.getLogger();
        this.schedulerAdapter = SchedulerManager.getAdapter();
        this.playerData = new ConcurrentHashMap<>();
        this.sequence = config.getAiSequence();
        this.step = config.getAiStep();
        this.worldGuardCompat = new WorldGuardCompat(
                plugin.getLogger(),
                config.isWorldGuardEnabled(),
                config.getWorldGuardDisabledRegions());
    }

    public void setConfig(Config config) {
        this.config = config;
        this.sequence = config.getAiSequence();
        this.step = config.getAiStep();
        this.worldGuardCompat = new WorldGuardCompat(
                plugin.getLogger(),
                config.isWorldGuardEnabled(),
                config.getWorldGuardDisabledRegions());
    }

    public void onAttack(Player player, Entity target) {
        if (!config.isAiEnabled()) {
            return;
        }
        if (!(target instanceof Player)) {
            return;
        }
        if (worldGuardCompat.shouldBypassAICheck(player)) {
            plugin.debug("[AI] Skipping attack for " + player.getName() + " - in disabled WorldGuard region");
            return;
        }
        if (!player.isValid()) {
            return;
        }
        if (!player.isValid()) {
            return;
        }
        schedulerAdapter.runEntitySync(player, () -> {
            AIPlayerData data = getOrCreatePlayerData(player);
            if (data.isBedrock()) {
                plugin.debug("[AI] Skipping attack for " + player.getName() + " - Bedrock player detected");
                return;
            }
            if (!data.isInCombat()) {
                data.clearBuffer();
                data.getAimProcessor().reset();
                plugin.debug("[AI] New combat started for " + player.getName() + ", cleared old data");
            }
            data.onAttack();
            plugin.debug("[AI] Attack registered for " + player.getName() +
                    ", buffer=" + data.getBufferSize() + "/" + sequence);
        });
    }

    public void onTeleport(Player player) {
        if (!config.isAiEnabled()) {
            return;
        }
        if (!player.isValid()) {
            return;
        }
        schedulerAdapter.runEntitySync(player, () -> {
            AIPlayerData data = playerData.get(player.getUniqueId());
            if (data != null) {
                data.onTeleport();
                plugin.debug("[AI] Teleport registered for " + player.getName() + ", resetting data");
            }
        });
    }

    public void onTick(Player player) {
        if (!config.isAiEnabled()) {
            return;
        }
        if (!isClientAvailable()) {
            return;
        }
        if (!player.isValid()) {
            return;
        }
        schedulerAdapter.runEntitySync(player, () -> {
            AIPlayerData data = getOrCreatePlayerData(player);
            if (data.isBedrock())
                return;
            data.incrementTicksSinceAttack();
            if (data.getTicksSinceAttack() > sequence) {
                if (!data.isPendingRequest() && data.getBufferSize() >= sequence) {
                    plugin.debug("[AI] Combat ended for " + player.getName() +
                            ", sending final buffer (" + data.getBufferSize() + " ticks)");
                    data.setPendingRequest(true);
                    sendDataToAI(player, data);
                }
                if (!data.isPendingRequest() && data.getTicksSinceAttack() > sequence * 2 && data.getBufferSize() > 0) {
                    data.clearBuffer();
                }
                data.resetStepCounter();
                return;
            }
        });
    }

    public void onRotationPacket(Player player, float yaw, float pitch) {
        if (!config.isAiEnabled()) {
            return;
        }
        if (!isClientAvailable()) {
            return;
        }
        if (!player.isValid()) {
            return;
        }
        schedulerAdapter.runEntitySync(player, () -> {
            AIPlayerData data = playerData.get(player.getUniqueId());
            if (data == null) {
                return;
            }
            if (!data.isInCombat()) {
                return;
            }
            if (worldGuardCompat.shouldBypassAICheck(player)) {
                plugin.debug("[AI] Skipping rotation for " + player.getName() + " - in disabled WorldGuard region");
                return;
            }
            if (data.isBedrock())
                return;
            data.processTick(yaw, pitch);
            data.incrementStepCounter();
            if (data.shouldSendData(step, sequence)) {
                data.setPendingRequest(true);
                sendDataToAI(player, data);
                data.resetStepCounter();
            }
        });
    }

    private void sendDataToAI(Player player, AIPlayerData data) {
        List<TickData> ticks = data.getTickBuffer();
        if (ticks.size() < sequence) {
            plugin.debug("[AI] Not enough ticks for " + player.getName() +
                    ": " + ticks.size() + "/" + sequence);
            return;
        }
        IAIClient client = clientProvider.get();
        if (client == null) {
            return;
        }
        plugin.debug("[AI] Sending " + ticks.size() + " ticks for " + player.getName() +
                " (ticksSinceAttack=" + data.getTicksSinceAttack() + ")");
        if (config.isDebug()) {
            plugin.debug("[AI] === TICK BUFFER START ===");
            int i = 0;
            for (TickData tick : ticks) {
                plugin.debug("[AI] Tick[" + i + "]: dYaw=" + String.format("%.4f", tick.deltaYaw) +
                        ", dPitch=" + String.format("%.4f", tick.deltaPitch) +
                        ", aYaw=" + String.format("%.4f", tick.accelYaw) +
                        ", aPitch=" + String.format("%.4f", tick.accelPitch) +
                        ", jYaw=" + String.format("%.4f", tick.jerkYaw) +
                        ", jPitch=" + String.format("%.4f", tick.jerkPitch) +
                        ", gcdYaw=" + String.format("%.4f", tick.gcdErrorYaw) +
                        ", gcdPitch=" + String.format("%.4f", tick.gcdErrorPitch));
                i++;
            }
            plugin.debug("[AI] === TICK BUFFER END ===");
        }
        byte[] serialized = FlatBufferSerializer.serializeJsonFeatures(ticks, sequence);
        final UUID playerUuid = player.getUniqueId();
        final String playerName = player.getName();
        client.predictAsync(serialized, playerUuid.toString(), playerName)
                .thenAccept(response -> schedulerAdapter.runSync(() -> processResponse(playerUuid, playerName, data, response)))
                .exceptionally(error -> {
                    handleError(playerName, data, error);
                    return null;
                });
    }

    private boolean isClientAvailable() {
        return clientProvider != null && clientProvider.isAvailable();
    }

    private void processResponse(UUID playerUuid, String playerName, AIPlayerData data, AIResponse response) {
        schedulerAdapter.runSync(() -> {
            data.setPendingRequest(false);
            data.clearBuffer();
            if (response.hasError()) {
                if (response.getError().contains("INVALID_SEQUENCE")) {
                    handleInvalidSequence(response.getError());
                } else {
                    plugin.debug("[AI] Response error for " + playerName + ": " + response.getError());
                }
                return;
            }
            double probability = response.getProbability();
            String modelName = response.getModel();
            String action = response.getAction();
            boolean isOnlyAlert = config.isOnlyAlertForModel(modelName);

            plugin.debug("[AI] Response for " + playerName + ": probability=" +
                    String.format("%.3f", probability) + ", model=" + modelName +
                    ", action=" + action + ", onlyAlert=" + isOnlyAlert);

            if (!isOnlyAlert) {
                data.updateBuffer(probability, config.getAiBufferMultiplier(),
                        config.getAiBufferDecrease(), config.getAiAlertThreshold());
            } else {
                plugin.debug("[AI] Only-alert mode for model " + modelName + ", skipping buffer/punishment");
            }

            if (alertManager.shouldAlert(probability)) {
                alertManager.sendAlert(playerName, probability, data.getBuffer(), modelName);
            }

            if (!isOnlyAlert && data.shouldFlag(config.getAiBufferFlag())) {
                Player player = Bukkit.getPlayer(playerUuid);
                if (player != null && player.isOnline()) {
                    violationManager.handleFlag(player, probability, data.getBuffer());
                } else {
                    logger.warning("[AI] Player " + playerName + " went offline before punishment");
                }
                data.resetBuffer(config.getAiBufferResetOnFlag());
            }
        });

    }

    private void handleInvalidSequence(String error) {
        try {
            String[] parts = error.split(":");
            if (parts.length >= 2) {
                int newSequence = Integer.parseInt(parts[1].trim());
                if (newSequence > 0 && newSequence != this.sequence) {
                    logger.info("[AI] Updating sequence from " + this.sequence + " to " + newSequence);
                    this.sequence = newSequence;
                    for (AIPlayerData data : playerData.values()) {
                        data.clearBuffer();
                    }
                }
            }
        } catch (NumberFormatException e) {
            logger.warning("[AI] Failed to parse new sequence from error: " + error);
        }
    }

    private void handleError(String playerName, AIPlayerData data, Throwable error) {
        if (data != null) {
            data.setPendingRequest(false);
        }
        Throwable cause = error.getCause() != null ? error.getCause() : error;
        logger.warning("[AI] Error for " + playerName + ": " + cause.getMessage());
    }

    private AIPlayerData getOrCreatePlayerData(Player player) {
        return playerData.computeIfAbsent(player.getUniqueId(),
                uuid -> {
                    AIPlayerData data = new AIPlayerData(uuid, sequence);
                    if (GeyserUtil.isBedrockPlayer(uuid)) {
                        data.setBedrock(true);
                        logger.info("[AI] Detected Bedrock player: " + player.getName() + " - bypassing checks");
                    }
                    return data;
                });
    }

    public AIPlayerData getPlayerData(UUID playerId) {
        return playerData.get(playerId);
    }

    public void handlePlayerQuit(Player player) {
    }

    public void clearAll() {
        playerData.clear();
    }

    public int getSequence() {
        return sequence;
    }

    public int getStep() {
        return step;
    }

    public WorldGuardCompat getWorldGuardCompat() {
        return worldGuardCompat;
    }
}
