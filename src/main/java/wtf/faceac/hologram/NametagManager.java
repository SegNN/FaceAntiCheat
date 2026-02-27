package wtf.faceac.hologram;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import wtf.faceac.Main;

import wtf.faceac.checks.AICheck;
import wtf.faceac.data.AIPlayerData;
import wtf.faceac.scheduler.ScheduledTask;
import wtf.faceac.scheduler.SchedulerManager;
import wtf.faceac.util.ColorUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import javax.annotation.Nullable;

public class NametagManager implements Listener {
    private static final int HISTORY_DISPLAY_LIMIT = 5;
    private static final double DEFAULT_LINE_SPACING = 0.28D;
    private static final long ADMIN_CACHE_MILLIS = 2000L;
    private static final int VIEW_DISTANCE_SQUARED = 10000;

    private final JavaPlugin plugin;
    private final AICheck aiCheck;
    private final Map<UUID, Integer> topArmorStandIds = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> bottomArmorStandIds = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastSentTopText = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastSentBottomText = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> viewersMap = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> lastDataVersion = new ConcurrentHashMap<>();
    private final Map<UUID, String> cachedTopBuilt = new ConcurrentHashMap<>();
    private final Map<UUID, String> cachedBottomBuilt = new ConcurrentHashMap<>();
    private final Map<String, List<com.github.retrooper.packetevents.protocol.entity.data.EntityData<?>>> metadataCacheLegacy = new ConcurrentHashMap<>();
    private final Map<String, List<com.github.retrooper.packetevents.protocol.entity.data.EntityData<?>>> metadataCacheModern = new ConcurrentHashMap<>();
    private final Map<String, List<com.github.retrooper.packetevents.protocol.entity.data.EntityData<?>>> metadataCacheAdventure = new ConcurrentHashMap<>();
    private ScheduledTask task;
    private int cleanupCounter = 0;

    // --- Cached config values ---
    private volatile double cachedYOffset = 2.5;
    private volatile double cachedLineSpacing = DEFAULT_LINE_SPACING;
    private volatile String cachedTopFormat = "{TOP5}";
    private volatile String cachedBottomFormat = "&fAVG: {AVG_COLORED}";
    private volatile int cachedUpdateInterval = 1;

    // --- Admin cache ---
    private volatile List<Player> cachedAdmins = Collections.emptyList();
    private volatile long adminCacheTime = 0;

    // --- Tick counter for interval ---
    private int tickCounter = 0;

    public NametagManager(JavaPlugin plugin, AICheck aiCheck) {
        this.plugin = plugin;
        this.aiCheck = aiCheck;
    }

    /** Call this on start and whenever the hologram config is reloaded. */
    public void reloadCachedConfig() {
        org.bukkit.configuration.file.FileConfiguration config = ((Main) plugin).getHologramConfig().getConfig();
        cachedYOffset = config.getDouble("nametags.height_offset", 2.5);
        cachedLineSpacing = config.getDouble("nametags.line_spacing", DEFAULT_LINE_SPACING);
        cachedTopFormat = config.getString("nametags.top-format", "{TOP5}");
        cachedBottomFormat = config.getString("nametags.bottom-format", "&fAVG: {AVG_COLORED}");
        cachedUpdateInterval = Math.max(1, config.getInt("nametags.update_interval_ticks", 1));
        metadataCacheLegacy.clear();
        metadataCacheModern.clear();
        metadataCacheAdventure.clear();
    }

    public void start() {
        org.bukkit.configuration.file.FileConfiguration config = ((Main) plugin).getHologramConfig().getConfig();
        if (!config.getBoolean("nametags.enabled", true))
            return;

        reloadCachedConfig();

        Bukkit.getPluginManager().registerEvents(this, plugin);

        task = SchedulerManager.getAdapter().runSyncRepeating(this::globalTick, 1L, 1L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }

        Set<UUID> knownTargets = new HashSet<>(topArmorStandIds.keySet());
        knownTargets.addAll(bottomArmorStandIds.keySet());
        knownTargets.addAll(viewersMap.keySet());
        for (UUID targetId : knownTargets) {
            despawnForall(targetId);
        }
        topArmorStandIds.clear();
        bottomArmorStandIds.clear();
        lastSentTopText.clear();
        lastSentBottomText.clear();
        lastDataVersion.clear();
        cachedTopBuilt.clear();
        cachedBottomBuilt.clear();
        metadataCacheLegacy.clear();
        metadataCacheModern.clear();
        metadataCacheAdventure.clear();
        viewersMap.clear();
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!event.hasChangedPosition())
            return;

        UUID targetId = event.getPlayer().getUniqueId();

        Set<UUID> viewers = viewersMap.get(targetId);
        if (viewers == null || viewers.isEmpty())
            return;

        Integer topEntityId = topArmorStandIds.get(targetId);
        Integer bottomEntityId = bottomArmorStandIds.get(targetId);
        if (topEntityId == null && bottomEntityId == null)
            return;

        Location to = event.getTo();
        double x = to.getX();
        double y = to.getY();
        double z = to.getZ();
        double yOffset = cachedYOffset;
        double lineSpacing = cachedLineSpacing;

        WrapperPlayServerEntityTeleport topTeleport = null;
        WrapperPlayServerEntityTeleport bottomTeleport = null;
        if (topEntityId != null) {
            topTeleport = new WrapperPlayServerEntityTeleport(
                    topEntityId, new Vector3d(x, y + yOffset, z), 0f, 0f, false);
        }
        if (bottomEntityId != null) {
            bottomTeleport = new WrapperPlayServerEntityTeleport(
                    bottomEntityId, new Vector3d(x, y + yOffset - lineSpacing, z), 0f, 0f, false);
        }

        for (UUID viewerId : viewers) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer != null && viewer.isOnline()) {
                if (topTeleport != null) {
                    PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, topTeleport);
                }
                if (bottomTeleport != null) {
                    PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, bottomTeleport);
                }
            }
        }
    }

    private void globalTick() {
        if (++tickCounter < cachedUpdateInterval) {
            return;
        }
        tickCounter = 0;

        long now = System.currentTimeMillis();
        if (now - adminCacheTime > ADMIN_CACHE_MILLIS || cachedAdmins.isEmpty()) {
            List<Player> admins = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.hasPermission(wtf.faceac.Permissions.ADMIN) || p.hasPermission(wtf.faceac.Permissions.ALERTS)) {
                    admins.add(p);
                }
            }
            cachedAdmins = admins;
            adminCacheTime = now;
        }

        List<Player> admins = cachedAdmins;
        if (admins.isEmpty()) {
            return;
        }

        for (Player target : Bukkit.getOnlinePlayers()) {
            updateNametag(target, admins);
        }

        if (++cleanupCounter > 100) {
            cleanupCounter = 0;
            cleanupOfflineViewers();
        }
    }

    private void cleanupOfflineViewers() {
        topArmorStandIds.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
        bottomArmorStandIds.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
        lastSentTopText.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
        lastSentBottomText.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
        lastDataVersion.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
        cachedTopBuilt.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
        cachedBottomBuilt.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
        viewersMap.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
        for (Set<UUID> viewers : viewersMap.values()) {
            viewers.removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
        }
        Set<String> activeTexts = new HashSet<>();
        activeTexts.addAll(lastSentTopText.values());
        activeTexts.addAll(lastSentBottomText.values());
        metadataCacheLegacy.keySet().retainAll(activeTexts);
        metadataCacheModern.keySet().retainAll(activeTexts);
        metadataCacheAdventure.keySet().retainAll(activeTexts);
    }

    public void updateNametag(Player target, List<Player> admins) {
        UUID targetId = target.getUniqueId();
        AIPlayerData data = aiCheck.getPlayerData(targetId);
        if (data == null)
            return;

        int currentVersion = data.getDataVersion();
        Integer lastVersion = lastDataVersion.get(targetId);
        boolean dataChanged = (lastVersion == null || lastVersion != currentVersion);

        String topText, bottomText;
        if (dataChanged) {
            double avgProb = data.getAverageProbability();
            List<Double> history = data.getProbabilityHistory();
            String top5History = buildTopHistory(history);
            String avgPlain = formatProbability(avgProb);
            String avgColored = getColorInfo(avgProb);

            topText = applyPlaceholders(cachedTopFormat, top5History, avgPlain, avgColored);
            bottomText = applyPlaceholders(cachedBottomFormat, top5History, avgPlain, avgColored);

            lastDataVersion.put(targetId, currentVersion);
            cachedTopBuilt.put(targetId, topText);
            cachedBottomBuilt.put(targetId, bottomText);
        } else {
            topText = cachedTopBuilt.getOrDefault(targetId, "");
            bottomText = cachedBottomBuilt.getOrDefault(targetId, "");
        }

        int topEntityId = topArmorStandIds.computeIfAbsent(targetId,
                k -> ThreadLocalRandom.current().nextInt(1000000, 2000000));
        int bottomEntityId = bottomArmorStandIds.computeIfAbsent(targetId,
                k -> ThreadLocalRandom.current().nextInt(2000000, 3000000));

        double yOffset = cachedYOffset;
        double lineSpacing = cachedLineSpacing;

        Location targetLoc = target.getLocation();
        Location topLoc = targetLoc.clone().add(0, yOffset, 0);
        Location bottomLoc = targetLoc.clone().add(0, yOffset - lineSpacing, 0);

        String lastTopText = lastSentTopText.get(targetId);
        String lastBottomText = lastSentBottomText.get(targetId);
        boolean topChanged = !topText.equals(lastTopText);
        boolean bottomChanged = !bottomText.equals(lastBottomText);

        for (Player viewer : admins) {
            if (viewer.getUniqueId().equals(targetId))
                continue;

            Location viewerLoc = viewer.getLocation();
            if (!viewerLoc.getWorld().equals(targetLoc.getWorld()) ||
                    viewerLoc.distanceSquared(targetLoc) > VIEW_DISTANCE_SQUARED) {
                removeViewer(targetId, viewer);
                continue;
            }

            Set<UUID> viewers = viewersMap.computeIfAbsent(targetId, k -> ConcurrentHashMap.newKeySet());
            boolean isNewViewer = viewers.add(viewer.getUniqueId());

            updateForLine(viewer, topEntityId, topLoc, topText, topChanged, isNewViewer);
            updateForLine(viewer, bottomEntityId, bottomLoc, bottomText, bottomChanged, isNewViewer);
        }

        if (topChanged) {
            lastSentTopText.put(targetId, topText);
        }
        if (bottomChanged) {
            lastSentBottomText.put(targetId, bottomText);
        }
    }

    private String getColorInfo(double val) {
        double percent = val * 100.0;
        return getGradient(percent) + formatProbability(val);
    }

    public void updateForLine(Player viewer, int entityId, Location loc, String text, boolean textChanged,
                              boolean shouldSpawn) {
        if (shouldSpawn) {
            WrapperPlayServerSpawnEntity spawn = new WrapperPlayServerSpawnEntity(
                    entityId, Optional.of(UUID.randomUUID()), EntityTypes.ARMOR_STAND,
                    new Vector3d(loc.getX(), loc.getY(), loc.getZ()), 0f, 0f, 0f, 0, Optional.empty());
            PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, spawn);
        }

        if (shouldSpawn || textChanged) {
            List<com.github.retrooper.packetevents.protocol.entity.data.EntityData<?>> metadata = getCachedMetadata(
                    viewer, text);
            WrapperPlayServerEntityMetadata metadataPacket = new WrapperPlayServerEntityMetadata(entityId, metadata);
            PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, metadataPacket);
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        for (UUID targetId : viewersMap.keySet()) {
            removeViewer(targetId, event.getPlayer());
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getFrom().getWorld() != event.getTo().getWorld())
            return;
        if (event.getFrom().distanceSquared(event.getTo()) > 2500) {
            for (UUID targetId : viewersMap.keySet()) {
                removeViewer(targetId, event.getPlayer());
            }
        }
    }

    public void handlePlayerQuit(Player player) {
        UUID uuid = player.getUniqueId();
        despawnForall(uuid);

        lastDataVersion.remove(uuid);
        cachedTopBuilt.remove(uuid);
        cachedBottomBuilt.remove(uuid);

        for (UUID targetId : viewersMap.keySet()) {
            removeViewer(targetId, player);
        }
    }

    private void removeViewer(UUID targetId, Player viewer) {
        Set<UUID> viewers = viewersMap.get(targetId);
        if (viewers != null && viewers.remove(viewer.getUniqueId())) {
            Integer topId = topArmorStandIds.get(targetId);
            Integer bottomId = bottomArmorStandIds.get(targetId);
            if (topId != null || bottomId != null) {
                int[] entities;
                if (topId != null && bottomId != null) {
                    entities = new int[] { topId, bottomId };
                } else if (topId != null) {
                    entities = new int[] { topId };
                } else {
                    entities = new int[] { bottomId };
                }
                WrapperPlayServerDestroyEntities destroy = new WrapperPlayServerDestroyEntities(entities);
                PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, destroy);
            }
        }
    }

    private List<com.github.retrooper.packetevents.protocol.entity.data.EntityData<?>> getCachedMetadata(
            Player viewer, String text) {
        com.github.retrooper.packetevents.protocol.player.ClientVersion clientVersion = PacketEvents.getAPI()
                .getPlayerManager().getClientVersion(viewer);
        int version = clientVersion != null ? clientVersion.getProtocolVersion() : 770;

        if (version >= 766) {
            return metadataCacheAdventure.computeIfAbsent(text, t -> buildMetadata(t, 2));
        } else if (version >= 393) {
            return metadataCacheModern.computeIfAbsent(text, t -> buildMetadata(t, 1));
        } else {
            return metadataCacheLegacy.computeIfAbsent(text, t -> buildMetadata(t, 0));
        }
    }

    private List<com.github.retrooper.packetevents.protocol.entity.data.EntityData<?>> buildMetadata(String text, int versionTier) {
        List<com.github.retrooper.packetevents.protocol.entity.data.EntityData<?>> metadata = new ArrayList<>(4);

        metadata.add(new com.github.retrooper.packetevents.protocol.entity.data.EntityData<Byte>(
                0, com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.BYTE, (byte) 0x20));

        String colorized = translateHexes(text);

        if (versionTier == 2) {
            net.kyori.adventure.text.Component component = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(colorized);
            metadata.add(
                    new com.github.retrooper.packetevents.protocol.entity.data.EntityData<Optional<net.kyori.adventure.text.Component>>(
                            2,
                            com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.OPTIONAL_ADV_COMPONENT,
                            Optional.of(component)));
            metadata.add(new com.github.retrooper.packetevents.protocol.entity.data.EntityData<Boolean>(
                    3, com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.BOOLEAN, true));
        } else if (versionTier == 1) {
            net.kyori.adventure.text.Component component = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(colorized);
            String json = com.github.retrooper.packetevents.util.adventure.AdventureSerializer.getGsonSerializer()
                    .serialize(component);
            metadata.add(new com.github.retrooper.packetevents.protocol.entity.data.EntityData<Optional<String>>(
                    2, com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.OPTIONAL_COMPONENT,
                    Optional.of(json)));
            metadata.add(new com.github.retrooper.packetevents.protocol.entity.data.EntityData<Boolean>(
                    3, com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.BOOLEAN, true));
        } else {
            metadata.add(new com.github.retrooper.packetevents.protocol.entity.data.EntityData<String>(
                    2, com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.STRING, colorized));
            metadata.add(new com.github.retrooper.packetevents.protocol.entity.data.EntityData<Boolean>(
                    3, com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.BOOLEAN, true));
            metadata.add(new com.github.retrooper.packetevents.protocol.entity.data.EntityData<Byte>(
                    15, com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.BYTE,
                    (byte) 0x10));
        }

        return Collections.unmodifiableList(metadata);
    }


    private void despawnForall(UUID targetId) {
        Integer topId = topArmorStandIds.remove(targetId);
        Integer bottomId = bottomArmorStandIds.remove(targetId);
        lastSentTopText.remove(targetId);
        lastSentBottomText.remove(targetId);
        Set<UUID> viewers = viewersMap.remove(targetId);

        if (viewers == null || (topId == null && bottomId == null))
            return;

        int[] entities;
        if (topId != null && bottomId != null) {
            entities = new int[] { topId, bottomId };
        } else if (topId != null) {
            entities = new int[] { topId };
        } else {
            entities = new int[] { bottomId };
        }

        WrapperPlayServerDestroyEntities destroy = new WrapperPlayServerDestroyEntities(entities);
        for (UUID viewerId : viewers) {
            Player p = Bukkit.getPlayer(viewerId);
            if (p != null && p.isOnline())
                PacketEvents.getAPI().getPlayerManager().sendPacket(p, destroy);
        }
    }

    private String applyPlaceholders(String format, String topHistory, String avgPlain, String avgColored) {
        String safeFormat = format == null ? "" : format;
        return safeFormat
                .replace("{AVG}", avgPlain)
                .replace("{AVG_COLORED}", avgColored)
                .replace("{TOP5}", topHistory)
                .replace("{HISTORY}", topHistory);
    }

    private String buildTopHistory(List<Double> history) {
        if (history == null || history.isEmpty()) {
            return "&7- &7- &7- &7- &7-";
        }
        int size = history.size();
        int start = Math.max(0, size - HISTORY_DISPLAY_LIMIT);
        StringBuilder sb = new StringBuilder();
        for (int i = size - 1; i >= start; i--) {
            sb.append(getColorInfo(history.get(i))).append(" ");
        }
        int missing = HISTORY_DISPLAY_LIMIT - (size - start);
        for (int i = 0; i < missing; i++) {
            sb.append("&7- ");
        }
        return sb.toString().trim();
    }

    private String formatProbability(double value) {
        int v = (int) (value * 10000 + 0.5);
        if (v < 0) v = 0;
        if (v > 99999) v = 99999;
        char[] buf = new char[6];
        buf[0] = (char) ('0' + v / 10000);
        buf[1] = '.';
        buf[2] = (char) ('0' + (v / 1000) % 10);
        buf[3] = (char) ('0' + (v / 100) % 10);
        buf[4] = (char) ('0' + (v / 10) % 10);
        buf[5] = (char) ('0' + v % 10);
        return new String(buf);
    }

    public static String getGradient(double value) {
        double percent = Math.max(0.0, Math.min(100.0, value)) / 100.0;

        int r, g;

        if (percent < 0.5) {
            g = 255;
            r = (int) (255 * percent * 2);
        } else {
            r = 255;
            g = (int) (255 * (1 - percent) * 2);
        }

        return String.format("&#%02X%02X%02X", r, g, 0);
    }


    public static String translateHexes(@Nullable final String textToTranslate) {
        if(textToTranslate == null) return "";
        final char altColorChar = '&';
        final StringBuilder b = new StringBuilder();
        final char[] mess = textToTranslate.toCharArray();
        boolean color = false, hashtag = false, doubleTag = false;
        char tmp; // Used in loops

        for (int i = 0; i < mess.length; ) { // I increment is handled case by case for speed

            final char c = mess[i];

            if (doubleTag) { // DoubleTag module
                doubleTag = false;

                final int max = i + 3;

                if (max <= mess.length) {
                    // There might be a hex color here
                    boolean match = true;

                    for (int n = i; n < max; n++) {
                        tmp = mess[n];
                        // The order of the checks below is meant to improve performances (i.e. capital letters check is at the end)
                        if (!((tmp >= '0' && tmp <= '9') || (tmp >= 'a' && tmp <= 'f') || (tmp >= 'A' && tmp <= 'F'))) {
                            // It wasn't a hex color, appending found chars to the StringBuilder and continue the for loop
                            match = false;
                            break;
                        }
                    }

                    if (match) {
                        b.append(ChatColor.COLOR_CHAR);
                        b.append('x');

                        // Copy colors with a color code in between
                        for (; i < max; i++) {
                            tmp = mess[i];
                            b.append(ChatColor.COLOR_CHAR);
                            b.append(tmp);
                            // Double the color code
                            b.append(ChatColor.COLOR_CHAR);
                            b.append(tmp);
                        }

                        // I increment has been already done
                        continue;
                    }
                }

                b.append(altColorChar);
                b.append("##");
                // Malformed hex, let's carry on checking mess[i]
            }

            if (hashtag) { // Hashtag module
                hashtag = false;

                // Check for double hashtag (&##123 => &#112233)
                if (c == '#') {
                    doubleTag = true;
                    i++;
                    continue;
                }

                final int max = i + 6;

                if (max <= mess.length) {
                    // There might be a hex color here
                    boolean match = true;

                    for (int n = i; n < max; n++) {
                        tmp = mess[n];
                        // The order of the checks below is meant to improve performances (i.e. capital letters check is at the end)
                        if (!((tmp >= '0' && tmp <= '9') || (tmp >= 'a' && tmp <= 'f') || (tmp >= 'A' && tmp <= 'F'))) {
                            // It wasn't a hex color, appending found chars to the StringBuilder and continue the for loop
                            match = false;
                            break;
                        }
                    }

                    if (match) {
                        b.append(ChatColor.COLOR_CHAR);
                        b.append('x');

                        // Copy colors with a color code in between
                        for (; i < max; i++) {
                            b.append(ChatColor.COLOR_CHAR);
                            b.append(mess[i]);
                        }
                        // I increment has been already done
                        continue;
                    }
                }

                b.append(altColorChar);
                b.append('#');
                // Malformed hex, let's carry on checking mess[i]
            }


            if (color) { // Color module
                color = false;

                if (c == '#') {
                    hashtag = true;
                    i++;
                    continue;
                }

                // The order of the checks below is meant to improve performances (i.e. capital letters check is at the end)
                if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || c == 'r' || (c >= 'k' && c <= 'o') || (c >= 'A' && c <= 'F') || c == 'R' || (c >= 'K' && c <= 'O')) {
                    b.append(ChatColor.COLOR_CHAR);
                    b.append(c);
                    i++;
                    continue;
                }

                b.append(altColorChar);
                // Not a valid color, let's carry on checking mess[i]
            }

            // Base case
            if (c == altColorChar) { // c == '&'
                color = true;
                i++;
                continue;
            }

            // None matched, append current character
            b.append(c);
            i++;

        }

        // Append '&' if '&' was the last character of the string
        if (color)
            b.append(altColorChar);
        else // color and hashtag cannot be true at the same time
            // Append "&#" if "&#" were the last characters of the string
            if (hashtag) {
                b.append(altColorChar);
                b.append('#');
            } else // color, hashtag, and doubleTag cannot be true at the same time
                // Append "&##" if "&##" were the last characters of the string
                if (doubleTag) {
                    b.append(altColorChar);
                    b.append("##");
                }

        return b.toString();
    }
}
