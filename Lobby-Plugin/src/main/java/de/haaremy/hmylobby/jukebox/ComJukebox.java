package de.haaremy.hmylobby.jukebox;

import de.haaremy.hmylobby.HmyLanguageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ComJukebox implements CommandExecutor {

    private final JukeboxManager manager;
    private final HmyLanguageManager language;

    public ComJukebox(JukeboxManager manager, HmyLanguageManager language) {
        this.manager = manager;
        this.language = language;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(language.getMessage("jukebox.com.player_only",
                    "§cDieser Befehl kann nur von einem Spieler ausgefuehrt werden."));
            return true;
        }

        if (!player.hasPermission("hmy.lobby.jukebox.admin")) {
            player.sendMessage(language.getMessage(player, "jukebox.com.no_permission",
                    "§cKeine Berechtigung."));
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> handleCreate(player, args);
            case "sync"   -> handleSync(player, args);
            case "list"   -> handleList(player);
            default       -> handleJukebox(player, args);
        }
        return true;
    }

    // ====== create ======

    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(language.getMessage(player, "jukebox.com.usage.create",
                    "§cUsage: §e/jukebox create <id>"));
            return;
        }
        manager.beginCreate(player, args[1]);
    }

    // ====== sync ======

    private void handleSync(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(language.getMessage(player, "jukebox.com.usage.sync",
                    "§cUsage: §e/jukebox sync <id1,id2,...>"));
            return;
        }
        List<String> ids = Arrays.asList(args[1].split(","));
        if (ids.isEmpty()) {
            player.sendMessage(language.getMessage(player, "jukebox.com.usage.sync_min",
                    "§cMindestens eine Jukebox-ID angeben."));
            return;
        }
        manager.syncJukeboxes(ids, player);
    }

    // ====== list ======

    private void handleList(Player player) {
        var jukeboxes = manager.getJukeboxes();
        if (jukeboxes.isEmpty()) {
            player.sendMessage(language.getMessage(player, "jukebox.com.empty",
                    "§7Keine Jukeboxen registriert."));
            return;
        }
        Map<String, String> headerPh = new HashMap<>();
        headerPh.put("count", String.valueOf(jukeboxes.size()));
        player.sendMessage(language.getMessage(player, "jukebox.com.list.header",
                "§6=== Jukeboxen ({count}) ===", headerPh));
        for (var entry : jukeboxes.entrySet()) {
            JukeboxData data = entry.getValue();
            String loc = data.jukeboxLoc.getWorld().getName()
                    + " " + data.jukeboxLoc.getBlockX()
                    + "/" + data.jukeboxLoc.getBlockY()
                    + "/" + data.jukeboxLoc.getBlockZ();
            String modeStr = renderMode(player, data);
            Map<String, String> entryPh = new HashMap<>();
            entryPh.put("id", entry.getKey());
            entryPh.put("mode", modeStr);
            entryPh.put("loc", loc);
            player.sendMessage(language.getMessage(player, "jukebox.com.list.entry",
                    "§e{id} §7– {mode} §7@ {loc}", entryPh));
        }
    }

    private String renderMode(Player player, JukeboxData data) {
        return switch (data.mode) {
            case ENDLESS -> {
                Map<String, String> ph = new HashMap<>();
                ph.put("disc", data.currentDisc.name().toLowerCase().replace("music_disc_", ""));
                yield language.getMessage(player, "jukebox.com.list.mode.endless",
                        "§aENDLESS §7({disc})", ph);
            }
            case DISKBOX -> {
                Map<String, String> ph = new HashMap<>();
                ph.put("index", String.valueOf(data.diskboxIndex));
                yield language.getMessage(player, "jukebox.com.list.mode.diskbox",
                        "§bDISKBOX §7(disc {index})", ph);
            }
            case STREAM -> {
                String streamKey = data.streamLive ? "jukebox.com.list.mode.stream.live"
                        : data.streamEndless ? "jukebox.com.list.mode.stream.endless"
                        : "jukebox.com.list.mode.stream.once";
                String streamDef = data.streamLive ? "§dSTREAM §7(live)"
                        : data.streamEndless ? "§dSTREAM §7(endless)"
                        : "§dSTREAM §7(once)";
                yield language.getMessage(player, streamKey, streamDef);
            }
            case STOPPED -> language.getMessage(player, "jukebox.com.list.mode.stopped",
                    "§8STOPPED");
        };
    }

    // ====== <id> <action> [args] ======

    private void handleJukebox(Player player, String[] args) {
        String id = args[0];

        if (!manager.getJukeboxes().containsKey(id)) {
            Map<String, String> ph = new HashMap<>();
            ph.put("id", id);
            player.sendMessage(language.getMessage(player, "jukebox.com.unknown",
                    "§cUnbekannte Jukebox: §e{id}§c. Nutze §e/jukebox list§c.", ph));
            return;
        }

        if (args.length < 2) {
            sendHelp(player);
            return;
        }

        switch (args[1].toLowerCase()) {
            case "play"   -> handlePlay(player, id, args);
            case "stop"   -> manager.stopPlayback(id, player);
            case "add"    -> handleAdd(player, id, args);
            case "set"    -> handleSet(player, id, args);
            default       -> sendHelp(player);
        }
    }

    private void handlePlay(Player player, String id, String[] args) {
        if (args.length < 3 || !args[2].equalsIgnoreCase("endless")) {
            player.sendMessage(language.getMessage(player, "jukebox.com.usage.play",
                    "§cUsage: §e/jukebox <id> play endless"));
            return;
        }
        manager.startEndless(id, player);
    }

    private void handleAdd(Player player, String id, String[] args) {
        if (args.length < 3 || !args[2].equalsIgnoreCase("diskbox")) {
            player.sendMessage(language.getMessage(player, "jukebox.com.usage.add",
                    "§cUsage: §e/jukebox <id> add diskbox"));
            return;
        }
        manager.beginAddDiskbox(player, id);
    }

    private void handleSet(Player player, String id, String[] args) {
        if (args.length < 4 || !args[2].equalsIgnoreCase("stream")) {
            player.sendMessage(language.getMessage(player, "jukebox.com.usage.set",
                    "§cUsage: §e/jukebox <id> set stream <url>"));
            return;
        }
        manager.setStream(id, args[3], player);
    }

    // ====== help ======

    private void sendHelp(Player player) {
        player.sendMessage(language.getMessage(player, "jukebox.com.help.header",
                "§6=== /jukebox ==="));
        player.sendMessage(language.getMessage(player, "jukebox.com.help.create",
                "§e/jukebox create <id>               §7Registriert eine neue Jukebox"));
        player.sendMessage(language.getMessage(player, "jukebox.com.help.play",
                "§e/jukebox <id> play endless         §7Loopt die aktuelle Disk endlos"));
        player.sendMessage(language.getMessage(player, "jukebox.com.help.stop",
                "§e/jukebox <id> stop                 §7Stoppt die Wiedergabe"));
        player.sendMessage(language.getMessage(player, "jukebox.com.help.add",
                "§e/jukebox <id> add diskbox          §7Verknuepft eine Truhe (Disk-Playlist)"));
        player.sendMessage(language.getMessage(player, "jukebox.com.help.set",
                "§e/jukebox <id> set stream <url>     §7Spielt einen Stream via OpenAudioMc"));
        player.sendMessage(language.getMessage(player, "jukebox.com.help.sync",
                "§e/jukebox sync <id1,id2,...>        §7Startet mehrere Jukeboxen gleichzeitig"));
        player.sendMessage(language.getMessage(player, "jukebox.com.help.list",
                "§e/jukebox list                      §7Listet alle Jukeboxen auf"));
    }
}
