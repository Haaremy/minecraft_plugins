package de.haaremy.hmylobby.jukebox;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public class ComJukebox implements CommandExecutor {

    private final JukeboxManager manager;

    public ComJukebox(JukeboxManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cDieser Befehl kann nur von einem Spieler ausgeführt werden.");
            return true;
        }

        if (!player.hasPermission("hmy.lobby.jukebox.admin")) {
            player.sendMessage("§cKeine Berechtigung.");
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
            player.sendMessage("§cUsage: §e/jukebox create <id>");
            return;
        }
        manager.beginCreate(player, args[1]);
    }

    // ====== sync ======

    private void handleSync(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: §e/jukebox sync <id1,id2,...>");
            return;
        }
        List<String> ids = Arrays.asList(args[1].split(","));
        if (ids.isEmpty()) {
            player.sendMessage("§cMindestens eine Jukebox-ID angeben.");
            return;
        }
        manager.syncJukeboxes(ids, player);
    }

    // ====== list ======

    private void handleList(Player player) {
        var jukeboxes = manager.getJukeboxes();
        if (jukeboxes.isEmpty()) {
            player.sendMessage("§7Keine Jukeboxen registriert.");
            return;
        }
        player.sendMessage("§6=== Jukeboxen (" + jukeboxes.size() + ") ===");
        for (var entry : jukeboxes.entrySet()) {
            JukeboxData data = entry.getValue();
            String loc = data.jukeboxLoc.getWorld().getName()
                    + " " + data.jukeboxLoc.getBlockX()
                    + "/" + data.jukeboxLoc.getBlockY()
                    + "/" + data.jukeboxLoc.getBlockZ();
            String modeStr = switch (data.mode) {
                case ENDLESS -> "§aENDLESS §7(" + data.currentDisc.name().toLowerCase().replace("music_disc_", "") + ")";
                case DISKBOX -> "§bDISKBOX §7(disc " + data.diskboxIndex + ")";
                case STREAM  -> "§dSTREAM §7(" + (data.streamLive ? "live" : data.streamEndless ? "endless" : "once") + ")";
                case STOPPED -> "§8STOPPED";
            };
            player.sendMessage("§e" + entry.getKey() + " §7– " + modeStr + " §7@ " + loc);
        }
    }

    // ====== <id> <action> [args] ======

    private void handleJukebox(Player player, String[] args) {
        String id = args[0];

        if (!manager.getJukeboxes().containsKey(id)) {
            player.sendMessage("§cUnbekannte Jukebox: §e" + id + "§c. Nutze §e/jukebox list§c.");
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
            player.sendMessage("§cUsage: §e/jukebox <id> play endless");
            return;
        }
        manager.startEndless(id, player);
    }

    private void handleAdd(Player player, String id, String[] args) {
        if (args.length < 3 || !args[2].equalsIgnoreCase("diskbox")) {
            player.sendMessage("§cUsage: §e/jukebox <id> add diskbox");
            return;
        }
        manager.beginAddDiskbox(player, id);
    }

    private void handleSet(Player player, String id, String[] args) {
        if (args.length < 4 || !args[2].equalsIgnoreCase("stream")) {
            player.sendMessage("§cUsage: §e/jukebox <id> set stream <url>");
            return;
        }
        manager.setStream(id, args[3], player);
    }

    // ====== help ======

    private void sendHelp(Player player) {
        player.sendMessage("§6=== /jukebox ===");
        player.sendMessage("§e/jukebox create <id>               §7Registriert eine neue Jukebox");
        player.sendMessage("§e/jukebox <id> play endless         §7Loopt die aktuelle Disk endlos");
        player.sendMessage("§e/jukebox <id> stop                 §7Stoppt die Wiedergabe");
        player.sendMessage("§e/jukebox <id> add diskbox          §7Verknüpft eine Truhe (Disk-Playlist)");
        player.sendMessage("§e/jukebox <id> set stream <url>     §7Spielt einen Stream via OpenAudioMc");
        player.sendMessage("§e/jukebox sync <id1,id2,...>        §7Startet mehrere Jukeboxen gleichzeitig");
        player.sendMessage("§e/jukebox list                      §7Listet alle Jukeboxen auf");
    }
}
