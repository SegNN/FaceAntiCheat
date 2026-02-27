


package wtf.faceac.data;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import wtf.faceac.Main;
import wtf.faceac.config.Label;
import wtf.faceac.util.AimProcessor;
public class DataSession {
    private final UUID uuid;
    private final String playerName;
    private final Label label;
    private final String comment;
    private final Queue<TickData> recordedTicks;
    private final Instant startTime;
    private final AimProcessor aimProcessor;
    private int ticksSinceAttack;
    private static final int COMBAT_TIMEOUT = 40;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    public DataSession(UUID uuid, String playerName, Label label, String comment) {
        this(uuid, playerName, label, comment, new AimProcessor());
    }
    public DataSession(UUID uuid, String playerName, Label label, String comment, AimProcessor aimProcessor) {
        this.uuid = uuid;
        this.playerName = playerName;
        this.label = label;
        this.comment = comment;
        this.recordedTicks = new ConcurrentLinkedQueue<>();
        this.startTime = Instant.now();
        this.aimProcessor = aimProcessor;
        this.ticksSinceAttack = COMBAT_TIMEOUT;
    }
    public void processTick(float yaw, float pitch) {
        lock.writeLock().lock();
        try {
            TickData tickData = aimProcessor.process(yaw, pitch);
            if (ticksSinceAttack < COMBAT_TIMEOUT) {
                recordedTicks.add(tickData);
            }
            ticksSinceAttack++;
        } finally {
            lock.writeLock().unlock();
        }
    }
    public void onAttack() {
        lock.writeLock().lock();
        try {
            ticksSinceAttack = 0;
        } finally {
            lock.writeLock().unlock();
        }
    }
    public int getTickCount() {
        lock.readLock().lock();
        try {
            return recordedTicks.size();
        } finally {
            lock.readLock().unlock();
        }
    }
    public boolean isInCombat() {
        lock.readLock().lock();
        try {
            return ticksSinceAttack < COMBAT_TIMEOUT;
        } finally {
            lock.readLock().unlock();
        }
    }
    public UUID getUuid() {
        return uuid;
    }
    public String getPlayerName() {
        return playerName;
    }
    public Label getLabel() {
        return label;
    }
    public String getComment() {
        return comment;
    }
    public Instant getStartTime() {
        return startTime;
    }
    public int getTicksSinceAttack() {
        lock.readLock().lock();
        try {
            return ticksSinceAttack;
        } finally {
            lock.readLock().unlock();
        }
    }
    public String generateFileName() {
        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss")
            .format(new Date(startTime.toEpochMilli()));
        String statusForFilename = label.name();
        if (comment != null && !comment.isEmpty()) {
            String sanitized = comment.replace(' ', '#')
                .replaceAll("[/\\\\?%*:|\"<>']", "-");
            statusForFilename = statusForFilename + "_" + sanitized;
        }
        return String.format("%s_%s_%s.csv", statusForFilename, playerName, timestamp);
    }
    public String generateCsvContent() {
        lock.readLock().lock();
        try {
            if (recordedTicks.isEmpty()) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            sb.append(TickData.getHeader()).append("\n");
            String cheatingStatus = "UNLABELED";
            if (label == Label.CHEAT) {
                cheatingStatus = "CHEAT";
            } else if (label == Label.LEGIT) {
                cheatingStatus = "LEGIT";
            }
            List<TickData> ticks = new ArrayList<>(recordedTicks);
            for (TickData tick : ticks) {
                sb.append(tick.toCsv(cheatingStatus)).append("\n");
            }
            return sb.toString();
        } finally {
            lock.readLock().unlock();
        }
    }
    public void saveAndClose(Main plugin) throws IOException {
        saveAndClose(plugin, null);
    }
    public void saveAndClose(Main plugin, String sessionFolder) throws IOException {
        String csvContent = generateCsvContent();
        if (csvContent.isEmpty()) {
            return;
        }
        File dataFolder;
        if (sessionFolder != null && !sessionFolder.isEmpty()) {
            dataFolder = new File(plugin.getDataFolder(), plugin.getConfig().getString("outputDirectory") + sessionFolder);
        } else {
            dataFolder = new File(plugin.getDataFolder(), "data");
        }
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        File outputFile = new File(dataFolder, generateFileName());
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write(csvContent);
        }
        plugin.getLogger().info("Saved " + recordedTicks.size() + " ticks to " + outputFile.getName());
    }
}