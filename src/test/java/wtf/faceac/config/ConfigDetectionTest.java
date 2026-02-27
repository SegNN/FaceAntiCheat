package wtf.faceac.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigDetectionTest {
    @Test
    void shouldResolveModelIdToHfInferenceUrl() {
        FileConfiguration yaml = new YamlConfiguration();
        yaml.set("detection.endpoint", "org/model-name");
        yaml.set("detection.server-type", "huggingface");
        yaml.set("detection.allow-http", false);

        Config config = createConfig(yaml);
        assertEquals("https://api-inference.huggingface.co/models/org/model-name", config.getServerAddress());
    }

    @Test
    void shouldFallbackWhenHttpNotAllowed() {
        FileConfiguration yaml = new YamlConfiguration();
        yaml.set("detection.endpoint", "http://localhost:8080/predict");
        yaml.set("detection.allow-http", false);

        Config config = createConfig(yaml);
        assertEquals(Config.DEFAULT_SERVER_ADDRESS, config.getServerAddress());
    }

    @Test
    void shouldClampTimeoutAndRetryRanges() {
        FileConfiguration yaml = new YamlConfiguration();
        yaml.set("detection.endpoint", "org/model");
        yaml.set("detection.timeout-ms", 999999);
        yaml.set("detection.retry.attempts", -5);
        yaml.set("detection.retry.backoff-ms", 20000);

        Config config = createConfig(yaml);
        assertEquals(120000, config.getDetectionTimeoutMs());
        assertEquals(0, config.getDetectionRetryAttempts());
        assertEquals(10000, config.getDetectionRetryBackoffMs());
    }

    private Config createConfig(FileConfiguration yaml) {
        JavaPlugin plugin = Mockito.mock(JavaPlugin.class);
        Mockito.when(plugin.getConfig()).thenReturn(yaml);
        Logger logger = Logger.getLogger("config-test");
        return new Config(plugin, logger);
    }
}
