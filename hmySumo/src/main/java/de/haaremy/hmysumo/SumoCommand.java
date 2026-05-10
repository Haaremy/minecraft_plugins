package de.haaremy.hmysumo;

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

public class SumoCommand implements CommandExecutor, TabCompleter {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final String PREFIX = "<gold><bold>SUMO</bold></gold> ";

    private final HmySumo plugin;

    public SumoCommand(HmySumo plugin) {
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
        if (plugin.getSumoArena().getFreeArena() == null && !plugin.getSumoManager().isInQueue(player.getUniqueId())) {
            // Keine Arenen vorhanden oder alle belegt — trotzdem in Queue erlauben
            if (plugin.getSumoArena().getArenas().isEmpty()) {
                player.sendMessage(MINI.deserialize(PREFIX + "<red>Es sind keine Arenen konfiguriert! Ein Admin muss zuerst eine Arena einrichten."));
                return;
            }
        }
        plugin.getSumoManager().joinQueue(player);
    }

    private void handleLeave(Player player) {
        plugin.getSumoManager().leaveQueue(player);
    }

    private void handleStats(Player player, String[] args) {
        UUID targetUuid = player.getUniqueId();
        String targetName = player.getName();

        // Optional: Stats eines anderen Spielers anzeigen
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

        SumoElo elo = plugin.getSumoElo();
        int eloValue = elo.getElo(targetUuid);
        int wins = elo.getWins(targetUuid);
        int losses = elo.getLosses(targetUuid);
        int totalGames = wins + losses;
        String winrate = totalGames > 0 ? String.format("%.1f", (wins * 100.0 / totalGames)) : "0.0";

        player.sendMessage(MINI.deserialize(
                "\n<gold><bold>SUMO Statistiken</bold></gold> <gray>- <yellow>" + targetName + "\n" +
                "<gray>ELO: <yellow>" + eloValue + "\n" +
                "<gray>Siege: <green>" + wins + "\n" +
                "<gray>Niederlagen: <red>" + losses + "\n" +
                "<gray>Spiele: <white>" + totalGames + "\n" +
                "<gray>Winrate: <yellow>" + winrate + "%\n"
        ));
    }

    private void handleSetup(Player player, String[] args) {
        if (!player.hasPermission("hmy.sumo.admin")) {
            player.sendMessage(MINI.deserialize(PREFIX + "<red>Keine Berechtigung!"));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(MINI.deserialize(PREFIX + "<gray>Nutzung: <yellow>/sumo setup <arenaName> <spawn1|spawn2>"));
            player.sendMessage(MINI.deserialize(PREFIX + "<gray>Stell dich an die gewuenschte Position und setze den Spawnpunkt."));
            return;
        }

        if (args.length < 3) {
            player.sendMessage(MINI.deserialize(PREFIX + "<gray>Nutzung: <yellow>/sumo setup " + args[1] + " <spawn1|spawn2>"));
            return;
        }

        String arenaName = args[1];
        String spawnArg = args[2].toLowerCase();

        if (!spawnArg.equals("spawn1") && !spawnArg.equals("spawn2")) {
            player.sendMessage(MINI.deserialize(PREFIX + "<red>Ungueltig! Nutze <yellow>spawn1 <red>oder <yellow>spawn2<red>."));
            return;
        }

        // Aktuelle Position holen
        org.bukkit.Location loc = player.getLocation();

        // Existierende Arena laden oder neue erstellen
        SumoArena.ArenaData existing = plugin.getSumoArena().getArena(arenaName);

        if (spawnArg.equals("spawn1")) {
            org.bukkit.Location spawn2 = existing != null ? existing.getSpawn2() : loc;
            plugin.getSumoArena().saveArena(arenaName, loc, spawn2);
            player.sendMessage(MINI.deserialize(PREFIX + "<green>Spawn 1 fuer Arena <yellow>" + arenaName + " <green>gesetzt!"));
            player.sendMessage(MINI.deserialize(PREFIX + "<gray>Position: <white>" +
                    String.format("%.1f, %.1f, %.1f", loc.getX(), loc.getY(), loc.getZ())));
        } else {
            org.bukkit.Location spawn1 = existing != null ? existing.getSpawn1() : loc;
            plugin.getSumoArena().saveArena(arenaName, spawn1, loc);
            player.sendMessage(MINI.deserialize(PREFIX + "<green>Spawn 2 fuer Arena <yellow>" + arenaName + " <green>gesetzt!"));
            player.sendMessage(MINI.deserialize(PREFIX + "<gray>Position: <white>" +
                    String.format("%.1f, %.1f, %.1f", loc.getX(), loc.getY(), loc.getZ())));
        }

        // Pruefen ob Arena komplett
        SumoArena.ArenaData arena = plugin.getSumoArena().getArena(arenaName);
        if (arena != null) {
            player.sendMessage(MINI.deserialize(PREFIX + "<green>Arena <yellow>" + arenaName + " <green>ist bereit!"));
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(MINI.deserialize(
                "\n<gold><bold>SUMO</bold></gold> <gray>- 1v1 Knockback-Kampf\n" +
                "<yellow>/sumo join <gray>- Warteschlange beitreten\n" +
                "<yellow>/sumo leave <gray>- Verlassen\n" +
                "<yellow>/sumo stats [Spieler] <gray>- Statistiken anzeigen\n" +
                (player.hasPermission("hmy.sumo.admin") ?
                        "<yellow>/sumo setup <arenaName> <spawn1|spawn2> <gray>- Arena einrichten\n" : "")
        ));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("join", "leave", "stats"));
            if (sender.hasPermission("hmy.sumo.admin")) {
                completions.add("setup");
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("setup") && sender.hasPermission("hmy.sumo.admin")) {
                // Bestehende Arena-Namen vorschlagen
                for (SumoArena.ArenaData arena : plugin.getSumoArena().getArenas()) {
                    completions.add(arena.getName());
                }
            } else if (args[0].equalsIgnoreCase("stats")) {
                // Online-Spieler vorschlagen
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    completions.add(player.getName());
                }
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("setup") && sender.hasPermission("hmy.sumo.admin")) {
                completions.addAll(Arrays.asList("spawn1", "spawn2"));
            }
        }

        // Filter basierend auf aktuellem Input
        String input = args[args.length - 1].toLowerCase();
        completions.removeIf(c -> !c.toLowerCase().startsWith(input));

        return completions;
    }
}
