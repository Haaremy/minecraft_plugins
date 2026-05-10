package de.haaremy.hmytntrun;

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

public class TNTRunCommand implements CommandExecutor, TabCompleter {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final String PREFIX = "<red><bold>TNTRUN</bold></red> ";

    private final HmyTNTRun plugin;

    public TNTRunCommand(HmyTNTRun plugin) {
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
        if (plugin.getTNTRunArena().getArenas().isEmpty()) {
            player.sendMessage(MINI.deserialize(PREFIX + "<red>Es sind keine Arenen konfiguriert! Ein Admin muss zuerst eine Arena einrichten."));
            return;
        }
        plugin.getTNTRunManager().joinQueue(player);
    }

    private void handleLeave(Player player) {
        plugin.getTNTRunManager().leaveQueue(player);
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
        PlayerStats stats = statsManager.getStats(targetUuid, "tntrun");

        int wins = stats.getWins();
        int losses = stats.getLosses();
        int totalGames = wins + losses;
        String winrate = totalGames > 0 ? String.format("%.1f", (wins * 100.0 / totalGames)) : "0.0";

        player.sendMessage(MINI.deserialize(
                "\n<red><bold>TNTRUN Statistiken</bold></red> <gray>- <yellow>" + targetName + "\n" +
                "<gray>Siege: <green>" + wins + "\n" +
                "<gray>Niederlagen: <red>" + losses + "\n" +
                "<gray>Spiele: <white>" + totalGames + "\n" +
                "<gray>Winrate: <yellow>" + winrate + "%\n"
        ));
    }

    private void handleSetup(Player player, String[] args) {
        if (!player.hasPermission("hmy.tntrun.admin")) {
            player.sendMessage(MINI.deserialize(PREFIX + "<red>Keine Berechtigung!"));
            return;
        }

        if (args.length < 3) {
            player.sendMessage(MINI.deserialize(PREFIX + "<gray>Nutzung:"));
            player.sendMessage(MINI.deserialize(PREFIX + "<yellow>/tntrun setup <name> spawn <gray>- Spawn setzen"));
            player.sendMessage(MINI.deserialize(PREFIX + "<yellow>/tntrun setup <name> lobby <gray>- Lobby setzen"));
            player.sendMessage(MINI.deserialize(PREFIX + "<yellow>/tntrun setup <name> layers <y1> <y2> <y3> <voidY> <gray>- Ebenen setzen"));
            return;
        }

        String arenaName = args[1];
        String action = args[2].toLowerCase();

        switch (action) {
            case "spawn" -> {
                plugin.getTNTRunArena().saveArenaSpawn(arenaName, player.getLocation());
                player.sendMessage(MINI.deserialize(PREFIX + "<green>Spawn fuer Arena <yellow>" + arenaName + " <green>gesetzt!"));
                player.sendMessage(MINI.deserialize(PREFIX + "<gray>Position: <white>" +
                        String.format("%.1f, %.1f, %.1f", player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ())));
            }
            case "lobby" -> {
                plugin.getTNTRunArena().saveArenaLobby(arenaName, player.getLocation());
                player.sendMessage(MINI.deserialize(PREFIX + "<green>Lobby fuer Arena <yellow>" + arenaName + " <green>gesetzt!"));
            }
            case "layers" -> {
                if (args.length < 7) {
                    player.sendMessage(MINI.deserialize(PREFIX + "<red>Nutzung: /tntrun setup " + arenaName + " layers <y1> <y2> <y3> <voidY>"));
                    return;
                }
                try {
                    List<Integer> layers = List.of(
                            Integer.parseInt(args[3]),
                            Integer.parseInt(args[4]),
                            Integer.parseInt(args[5])
                    );
                    int voidY = Integer.parseInt(args[6]);
                    plugin.getTNTRunArena().saveArenaLayers(arenaName, layers, voidY);
                    player.sendMessage(MINI.deserialize(PREFIX + "<green>Ebenen fuer Arena <yellow>" + arenaName + " <green>gesetzt: <white>" + layers + " <gray>(Void: " + voidY + ")"));
                } catch (NumberFormatException e) {
                    player.sendMessage(MINI.deserialize(PREFIX + "<red>Ungueltige Zahlen!"));
                }
            }
            default -> player.sendMessage(MINI.deserialize(PREFIX + "<red>Unbekannte Aktion: " + action));
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(MINI.deserialize(
                "\n<red><bold>TNTRUN</bold></red> <gray>- Boden verschwindet unter deinen Fuessen\n" +
                "<yellow>/tntrun join <gray>- Warteschlange beitreten\n" +
                "<yellow>/tntrun leave <gray>- Verlassen\n" +
                "<yellow>/tntrun stats [Spieler] <gray>- Statistiken anzeigen\n" +
                (player.hasPermission("hmy.tntrun.admin") ?
                        "<yellow>/tntrun setup <name> <spawn|lobby|layers> <gray>- Arena einrichten\n" : "")
        ));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("join", "leave", "stats"));
            if (sender.hasPermission("hmy.tntrun.admin")) {
                completions.add("setup");
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("setup") && sender.hasPermission("hmy.tntrun.admin")) {
                for (TNTRunArena.ArenaData arena : plugin.getTNTRunArena().getArenas()) {
                    completions.add(arena.getName());
                }
            } else if (args[0].equalsIgnoreCase("stats")) {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    completions.add(player.getName());
                }
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("setup") && sender.hasPermission("hmy.tntrun.admin")) {
                completions.addAll(Arrays.asList("spawn", "lobby", "layers"));
            }
        }

        String input = args[args.length - 1].toLowerCase();
        completions.removeIf(c -> !c.toLowerCase().startsWith(input));
        return completions;
    }
}
