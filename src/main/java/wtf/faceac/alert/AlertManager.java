

package wtf.faceac.alert;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import wtf.faceac.Main;
import wtf.faceac.Permissions;
import wtf.faceac.config.Config;
import wtf.faceac.config.MessagesConfig;
import wtf.faceac.scheduler.SchedulerAdapter;
import wtf.faceac.scheduler.SchedulerManager;
import wtf.faceac.util.ColorUtil;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Logger;

public class AlertManager {
    private final Logger logger;
    private final Set<UUID> playersWithAlerts;
    private final SchedulerAdapter scheduler;
    private Config config;
    private MessagesConfig messagesConfig;

    public AlertManager(Main plugin, Config config) {
        this.config = config;
        this.messagesConfig = plugin.getMessagesConfig();
        this.logger = plugin.getLogger();
        this.playersWithAlerts = new CopyOnWriteArraySet<>();
        this.scheduler = SchedulerManager.getAdapter();
    }

    private String getPrefix() {
        return ColorUtil.colorize(messagesConfig.getPrefix());
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public boolean toggleAlerts(Player player) {
        UUID uuid = player.getUniqueId();
        if (playersWithAlerts.contains(uuid)) {
            playersWithAlerts.remove(uuid);
            String msg = ColorUtil.colorize(messagesConfig.getMessage("alerts-disabled"));
            player.sendMessage(getPrefix() + msg);
            return false;
        } else {
            playersWithAlerts.add(uuid);
            String msg = ColorUtil.colorize(messagesConfig.getMessage("alerts-enabled"));
            player.sendMessage(getPrefix() + msg);
            return true;
        }
    }

    public void enableAlerts(Player player) {
        playersWithAlerts.add(player.getUniqueId());
    }

    public void disableAlerts(Player player) {
        playersWithAlerts.remove(player.getUniqueId());
    }

    public boolean hasAlertsEnabled(Player player) {
        return playersWithAlerts.contains(player.getUniqueId());
    }

    private boolean canReceiveAlerts(Player player) {
        return player.hasPermission(Permissions.ALERTS) || player.hasPermission(Permissions.ADMIN);
    }

    public void sendAlert(String suspectName, double probability, double buffer) {
        sendAlert(suspectName, probability, buffer, null);
    }

    public void sendAlert(String suspectName, double probability, double buffer, String modelName) {
        String message = formatAlertMessage(suspectName, probability, buffer, modelName);
        scheduler.runSync(() -> {
            for (UUID uuid : playersWithAlerts) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline() && canReceiveAlerts(player)) {
                    player.sendMessage(message);
                }
            }
            if (config.isAiConsoleAlerts()) {
                logger.info(ColorUtil.stripColors(message));
            }
        });
    }

    public void sendAlert(String suspectName, double probability, double buffer, int vl) {
        sendAlert(suspectName, probability, buffer, vl, null);
    }

    public void sendAlert(String suspectName, double probability, double buffer, int vl, String modelName) {
        String message = formatAlertMessage(suspectName, probability, buffer, vl, modelName);
        scheduler.runSync(() -> {
            for (UUID uuid : playersWithAlerts) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline() && canReceiveAlerts(player)) {
                    player.sendMessage(message);
                }
            }
            if (config.isAiConsoleAlerts()) {
                logger.info(ColorUtil.stripColors(message));
            }
        });
    }

    private String formatAlertMessage(String suspectName, double probability, double buffer, String modelName) {
        String template = messagesConfig.getMessage("alert-format", suspectName, probability, buffer, 0);
        String modelDisplay = modelName != null ? config.getModelDisplayName(modelName) : "Unknown";
        String checkName = resolveCheckName(modelName, modelDisplay);
        template = template
                .replace("{MODEL}", modelDisplay)
                .replace("<model>", modelDisplay)
                .replace("{CHECK}", checkName)
                .replace("<check>", checkName);
        return getPrefix() + ColorUtil.colorize(template);
    }

    private String formatAlertMessage(String suspectName, double probability, double buffer, int vl, String modelName) {
        String template = messagesConfig.getMessage("alert-format-vl", suspectName, probability, buffer, vl);
        String modelDisplay = modelName != null ? config.getModelDisplayName(modelName) : "Unknown";
        String checkName = resolveCheckName(modelName, modelDisplay);
        template = template
                .replace("{MODEL}", modelDisplay)
                .replace("<model>", modelDisplay)
                .replace("{CHECK}", checkName)
                .replace("<check>", checkName);
        return getPrefix() + ColorUtil.colorize(template);
    }

    private String resolveCheckName(String modelName, String modelDisplay) {
        String configuredCheckName = messagesConfig.getMessage("check-name");
        if (configuredCheckName != null
                && !configuredCheckName.isBlank()
                && !configuredCheckName.startsWith("&cMessage not found:")) {
            return configuredCheckName
                    .replace("{MODEL}", modelDisplay)
                    .replace("<model>", modelDisplay)
                    .replace("{MODEL_KEY}", modelName != null ? modelName : "");
        }
        return "Killaura";
    }

    public void handlePlayerQuit(Player player) {
        playersWithAlerts.remove(player.getUniqueId());
    }

    public boolean shouldAlert(double probability) {
        return probability >= config.getAiChatAlertThreshold();
    }

    public double getAlertThreshold() {
        return config.getAiChatAlertThreshold();
    }
}
