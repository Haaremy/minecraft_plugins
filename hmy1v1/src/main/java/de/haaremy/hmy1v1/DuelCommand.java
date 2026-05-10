package de.haaremy.hmy1v1;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class DuelCommand implements CommandExecutor, TabCompleter {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final String PREFIX = "<dark_purple><bold>1v1</bold></dark_purple> ";

    private final Hmy1v1 plugin;

    public DuelCommand(Hmy1v1 plugin) {
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
            case "accept" -> plugin.getDuelManager().acceptChallenge(player);
            case "deny" -> plugin.getDuelManager().denyChallenge(player);
            case "join" -> handleJoin(player, args);
            case "leave" -> plugin.getDuelManager().leaveQueue(player);
            case "stats" -> handleStats(player, args);
            case "kit" -> handleKit(player);
            case "spectate" -> handleSpectate(player, args);
            case "setup" -> handleSetup(player, args);
            case "top" -> handleTop(player);
            default -> handleChallenge(player, args[0]);
        }

        return true;
    }

    private void handleJoin(Player player, String[] args) {
        // Pruefen ob Arenen vorhanden
        if (plugin.getDuelArena().getArenas().isEmpty()) {
            player.sendMessage(MINI.deserialize(PREFIX + "<red>Es sind keine Arenen konfiguriert! Ein Admin muss zuerst eine Arena einrichten."));
            return;
        }

        boolean ranked = false;
        if (args.length >= 2) {
            if (args[1].equalsIgnoreCase("ranked")) {
                ranked = true;
            } else if (!args[1].equalsIgnoreCase("unranked")) {
                player.sendMessage(MINI.deserialize(PREFIX + "<gray>Nutzung: <yellow>/duel join [ranked|unranked]"));
                return;
            }
        }

        plugin.getDuelManager().joinQueue(player, ranked);
    }

    private void handleKit(Player player) {
        DuelKitGui gui = new DuelKitGui(plugin, player.getUniqueId(), kit -> {
            plugin.getDuelManager().setSelectedKit(player.getUniqueId(), kit);
            player.sendMessage(MINI.deserialize(PREFIX + "<gray>Kit ausgewaehlt: " + kit.getDisplayName()));
        });
        gui.open(player);
    }

    private void handleSpectate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(MINI.deserialize(PREFIX + "<gray>Nutzung: <yellow>/duel spectate <Spieler>"));
            return;
        }

        DuelManager manager = plugin.getDuelManager();

        // Nicht wenn selbst in einem Spiel
        if (manager.isInGame(player.getUniqueId())) {
            player.sendMessage(MINI.deserialize(PREFIX + "<red>Du bist selbst in einem Duell!"));
            return;
        }

        // Bereits Zuschauer?
        DuelGame currentSpec = manager.getSpectatingGame(player.getUniqueId());
        if (currentSpec != null) {
            currentSpec.removeSpectator(player);
        }

        DuelGame game = manager.getGameByPlayerName(args[1]);
        if (game == null) {
            player.sendMessage(MINI.deserialize(PREFIX + "<red>" + args[1] + " ist nicht in einem Duell!"));
            return;
        }

        game.addSpectator(player);
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

        DuelElo elo = plugin.getDuelElo();
        int eloValue = elo.getElo(targetUuid);
        int wins = elo.getWins(targetUuid);
        int losses = elo.getLosses(targetUuid);
        int totalGames = wins + losses;
        String winrate = totalGames > 0 ? String.format("%.1f", (wins * 100.0 / totalGames)) : "0.0";

        player.sendMessage(MINI.deserialize(
                "\n<dark_purple><bold>1v1 Statistiken</bold></dark_purple> <gray>- <yellow>" + targetName + "\n" +
                "<gray>ELO: <yellow>" + eloValue + "\n" +
                "<gray>Siege: <green>" + wins + "\n" +
                "<gray>Niederlagen: <red>" + losses + "\n" +
                "<gray>Spiele: <white>" + totalGames + "\n" +
                "<gray>Winrate: <yellow>" + winrate + "%\n"
        ));
    }

    private void handleTop(Player player) {
        List<String[]> top = plugin.getDuelElo().getTopTen();

        if (top.isEmpty()) {
            player.sendMessage(MINI.deserialize(PREFIX + "<gray>Noch keine Spieler in der Rangliste."));
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n<dark_purple><bold>1v1 Top 10</bold></dark_purple>\n");

        for (int i = 0; i < top.size(); i++) {
            String[] entry = top.get(i);
            String name = "???";
            try {
                Player p = Bukkit.getPlayer(UUID.fromString(entry[0]));
                if (p != null) name = p.getName();
                else {
                    // Offline-Spieler Name aus Cache
                    var offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(entry[0]));
                    if (offlinePlayer.getName() != null) name = offlinePlayer.getName();
                }
            } catch (Exception ignored) {}

            String color = i == 0 ? "<gold>" : i == 1 ? "<gray>" : i == 2 ? "<#cd7f32>" : "<white>";
            sb.append(color).append("#").append(i + 1).append(" <yellow>").append(name)
              .append(" <gray>- <yellow>").append(entry[1]).append(" ELO <dark_gray>(")
              .append(entry[2]).append("W/").append(entry[3]).append("L)\n");
        }

        player.sendMessage(MINI.deserialize(sb.toString()));
    }

    private void handleChallenge(Player player, String targetName) {
        Player target = plugin.getServer().getPlayer(targetName);
        if (target == null || !target.isOnline()) {
            player.sendMessage(MINI.deserialize(PREFIX + "<red>Spieler <yellow>" + targetName + " <red>nicht gefunden!"));
            return;
        }

        plugin.getDuelManager().sendChallenge(player, target);
    }

    private void handleSetup(Player player, String[] args) {
        if (!player.hasPermission("hmy.1v1.admin")) {
            player.sendMessage(MINI.deserialize(PREFIX + "<red>Keine Berechtigung!"));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(MINI.deserialize(PREFIX + "<gray>Nutzung: <yellow>/duel setup <arenaName> <spawn1|spawn2>"));
            player.sendMessage(MINI.deserialize(PREFIX + "<gray>Stell dich an die gewuenschte Position und setze den Spawnpunkt."));
            return;
        }

        if (args.length < 3) {
            player.sendMessage(MINI.deserialize(PREFIX + "<gray>Nutzung: <yellow>/duel setup " + args[1] + " <spawn1|spawn2>"));
            return;
        }

        String arenaName = args[1];
        String spawnArg = args[2].toLowerCase();

        if (!spawnArg.equals("spawn1") && !spawnArg.equals("spawn2")) {
            player.sendMessage(MINI.deserialize(PREFIX + "<red>Ungueltig! Nutze <yellow>spawn1 <red>oder <yellow>spawn2<red>."));
            return;
        }

        org.bukkit.Location loc = player.getLocation();
        DuelArena.ArenaData existing = plugin.getDuelArena().getArena(arenaName);

        if (spawnArg.equals("spawn1")) {
            org.bukkit.Location spawn2 = existing != null ? existing.getSpawn2() : loc;
            plugin.getDuelArena().saveArena(arenaName, loc, spawn2);
            player.sendMessage(MINI.deserialize(PREFIX + "<green>Spawn 1 fuer Arena <yellow>" + arenaName + " <green>gesetzt!"));
        } else {
            org.bukkit.Location spawn1 = existing != null ? existing.getSpawn1() : loc;
            plugin.getDuelArena().saveArena(arenaName, spawn1, loc);
            player.sendMessage(MINI.deserialize(PREFIX + "<green>Spawn 2 fuer Arena <yellow>" + arenaName + " <green>gesetzt!"));
        }

        player.sendMessage(MINI.deserialize(PREFIX + "<gray>Position: <white>" +
                String.format("%.1f, %.1f, %.1f", loc.getX(), loc.getY(), loc.getZ())));

        DuelArena.ArenaData arena = plugin.getDuelArena().getArena(arenaName);
        if (arena != null) {
            player.sendMessage(MINI.deserialize(PREFIX + "<green>Arena <yellow>" + arenaName + " <green>ist bereit!"));
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(MINI.deserialize(
                "\n<dark_purple><bold>1v1 DUELL</bold></dark_purple> <gray>- Kit-basiertes Duell-System\n" +
                "<yellow>/duel <Spieler> <gray>- Spieler herausfordern\n" +
                "<yellow>/duel accept <gray>- Herausforderung annehmen\n" +
                "<yellow>/duel deny <gray>- Herausforderung ablehnen\n" +
                "<yellow>/duel join [ranked|unranked] <gray>- Warteschlange beitreten\n" +
                "<yellow>/duel leave <gray>- Verlassen\n" +
                "<yellow>/duel kit <gray>- Kit auswaehlen\n" +
                "<yellow>/duel stats [Spieler] <gray>- Statistiken anzeigen\n" +
                "<yellow>/duel top <gray>- Top 10 Rangliste\n" +
                "<yellow>/duel spectate <Spieler> <gray>- Zuschauer-Modus\n" +
                (player.hasPermission("hmy.1v1.admin") ?
                        "<yellow>/duel setup <arenaName> <spawn1|spawn2> <gray>- Arena einrichten\n" : "")
        ));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("accept", "deny", "join", "leave", "kit", "stats", "top", "spectate"));
            if (sender.hasPermission("hmy.1v1.admin")) {
                completions.add("setup");
            }
            // Online-Spieler fuer direkte Herausforderung
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (!player.getName().equals(sender.getName())) {
                    completions.add(player.getName());
                }
            }
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "join" -> completions.addAll(Arrays.asList("ranked", "unranked"));
                case "stats" -> {
                    for (Player player : plugin.getServer().getOnlinePlayers()) {
                        completions.add(player.getName());
                    }
                }
                case "spectate" -> {
                    for (Player player : plugin.getServer().getOnlinePlayers()) {
                        if (plugin.getDuelManager().isInGame(player.getUniqueId())) {
                            completions.add(player.getName());
                        }
                    }
                }
                case "setup" -> {
                    if (sender.hasPermission("hmy.1v1.admin")) {
                        for (DuelArena.ArenaData arena : plugin.getDuelArena().getArenas()) {
                            completions.add(arena.getName());
                        }
                    }
                }
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("setup") && sender.hasPermission("hmy.1v1.admin")) {
                completions.addAll(Arrays.asList("spawn1", "spawn2"));
            }
        }

        // Filter basierend auf aktuellem Input
        String input = args[args.length - 1].toLowerCase();
        completions.removeIf(c -> !c.toLowerCase().startsWith(input));

        return completions;
    }
}
