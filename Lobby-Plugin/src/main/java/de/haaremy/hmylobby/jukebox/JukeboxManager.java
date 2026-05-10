package de.haaremy.hmylobby.jukebox;

import de.haaremy.hmylobby.HmyLanguageManager;
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
    private final HmyLanguageManager language;
    private final Map<String, JukeboxData> jukeboxes = new LinkedHashMap<>();
    private final JukeboxConfig config;
    private final Map<UUID, PendingSelection> pendingSelections = new HashMap<>();

    public JukeboxManager(Plugin plugin, HmyLanguageManager language, Path hmySettingsDir) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.language = language;
        this.config = new JukeboxConfig(hmySettingsDir, logger);
        config.load(jukeboxes);
    }

    // ====== SELECTION FLOW ======

    public void beginCreate(Player player, String id) {
        if (jukeboxes.containsKey(id)) {
            player.sendMessage(language.getMessage(player, "jukebox.mgr.create.exists",
                    "§cEine Jukebox mit ID §e{id}§c existiert bereits.", Map.of("id", id)));
            return;
        }
        pendingSelections.put(player.getUniqueId(), new PendingSelection(SelectionAction.CREATE, id));
        player.sendMessage(language.getMessage(player, "jukebox.mgr.create.prompt",
                "§6Rechtsklick die Jukebox mit dem §egoldenen Schwert§6."));
    }

    public void beginAddDiskbox(Player player, String id) {
        JukeboxData data = jukeboxes.get(id);
        if (data == null) {
            player.sendMessage(language.getMessage(player, "jukebox.mgr.unknown",
                    "§cUnbekannte Jukebox: §e{id}", Map.of("id", id)));
            return;
        }
        pendingSelections.put(player.getUniqueId(), new PendingSelection(SelectionAction.ADD_DISKBOX, id));
        player.sendMessage(language.getMessage(player, "jukebox.mgr.add.prompt",
                "§6Rechtsklick die Truhe mit dem §egoldenen Schwert§6."));
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
                player.sendMessage(language.getMessage(player, "jukebox.mgr.create.not_jukebox",
                        "§cDas ist keine Jukebox. Aktion abgebrochen."));
                return;
            }
            JukeboxData data = new JukeboxData(pending.jukeboxId(), block.getLocation());
            jukeboxes.put(pending.jukeboxId(), data);
            config.save(jukeboxes);
            player.sendMessage(language.getMessage(player, "jukebox.mgr.create.ok",
                    "§6✓ Jukebox §e{id}§6 registriert! Lege eine Disk ein und nutze §e/jukebox {id} play endless§6.",
                    Map.of("id", pending.jukeboxId())));

        } else if (pending.action() == SelectionAction.ADD_DISKBOX) {
            if (!(block.getState() instanceof Container)) {
                player.sendMessage(language.getMessage(player, "jukebox.mgr.add.not_container",
                        "§cDas ist kein Container. Aktion abgebrochen."));
                return;
            }
            JukeboxData data = jukeboxes.get(pending.jukeboxId());
            if (data == null) return;
            data.chestLoc = block.getLocation();
            config.save(jukeboxes);
            doStartDiskbox(data);
            player.sendMessage(language.getMessage(player, "jukebox.mgr.add.ok",
                    "§6✓ Diskbox §e{id}§6 verknuepft und gestartet!",
                    Map.of("id", pending.jukeboxId())));
        }
    }

    // ====== PLAYBACK ======

    /** Start endless disc loop. If mode is already STREAM, enables stream repeat instead. */
    public boolean startEndless(String id, Player feedback) {
        JukeboxData data = jukeboxes.get(id);
        if (data == null) {
            if (feedback != null) feedback.sendMessage(language.getMessage(feedback, "jukebox.mgr.unknown",
                    "§cUnbekannte Jukebox: §e{id}", Map.of("id", id)));
            return false;
        }

        // Special case: endless on an active stream
        if (data.mode == JukeboxMode.STREAM && data.streamUrl != null) {
            data.streamEndless = true;
            config.save(jukeboxes);
            scheduleStreamRepeat(data);
            if (feedback != null) feedback.sendMessage(language.getMessage(feedback, "jukebox.mgr.endless.stream_loop",
                    "§6✓ Stream §e{id}§6 wird endlos wiederholt.", Map.of("id", id)));
            return true;
        }

        Block block = data.jukeboxLoc.getBlock();
        if (!(block.getState() instanceof Jukebox jukebox)) {
            if (feedback != null) feedback.sendMessage(language.getMessage(feedback, "jukebox.mgr.endless.no_block",
                    "§cAn der Jukebox-Position befindet sich keine Jukebox."));
            return false;
        }

        // Use cached disc or read from block
        Material disc = isMusicDisc(data.currentDisc) ? data.currentDisc : jukebox.getPlaying();
        if (!isMusicDisc(disc)) {
            if (feedback != null) feedback.sendMessage(language.getMessage(feedback, "jukebox.mgr.endless.no_disc",
                    "§cKeine Disk in der Jukebox §e{id}§c.", Map.of("id", id)));
            return false;
        }

        cancelTask(data);
        data.currentDisc = disc;
        data.mode = JukeboxMode.ENDLESS;
        jukebox.setPlaying(disc);
        jukebox.update();
        jukebox.startPlaying();
        scheduleEndlessNext(data);
        if (feedback != null) feedback.sendMessage(language.getMessage(feedback, "jukebox.mgr.endless.ok",
                "§6✓ Jukebox §e{id}§6 spielt §e{disc}§6 endlos.",
                Map.of("id", id, "disc", discName(disc))));
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
            jukebox.update();
            jukebox.startPlaying();
            scheduleEndlessNext(data);
        }, duration - 10L);
    }

    /** Start diskbox using an already-linked chest. */
    public boolean startDiskbox(String id, Player feedback) {
        JukeboxData data = jukeboxes.get(id);
        if (data == null) {
            if (feedback != null) feedback.sendMessage(language.getMessage(feedback, "jukebox.mgr.unknown",
                    "§cUnbekannte Jukebox: §e" + id, Map.of("id", id)));
            return false;
        }
        if (data.chestLoc == null) {
            if (feedback != null) feedback.sendMessage(language.getMessage(feedback, "jukebox.mgr.diskbox.no_link",
                    "§cKeine Diskbox für §e" + id + "§c verknüpft. Nutze §e/jukebox " + id + " add diskbox§c.",
                    Map.of("id", id)));
            return false;
        }
        doStartDiskbox(data);
        if (feedback != null) feedback.sendMessage(language.getMessage(feedback, "jukebox.mgr.diskbox.ok",
                "§6✓ Diskbox §e" + id + "§6 gestartet.", Map.of("id", id)));
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
        jukebox.update();
        jukebox.startPlaying();

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
            if (feedback != null) feedback.sendMessage(language.getMessage(feedback, "jukebox.mgr.unknown",
                    "§cUnbekannte Jukebox: §e" + id, Map.of("id", id)));
            return false;
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            if (feedback != null) feedback.sendMessage(language.getMessage(feedback, "jukebox.mgr.stream.bad_url",
                    "§cNur §ehttp://§c und §ehttps://§c URLs erlaubt."));
            return false;
        }
        cancelTask(data);
        data.streamUrl = url;
        data.streamEndless = false;
        data.mode = JukeboxMode.STREAM;
        config.save(jukeboxes);
        if (feedback != null) feedback.sendMessage(language.getMessage(feedback, "jukebox.mgr.stream.checking",
                "§7⏳ Stream wird geprüft..."));

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean live = isLiveStream(url);
            Bukkit.getScheduler().runTask(plugin, () -> {
                data.streamLive = live;
                playStreamToAll(data);
                if (feedback != null) {
                    String key = live ? "jukebox.mgr.stream.ok.live" : "jukebox.mgr.stream.ok.normal";
                    String fallback = "§6✓ Jukebox §e" + id + "§6 spielt Stream"
                            + (live ? " §7(Live)" : "") + "§6. Nutze §e/jukebox " + id + " play endless§6 zum Wiederholen.";
                    feedback.sendMessage(language.getMessage(feedback, key, fallback, Map.of("id", id)));
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
            if (feedback != null) feedback.sendMessage(language.getMessage(feedback, "jukebox.mgr.unknown",
                    "§cUnbekannte Jukebox: §e" + id, Map.of("id", id)));
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
        if (feedback != null) feedback.sendMessage(language.getMessage(feedback, "jukebox.mgr.stop.ok",
                "§6✓ Jukebox §e" + id + "§6 gestoppt.", Map.of("id", id)));
        return true;
    }

    /** Stop all named jukeboxes, then restart them all in the same server tick. */
    public boolean syncJukeboxes(List<String> ids, Player feedback) {
        List<JukeboxData> toSync = new ArrayList<>();
        for (String id : ids) {
            JukeboxData data = jukeboxes.get(id);
            if (data == null) {
                if (feedback != null) feedback.sendMessage(language.getMessage(feedback, "jukebox.mgr.unknown",
                        "§cUnbekannte Jukebox: §e" + id, Map.of("id", id)));
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
                                jukebox.update();
                                jukebox.startPlaying();
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

        if (feedback != null) feedback.sendMessage(language.getMessage(feedback, "jukebox.mgr.sync.ok",
                "§6✓ §e" + toSync.size() + "§6 Jukeboxen synchronisiert.",
                Map.of("count", String.valueOf(toSync.size()))));
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
        // Bail early if OpenAudioMc is not installed at all
        if (Bukkit.getPluginManager().getPlugin("OpenAudioMc") == null) {
            player.sendMessage(language.getMessage(player, "jukebox.mgr.oam.not_installed",
                    "§cOpenAudioMc ist nicht installiert – Stream kann nicht abgespielt werden."));
            return;
        }
        try {
            // OpenAudioMc 6.x API:
            // ClientApi.getInstance().getByPlayer(player) -> Optional<Client>
            // new OAMediaLink(url, new MediaOptions()) -> Client.playMedia(link)
            Class<?> clientApiClass = Class.forName("com.craftmend.openaudiomc.api.ClientApi");
            Object clientApi = clientApiClass.getMethod("getInstance").invoke(null);

            // getByPlayer(Player) or getClient(UUID) depending on version
            Optional<?> optClient;
            try {
                optClient = (Optional<?>) clientApiClass
                        .getMethod("getByPlayer", Player.class)
                        .invoke(clientApi, player);
            } catch (NoSuchMethodException e) {
                optClient = (Optional<?>) clientApiClass
                        .getMethod("getClient", java.util.UUID.class)
                        .invoke(clientApi, player.getUniqueId());
            }

            if (optClient == null || optClient.isEmpty()) {
                player.sendMessage("§cDu bist nicht mit OpenAudioMc verbunden. Bitte verbinde dich zuerst.");
                return;
            }

            Object client = optClient.get();

            // Build OAMediaLink(url, new MediaOptions()) and call client.playMedia(link)
            Class<?> mediaOptionsClass = Class.forName("com.craftmend.openaudiomc.api.media.MediaOptions");
            Class<?> mediaLinkClass    = Class.forName("com.craftmend.openaudiomc.api.media.OAMediaLink");
            Object mediaOptions = mediaOptionsClass.getDeclaredConstructor().newInstance();
            Object mediaLink    = mediaLinkClass
                    .getDeclaredConstructor(String.class, mediaOptionsClass)
                    .newInstance(url, mediaOptions);
            client.getClass().getMethod("playMedia", mediaLinkClass).invoke(client, mediaLink);

        } catch (ClassNotFoundException ignored) {
            // OpenAudioMc API classes not found — different version installed?
            player.sendMessage("§cOpenAudioMc API nicht gefunden. Bitte überprüfe die Version.");
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException
                 | java.lang.reflect.InvocationTargetException | IllegalArgumentException e) {
            logger.warning(() -> String.format("Haaremy: OpenAudioMc Fehler für %s: %s", player.getName(), e.getMessage()));
            player.sendMessage("§cStream-Fehler: §e" + e.getMessage());
        }
    }

    public Map<String, JukeboxData> getJukeboxes() {
        return Collections.unmodifiableMap(jukeboxes);
    }
}
