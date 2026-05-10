package de.haaremy.hmyparkour;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ParkourCommand implements CommandExecutor, TabCompleter {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final String PREFIX = "<gold>[Parkour]</gold> ";

    private final HmyParkour plugin;
    private final ParkourManager manager;

    public ParkourCommand(HmyParkour plugin, ParkourManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Nur Spieler koennen diesen Befehl verwenden.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list" -> handleList(player);
            case "join" -> handleJoin(player, args);
            case "quit" -> manager.quitCourse(player);
            case "checkpoint", "cp" -> manager.teleportToCheckpoint(player);
            case "top" -> handleTop(player, args);
            case "setup" -> handleSetup(player, args);
            default -> sendHelp(player);
        }

        return true;
    }

    private void handleList(Player player) {
        Map<String, ParkourCourse> courses = manager.getCourses();
        if (courses.isEmpty()) {
            sendMessage(player, "<yellow>Keine Parkour-Kurse vorhanden.");
            return;
        }

        sendMessage(player, "<gold><bold>Verfuegbare Kurse:</bold></gold>");
        for (Map.Entry<String, ParkourCourse> entry : courses.entrySet()) {
            ParkourCourse course = entry.getValue();
            String diffColor = course.getDifficulty().getMiniMessageColor();
            String status = course.isComplete() ? "<green>Bereit" : "<red>Unvollstaendig";

            Long bestTime = manager.getLeaderboard().getBestTime(entry.getKey(), player.getUniqueId());
            String bestTimeStr = bestTime != null ? ParkourSession.formatTime(bestTime) : "---";

            sendMessage(player, diffColor + course.getName() + " <gray>| " +
                    diffColor + course.getDifficulty().getDisplayName() +
                    " <gray>| Bestzeit: <white>" + bestTimeStr +
                    " <gray>| <gold>" + course.getCoinReward() + " Coins" +
                    " <gray>| " + status);
        }
    }

    private void handleJoin(Player player, String[] args) {
        if (args.length < 2) {
            sendMessage(player, "<red>Benutzung: <white>/parkour join <Kurs>");
            return;
        }
        manager.startCourse(player, args[1]);
    }

    private void handleTop(Player player, String[] args) {
        if (args.length < 2) {
            sendMessage(player, "<red>Benutzung: <white>/parkour top <Kurs>");
            return;
        }

        String courseName = args[1].toLowerCase();
        ParkourCourse course = manager.getCourses().get(courseName);
        if (course == null) {
            sendMessage(player, "<red>Kurs <white>" + args[1] + "</white> nicht gefunden!");
            return;
        }

        List<ParkourLeaderboard.LeaderboardEntry> top = manager.getLeaderboard().getTopTimes(courseName, 10);
        if (top.isEmpty()) {
            sendMessage(player, "<yellow>Noch keine Zeiten fuer <white>" + course.getName() + "</white>.");
            return;
        }

        String diffColor = course.getDifficulty().getMiniMessageColor();
        sendMessage(player, "<gold><bold>Top-10 " + diffColor + course.getName() + "</bold></gold>");

        for (ParkourLeaderboard.LeaderboardEntry entry : top) {
            String rankColor = switch (entry.getRank()) {
                case 1 -> "<gold>";
                case 2 -> "<gray>";
                case 3 -> "<#CD7F32>";
                default -> "<white>";
            };
            sendMessage(player, rankColor + "#" + entry.getRank() + " <white>" + entry.getPlayerName() +
                    " <gray>- <yellow>" + ParkourSession.formatTime(entry.getTimeMs()));
        }
    }

    private void handleSetup(Player player, String[] args) {
        if (!player.hasPermission("hmyparkour.admin")) {
            sendMessage(player, "<red>Keine Berechtigung!");
            return;
        }

        if (args.length < 2) {
            sendSetupHelp(player);
            return;
        }

        switch (args[1].toLowerCase()) {
            case "create" -> {
                if (args.length < 4) {
                    sendMessage(player, "<red>Benutzung: <white>/parkour setup create <Name> <Schwierigkeit>");
                    return;
                }
                String name = args[2];
                ParkourDifficulty difficulty;
                try {
                    difficulty = ParkourDifficulty.valueOf(args[3].toUpperCase());
                } catch (IllegalArgumentException e) {
                    sendMessage(player, "<red>Ungueltige Schwierigkeit! Verfuegbar: EASY, MEDIUM, HARD, EXPERT");
                    return;
                }
                if (manager.createCourse(name, difficulty, player.getLocation())) {
                    sendMessage(player, "<green>Kurs <white>" + name + "</white> erstellt! Startpunkt gesetzt.");
                    sendMessage(player, "<gray>Setze jetzt das Ziel mit <white>/parkour setup end " + name);
                } else {
                    sendMessage(player, "<red>Ein Kurs mit diesem Namen existiert bereits!");
                }
            }
            case "end" -> {
                if (args.length < 3) {
                    sendMessage(player, "<red>Benutzung: <white>/parkour setup end <Name>");
                    return;
                }
                if (manager.setEnd(args[2], player.getLocation())) {
                    sendMessage(player, "<green>Zielpunkt fuer <white>" + args[2] + "</white> gesetzt!");
                } else {
                    sendMessage(player, "<red>Kurs nicht gefunden!");
                }
            }
            case "addcheckpoint" -> {
                if (args.length < 3) {
                    sendMessage(player, "<red>Benutzung: <white>/parkour setup addcheckpoint <Name>");
                    return;
                }
                if (manager.addCheckpoint(args[2], player.getLocation())) {
                    ParkourCourse course = manager.getCourses().get(args[2].toLowerCase());
                    int count = course != null ? course.getCheckpoints().size() : 0;
                    sendMessage(player, "<green>Checkpoint <white>#" + count + "</white> fuer <white>" + args[2] + "</white> hinzugefuegt!");
                } else {
                    sendMessage(player, "<red>Kurs nicht gefunden!");
                }
            }
            case "delete" -> {
                if (args.length < 3) {
                    sendMessage(player, "<red>Benutzung: <white>/parkour setup delete <Name>");
                    return;
                }
                if (manager.deleteCourse(args[2])) {
                    sendMessage(player, "<green>Kurs <white>" + args[2] + "</white> geloescht!");
                } else {
                    sendMessage(player, "<red>Kurs nicht gefunden!");
                }
            }
            default -> sendSetupHelp(player);
        }
    }

    private void sendHelp(Player player) {
        sendMessage(player, "<gold><bold>Parkour Befehle:</bold></gold>");
        sendMessage(player, "<yellow>/parkour list <gray>- Alle Kurse anzeigen");
        sendMessage(player, "<yellow>/parkour join <Kurs> <gray>- Kurs starten");
        sendMessage(player, "<yellow>/parkour quit <gray>- Kurs verlassen");
        sendMessage(player, "<yellow>/parkour checkpoint <gray>- Zum Checkpoint teleportieren");
        sendMessage(player, "<yellow>/parkour top <Kurs> <gray>- Top-10 Bestzeiten");
    }

    private void sendSetupHelp(Player player) {
        sendMessage(player, "<gold><bold>Parkour Setup:</bold></gold>");
        sendMessage(player, "<yellow>/parkour setup create <Name> <Schwierigkeit> <gray>- Neuen Kurs erstellen");
        sendMessage(player, "<yellow>/parkour setup end <Name> <gray>- Zielpunkt setzen");
        sendMessage(player, "<yellow>/parkour setup addcheckpoint <Name> <gray>- Checkpoint hinzufuegen");
        sendMessage(player, "<yellow>/parkour setup delete <Name> <gray>- Kurs loeschen");
    }

    private void sendMessage(Player player, String miniMessageText) {
        player.sendMessage(MINI.deserialize(PREFIX + miniMessageText));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("list", "join", "quit", "checkpoint", "top"));
            if (sender.hasPermission("hmyparkour.admin")) {
                completions.add("setup");
            }
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "join", "top" -> completions.addAll(manager.getCourses().keySet());
                case "setup" -> {
                    if (sender.hasPermission("hmyparkour.admin")) {
                        completions.addAll(Arrays.asList("create", "end", "addcheckpoint", "delete"));
                    }
                }
            }
        } else if (args.length == 3) {
            switch (args[0].toLowerCase()) {
                case "setup" -> {
                    if (sender.hasPermission("hmyparkour.admin")) {
                        switch (args[1].toLowerCase()) {
                            case "end", "addcheckpoint", "delete" -> completions.addAll(manager.getCourses().keySet());
                        }
                    }
                }
            }
        } else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("setup") && args[1].equalsIgnoreCase("create")) {
                completions.addAll(Arrays.asList("EASY", "MEDIUM", "HARD", "EXPERT"));
            }
        }

        String lastArg = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(lastArg))
                .collect(Collectors.toList());
    }
}
