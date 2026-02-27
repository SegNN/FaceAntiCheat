

package wtf.faceac.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class MessagesConfig {
    private final JavaPlugin plugin;
    private FileConfiguration config;
    private File configFile;

    public MessagesConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "messages.yml");
    }

    public void load() {
        if (!configFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public FileConfiguration getConfig() {
        if (config == null) {
            load();
        }
        return config;
    }

    public void reload() {
        load();
    }

    public String getPrefix() {
        return getConfig().getString("prefix", "&bAC &8>> &r");
    }

    public String getMessage(String key) {
        return getConfig().getString(key, "&cMessage not found: " + key);
    }

    public String getMessage(String key, String player, double probability, double buffer, int vl) {
        String msg = getMessage(key);
        String playerValue = player != null ? player : "";
        String probValue = String.format("%.2f", probability);
        String bufferValue = String.format("%.1f", buffer);
        String vlValue = String.valueOf(vl);
        return msg
                .replace("{PLAYER}", playerValue)
                .replace("{PROBABILITY}", probValue)
                .replace("{BUFFER}", bufferValue)
                .replace("{VL}", vlValue)
                .replace("<player>", playerValue)
                .replace("<probability>", probValue)
                .replace("<buffer>", bufferValue)
                .replace("<vl>", vlValue);
    }

    public String getMessage(String key, String... replacements) {
        String msg = getMessage(key);
        for (int i = 0; i < replacements.length - 1; i += 2) {
            msg = msg.replace(replacements[i], replacements[i + 1]);
        }
        return msg;
    }

    public java.util.List<String> getMessageList(String key) {
        return getConfig().getStringList(key);
    }
}
