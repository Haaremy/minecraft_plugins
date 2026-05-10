package de.haaremy.hmyspleef;

import de.haaremy.hmycore.HmyCore;
import de.haaremy.hmycore.stats.PlayerStats;
import de.haaremy.hmycore.stats.StatsManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class SpleefCommand implements CommandExecutor, TabCompleter {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final String PREFIX = "<aqua><bold>SPLEEF</bold></aqua> ";

    private final HmySpleef plugin;

    public SpleefCommand(HmySpleef plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MINI.deserialize(PREFIX + "<red>Nur Spieler koennen diesen Befehl nutzen!"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "join" -> handleJoin(player);
            case "leave" -> handleLeave(player);
            case "stats" -> handleStats(player, args);
            case "setup" -> handleSetup(player, args);
            default -> sendHelp(player);
        }

        return true;
    }

    private void handleJoin(Player player) {
        if (plugin.getSpleefArena().getArenas().isEmpty()) {
            player.sendMessage(MINI.deserialize(PREFIX + "<red>Es sind keine Arenen konfiguriert! Ein Admin muss zuerst eine Arena einrichten."));
            return;
        }
        plugin.getSpleefManager().joinQueue(player);
    }

    private void handleLeave(Player player) {
        plugin.getSpleefManager().leaveQueue(player);
    }

    private void handleStats(Player player, String[] args) {
        UUID targetUuid = player.getUniqueId();
        String targetName = player.getName();

        if (args.length >= 2) {
            Player target = plugin.getServer().getPlayer(args[1]);
            if (target != null) {
                targetUuid = target.getUniqueId();
                targetName = target.getName();
            } else {
                player.sendMessage(MINI.deserialize(PREFIX + "<red>Spieler nicht gefunden!"));
                return;
            }
        }

        StatsManager statsManager = HmyCore.getInstance().getStatsManager();
        PlayerStats stats = statsManager.getStats(targetUuid, "spleef");

        int wins = stats.getWins();
        int losses = stats.getLosses();
        int totalGames = wins + losses;
        String winrate = totalGames > 0 ? String.format("%.1f", (wins * 100.0 / totalGames)) : "0.0";

        player.sendMessage(MINI.deserialize(
                "\n<aqua><bold>SPLEEF Statistiken</bold></aqua> <gray>- <yellow>" + targetName + "\n" +
                "<gray>Siege: <green>" + wins + "\n" +
                "<gray>Niederlagen: <red>" + losses + "\n" +
                "<gray>Spiele: <white>" + totalGames + "\n" +
                "<gray>Winrate: <yellow>" + winrate + "%\n"
        ));
    }

    private void handleSetup(Player player, String[] args) {
        if (!player.hasPermission("hmy.spleef.admin")) {
            player.sendMessage(MINI.deserialize(PREFIX + "<red>Keine Berechtigung!"));
            return;
        }

        if (args.length < 3) {
            player.sendMessage(MINI.deserialize(PREFIX + "<gray>Nutzung:"));
            player.sendMessage(MINI.deserialize(PREFIX + "<yellow>/spleef setup <name> addspawn <gray>- Spawn-Punkt hinzufuegen"));
            player.sendMessage(MINI.deserialize(PREFIX + "<yellow>/spleef setup <name> floor <floorY> <voidY> <gray>- Boden-Ebene setzen"));
            return;
        }

        String arenaName = args[1];
        String action = args[2].toLowerCase();

        switch (action) {
            case "addspawn" -> {
                plugin.getSpleefArena().addSpawnPoint(arenaName, player.getLocation());
                SpleefArena.ArenaData arena = plugin.getSpleefArena().getArena(arenaName);
                int count = arena != null ? arena.getSpawnPoints().size() : 1;
                player.sendMessage(MINI.deserialize(PREFIX + "<green>Spawn-Punkt " + count + " fuer Arena <yellow>" + arenaName + " <green>hinzugefuegt!"));
                player.sendMessage(MINI.deserialize(PREFIX + "<gray>Position: <white>" +
                        String.format("%.1f, %.1f, %.1f", player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ())));
            }
            case "floor" -> {
                if (args.length < 5) {
                    player.sendMessage(MINI.deserialize(PREFIX + "<red>Nutzung: /spleef setup " + arenaName + " floor <floorY> <voidY>"));
                    return;
                }
                try {
                    int floorY = Integer.parseInt(args[3]);
                    int voidY = Integer.parseInt(args[4]);
                    plugin.getSpleefArena().setFloorY(arenaName, floorY, voidY);
                    player.sendMessage(MINI.deserialize(PREFIX + "<green>Boden fuer Arena <yellow>" + arenaName + " <green>gesetzt: Y=" + floorY + " <gray>(Void: " + voidY + ")"));
                } catch (NumberFormatException e) {
                    player.sendMessage(MINI.deserialize(PREFIX + "<red>Ungueltige Zahlen!"));
                }
            }
            default -> player.sendMessage(MINI.deserialize(PREFIX + "<red>Unbekannte Aktion: " + action));
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(MINI.deserialize(
                "\n<aqua><bold>SPLEEF</bold></aqua> <gray>- Zerstoere den Boden unter deinen Gegnern\n" +
                "<yellow>/spleef join <gray>- Warteschlange beitreten\n" +
                "<yellow>/spleef leave <gray>- Verlassen\n" +
                "<yellow>/spleef stats [Spieler] <gray>- Statistiken anzeigen\n" +
                (player.hasPermission("hmy.spleef.admin") ?
                        "<yellow>/spleef setup <name> <addspawn|floor> <gray>- Arena einrichten\n" : "")
        ));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("join", "leave", "stats"));
            if (sender.hasPermission("hmy.spleef.admin")) {
                completions.add("setup");
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("setup") && sender.hasPermission("hmy.spleef.admin")) {
                for (SpleefArena.ArenaData arena : plugin.getSpleefArena().getArenas()) {
                    completions.add(arena.getName());
                }
            } else if (args[0].equalsIgnoreCase("stats")) {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    completions.add(player.getName());
                }
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("setup") && sender.hasPermission("hmy.spleef.admin")) {
                completions.addAll(Arrays.asList("addspawn", "floor"));
            }
        }

        String input = args[args.length - 1].toLowerCase();
        completions.removeIf(c -> !c.toLowerCase().startsWith(input));
        return completions;
    }
}
