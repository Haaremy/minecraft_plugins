package de.haaremy.hmylobby.jukebox;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.Jukebox;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;

public class JukeboxManager {

    // ====== DISC DURATIONS (ticks) ======

    private static final Map<Material, Long> DISC_DURATIONS = new HashMap<>();
    static {
        DISC_DURATIONS.put(Material.MUSIC_DISC_13,        178L * 20);
        DISC_DURATIONS.put(Material.MUSIC_DISC_CAT,       185L * 20);
        DISC_DURATIONS.put(Material.MUSIC_DISC_BLOCKS,    345L * 20);
        DISC_DURATIONS.put(Material.MUSIC_DISC_CHIRP,     185L * 20);
        DISC_DURATIONS.put(Material.MUSIC_DISC_FAR,       174L * 20);
        DISC_DURATIONS.put(Material.MUSIC_DISC_MALL,      197L * 20);
        DISC_DURATIONS.put(Material.MUSIC_DISC_MELLOHI,    96L * 20);
        DISC_DURATIONS.put(Material.MUSIC_DISC_STAL,      150L * 20);
        DISC_DURATIONS.put(Material.MUSIC_DISC_STRAD,     188L * 20);
        DISC_DURATIONS.put(Material.MUSIC_DISC_WARD,      251L * 20);
        DISC_DURATIONS.put(Material.MUSIC_DISC_11,         71L * 20);
        DISC_DURATIONS.put(Material.MUSIC_DISC_WAIT,      238L * 20);
        DISC_DURATIONS.put(Material.MUSIC_DISC_OTHERSIDE, 195L * 20);
        DISC_DURATIONS.put(Material.MUSIC_DISC_5,         178L * 20);
        DISC_DURATIONS.put(Material.MUSIC_DISC_PIGSTEP,   149L * 20);
        DISC_DURATIONS.put(Material.MUSIC_DISC_RELIC,     218L * 20);
    }

    /** Repeat interval for non-live endless streams (~5 min). */
    private static final long STREAM_REPEAT_TICKS = 6000L;

    // ====== SELECTION STATE ======

    public enum SelectionAction { CREATE, ADD_DISKBOX }
    public record PendingSelection(SelectionAction action, String jukeboxId) {}

    // ====== FIELDS ======

    private final Plugin plugin;
    private final Logger logger;
    private final Map<String, JukeboxData> jukeboxes = new LinkedHashMap<>();
    private final JukeboxConfig config;
    private final Map<UUID, PendingSelection> pendingSelections = new HashMap<>();

    public JukeboxManager(Plugin plugin, Path hmySettingsDir) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.config = new JukeboxConfig(hmySettingsDir, logger);
        config.load(jukeboxes);
    }

    // ====== SELECTION FLOW ======

    public void beginCreate(Player player, String id) {
        if (jukeboxes.containsKey(id)) {
            player.sendMessage("§cEine Jukebox mit ID §e" + id + "§c existiert bereits.");
            return;
        }
        pendingSelections.put(player.getUniqueId(), new PendingSelection(SelectionAction.CREATE, id));
        player.sendMessage("§6Rechtsklick die Jukebox mit dem §egoldenen Schwert§6.");
    }

    public void beginAddDiskbox(Player player, String id) {
        JukeboxData data = jukeboxes.get(id);
        if (data == null) {
            player.sendMessage("§cUnbekannte Jukebox: §e" + id);
            return;
        }
        pendingSelections.put(player.getUniqueId(), new PendingSelection(SelectionAction.ADD_DISKBOX, id));
        player.sendMessage("§6Rechtsklick die Truhe mit dem §egoldenen Schwert§6.");
    }

    public boolean hasPendingSelection(UUID uuid) {
        return pendingSelections.containsKey(uuid);
    }

    public void clearPendingSelection(UUID uuid) {
        pendingSelections.remove(uuid);
    }

    public void handleSelection(Player player, Block block) {
        PendingSelection pending = pendingSelections.remove(player.getUniqueId());
        if (pending == null) return;

        if (pending.action() == SelectionAction.CREATE) {
            if (block.getType() != Material.JUKEBOX) {
                player.sendMessage("§cDas ist keine Jukebox. Aktion abgebrochen.");
                return;
            }
            JukeboxData data = new JukeboxData(pending.jukeboxId(), block.getLocation());
            jukeboxes.put(pending.jukeboxId(), data);
            config.save(jukeboxes);
            player.sendMessage("§6✓ Jukebox §e" + pending.jukeboxId() + "§6 registriert! Lege eine Disk ein und nutze §e/jukebox " + pending.jukeboxId() + " play endless§6.");

        } else if (pending.action() == SelectionAction.ADD_DISKBOX) {
            if (!(block.getState() instanceof Container)) {
                player.sendMessage("§cDas ist kein Container. Aktion abgebrochen.");
                return;
            }
            JukeboxData data = jukeboxes.get(pending.jukeboxId());
            if (data == null) return;
            data.chestLoc = block.getLocation();
            config.save(jukeboxes);
            doStartDiskbox(data);
            player.sendMessage("§6✓ Diskbox §e" + pending.jukeboxId() + "§6 verknüpft und gestartet!");
        }
    }

    // ====== PLAYBACK ======

    /** Start endless disc loop. If mode is already STREAM, enables stream repeat instead. */
    public boolean startEndless(String id, Player feedback) {
        JukeboxData data = jukeboxes.get(id);
        if (data == null) {
            if (feedback != null) feedback.sendMessage("§cUnbekannte Jukebox: §e" + id);
            return false;
        }

        // Special case: endless on an active stream
        if (data.mode == JukeboxMode.STREAM && data.streamUrl != null) {
            data.streamEndless = true;
            config.save(jukeboxes);
            scheduleStreamRepeat(data);
            if (feedback != null) feedback.sendMessage("§6✓ Stream §e" + id + "§6 wird endlos wiederholt.");
            return true;
        }

        Block block = data.jukeboxLoc.getBlock();
        if (!(block.getState() instanceof Jukebox jukebox)) {
            if (feedback != null) feedback.sendMessage("§cAn der Jukebox-Position befindet sich keine Jukebox.");
            return false;
        }

        // Use cached disc or read from block
        Material disc = isMusicDisc(data.currentDisc) ? data.currentDisc : jukebox.getPlaying();
        if (!isMusicDisc(disc)) {
            if (feedback != null) feedback.sendMessage("§cKeine Disk in der Jukebox §e" + id + "§c.");
            return false;
        }

        cancelTask(data);
        data.currentDisc = disc;
        data.mode = JukeboxMode.ENDLESS;
        jukebox.setPlaying(disc);
        jukebox.startPlaying();
        jukebox.update();
        scheduleEndlessNext(data);
        if (feedback != null) feedback.sendMessage("§6✓ Jukebox §e" + id + "§6 spielt §e" + discName(disc) + "§6 endlos.");
        return true;
    }

    private void scheduleEndlessNext(JukeboxData data) {
        long duration = DISC_DURATIONS.getOrDefault(data.currentDisc, 200L * 20);
        data.currentTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (data.mode != JukeboxMode.ENDLESS) return;
            Block block = data.jukeboxLoc.getBlock();
            if (!(block.getState() instanceof Jukebox jukebox)) return;
            jukebox.stopPlaying();
            jukebox.setPlaying(data.currentDisc);
            jukebox.startPlaying();
            jukebox.update();
            scheduleEndlessNext(data);
        }, duration - 10L);
    }

    /** Start diskbox using an already-linked chest. */
    public boolean startDiskbox(String id, Player feedback) {
        JukeboxData data = jukeboxes.get(id);
        if (data == null) {
            if (feedback != null) feedback.sendMessage("§cUnbekannte Jukebox: §e" + id);
            return false;
        }
        if (data.chestLoc == null) {
            if (feedback != null) feedback.sendMessage("§cKeine Diskbox für §e" + id + "§c verknüpft. Nutze §e/jukebox " + id + " add diskbox§c.");
            return false;
        }
        doStartDiskbox(data);
        if (feedback != null) feedback.sendMessage("§6✓ Diskbox §e" + id + "§6 gestartet.");
        return true;
    }

    private void doStartDiskbox(JukeboxData data) {
        cancelTask(data);
        data.mode = JukeboxMode.DISKBOX;
        data.diskboxIndex = 0;
        playNextDiskboxDisc(data);
    }

    private void playNextDiskboxDisc(JukeboxData data) {
        List<Material> discs = getDiscsFromChest(data.chestLoc);
        if (discs.isEmpty()) {
            data.mode = JukeboxMode.STOPPED;
            return;
        }
        data.diskboxIndex = data.diskboxIndex % discs.size();
        Material disc = discs.get(data.diskboxIndex);
        data.currentDisc = disc;
        data.diskboxIndex++;

        Block block = data.jukeboxLoc.getBlock();
        if (!(block.getState() instanceof Jukebox jukebox)) return;
        jukebox.stopPlaying();
        jukebox.setPlaying(disc);
        jukebox.startPlaying();
        jukebox.update();

        long duration = DISC_DURATIONS.getOrDefault(disc, 200L * 20);
        data.currentTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (data.mode != JukeboxMode.DISKBOX) return;
            playNextDiskboxDisc(data);
        }, duration - 10L);
    }

    /** Set stream URL, play to all online players, detect live asynchronously. */
    public boolean setStream(String id, String url, Player feedback) {
        JukeboxData data = jukeboxes.get(id);
        if (data == null) {
            if (feedback != null) feedback.sendMessage("§cUnbekannte Jukebox: §e" + id);
            return false;
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            if (feedback != null) feedback.sendMessage("§cNur §ehttp://§c und §ehttps://§c URLs erlaubt.");
            return false;
        }
        cancelTask(data);
        data.streamUrl = url;
        data.streamEndless = false;
        data.mode = JukeboxMode.STREAM;
        config.save(jukeboxes);
        if (feedback != null) feedback.sendMessage("§7⏳ Stream wird geprüft...");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean live = isLiveStream(url);
            Bukkit.getScheduler().runTask(plugin, () -> {
                data.streamLive = live;
                playStreamToAll(data);
                if (feedback != null) {
                    feedback.sendMessage("§6✓ Jukebox §e" + id + "§6 spielt Stream"
                            + (live ? " §7(Live)" : "") + "§6. Nutze §e/jukebox " + id + " play endless§6 zum Wiederholen.");
                }
            });
        });
        return true;
    }

    private void playStreamToAll(JukeboxData data) {
        if (data.streamUrl == null) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            playViaOpenAudioMc(player, data.streamUrl);
        }
    }

    private void scheduleStreamRepeat(JukeboxData data) {
        cancelTask(data);
        if (data.streamLive) return;
        data.currentTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (data.mode != JukeboxMode.STREAM || !data.streamEndless) {
                cancelTask(data);
                return;
            }
            playStreamToAll(data);
        }, STREAM_REPEAT_TICKS, STREAM_REPEAT_TICKS);
    }

    /** Stop playback and cancel scheduled tasks. */
    public boolean stopPlayback(String id, Player feedback) {
        JukeboxData data = jukeboxes.get(id);
        if (data == null) {
            if (feedback != null) feedback.sendMessage("§cUnbekannte Jukebox: §e" + id);
            return false;
        }
        cancelTask(data);
        data.mode = JukeboxMode.STOPPED;
        data.streamEndless = false;
        Block block = data.jukeboxLoc.getBlock();
        if (block.getState() instanceof Jukebox jukebox) {
            jukebox.stopPlaying();
            jukebox.update();
        }
        if (feedback != null) feedback.sendMessage("§6✓ Jukebox §e" + id + "§6 gestoppt.");
        return true;
    }

    /** Stop all named jukeboxes, then restart them all in the same server tick. */
    public boolean syncJukeboxes(List<String> ids, Player feedback) {
        List<JukeboxData> toSync = new ArrayList<>();
        for (String id : ids) {
            JukeboxData data = jukeboxes.get(id);
            if (data == null) {
                if (feedback != null) feedback.sendMessage("§cUnbekannte Jukebox: §e" + id);
                return false;
            }
            toSync.add(data);
        }

        // Snapshot current modes before stopping
        Map<JukeboxData, JukeboxMode> modes = new IdentityHashMap<>();
        for (JukeboxData data : toSync) {
            modes.put(data, data.mode);
        }

        // Cancel tasks and stop audio
        for (JukeboxData data : toSync) {
            if (data.currentTask != null) {
                data.currentTask.cancel();
                data.currentTask = null;
            }
            Block block = data.jukeboxLoc.getBlock();
            if (block.getState() instanceof Jukebox jukebox) {
                jukebox.stopPlaying();
                jukebox.update();
            }
        }

        // Restart all in the same tick
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (JukeboxData data : toSync) {
                JukeboxMode prevMode = modes.get(data);
                data.mode = prevMode;
                switch (prevMode) {
                    case ENDLESS -> {
                        if (isMusicDisc(data.currentDisc)) {
                            Block block = data.jukeboxLoc.getBlock();
                            if (block.getState() instanceof Jukebox jukebox) {
                                jukebox.setPlaying(data.currentDisc);
                                jukebox.startPlaying();
                                jukebox.update();
                                scheduleEndlessNext(data);
                            }
                        }
                    }
                    case DISKBOX -> {
                        data.diskboxIndex = 0;
                        playNextDiskboxDisc(data);
                    }
                    case STREAM -> {
                        if (data.streamUrl != null) {
                            playStreamToAll(data);
                            if (data.streamEndless && !data.streamLive) {
                                scheduleStreamRepeat(data);
                            }
                        }
                    }
                    default -> {} // STOPPED — nothing to do
                }
            }
        });

        if (feedback != null) feedback.sendMessage("§6✓ §e" + toSync.size() + "§6 Jukeboxen synchronisiert.");
        return true;
    }

    // ====== PROTECTION ======

    /** Returns true if the given block location belongs to a managed jukebox. */
    public boolean isManaged(Location loc) {
        int bx = loc.getBlockX(), by = loc.getBlockY(), bz = loc.getBlockZ();
        for (JukeboxData data : jukeboxes.values()) {
            Location jloc = data.jukeboxLoc;
            if (jloc.getWorld().equals(loc.getWorld())
                    && jloc.getBlockX() == bx
                    && jloc.getBlockY() == by
                    && jloc.getBlockZ() == bz) {
                return true;
            }
        }
        return false;
    }

    // ====== HELPERS ======

    private void cancelTask(JukeboxData data) {
        if (data.currentTask != null) {
            data.currentTask.cancel();
            data.currentTask = null;
        }
    }

    private List<Material> getDiscsFromChest(Location chestLoc) {
        if (chestLoc == null) return List.of();
        Block block = chestLoc.getBlock();
        if (!(block.getState() instanceof Container container)) return List.of();
        List<Material> discs = new ArrayList<>();
        for (ItemStack item : container.getInventory().getContents()) {
            if (item != null && isMusicDisc(item.getType())) {
                discs.add(item.getType());
            }
        }
        return discs;
    }

    private static boolean isMusicDisc(Material m) {
        return m != null && m.name().startsWith("MUSIC_DISC_");
    }

    private static String discName(Material m) {
        return m.name().toLowerCase().replace("music_disc_", "");
    }

    private static boolean isLiveStream(String urlStr) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(3_000);
            conn.setReadTimeout(3_000);
            conn.setRequestProperty("User-Agent", "hmyLobby/1");
            conn.setRequestProperty("Icy-MetaData", "1");
            conn.connect();
            boolean live = conn.getHeaderField("icy-metaint") != null
                    || (conn.getContentLengthLong() == -1
                    && conn.getContentType() != null
                    && conn.getContentType().startsWith("audio/"));
            conn.disconnect();
            return live;
        } catch (IOException e) {
            return false;
        }
    }

    private void playViaOpenAudioMc(Player player, String url) {
        try {
            Class<?> apiClass = Class.forName("com.craftmend.openaudiomc.api.MediaApi");
            Object instance = apiClass.getMethod("getInstance").invoke(null);
            for (Method m : apiClass.getMethods()) {
                if (m.getParameterCount() == 2
                        && (m.getName().equals("playMedia") || m.getName().equals("playAudio"))
                        && m.getParameterTypes()[0].isAssignableFrom(player.getClass())) {
                    m.invoke(instance, player, url);
                    return;
                }
            }
        } catch (ClassNotFoundException ignored) {
            // OpenAudioMc not installed — silently skip
        } catch (Exception e) {
            logger.warning("Haaremy: OpenAudioMc Fehler: " + e.getMessage());
        }
    }

    public Map<String, JukeboxData> getJukeboxes() {
        return Collections.unmodifiableMap(jukeboxes);
    }
}
