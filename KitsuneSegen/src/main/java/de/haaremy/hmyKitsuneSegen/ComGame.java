package de.haaremy.hmykitsunesegen;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * /game <sub-command>
 *
 * Sub-commands:
 *   start   – Force-start the game now
 *   stop    – Force-end the game and return everyone to hub
 *   info    – Show current game state
 *   kick    – Remove a player from the current game
 *   reset   – Reset game state without world reset (admin)
 */
public class ComGame implements CommandExecutor, TabCompleter {

    private static final String PERM = "hmy.kitsunesegen.admin";
    private final HmyKitsuneSegen plugin;

    public ComGame(HmyKitsuneSegen plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERM)) {
            sender.sendMessage(ChatColor.RED + "Keine Berechtigung.");
            return true;
        }
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        GameManager gm = plugin.getGameManager();

        switch (args[0].toLowerCase()) {

            case "start" -> {
                if (gm.isRunning()) {
                    sender.sendMessage(ChatColor.RED + "Spiel läuft bereits.");
                    return true;
                }
                if (gm.getState() == GameManager.State.COUNTDOWN) {
                    // Skip countdown
                    plugin.getGameManager().cancelCountdown();
                }
                sender.sendMessage(ChatColor.GREEN + "Starte Spiel erzwungen…");
                gm.startGame();
            }

            case "countdown" -> {
                if (gm.getState() != GameManager.State.WAITING) {
                    sender.sendMessage(ChatColor.RED + "Countdown kann nur aus dem WAITING-Zustand gestartet werden.");
                    return true;
                }
                gm.startCountdown();
                sender.sendMessage(ChatColor.GREEN + "Countdown gestartet.");
            }

            case "stop" -> {
                if (gm.getState() == GameManager.State.WAITING) {
                    sender.sendMessage(ChatColor.RED + "Kein Spiel aktiv.");
                    return true;
                }
                sender.sendMessage(ChatColor.YELLOW + "Beende Spiel…");
                if (gm.getState() == GameManager.State.COUNTDOWN) {
                    gm.cancelCountdown();
                    Bukkit.broadcast(net.kyori.adventure.text.Component.text(ChatColor.RED + "Spiel abgebrochen."));
                } else {
                    gm.endGame(null);
                }
            }

            case "info" -> {
                sender.sendMessage(ChatColor.GOLD + "── Kitsune Segen Status ──");
                sender.sendMessage(ChatColor.YELLOW + "Zustand: " + ChatColor.WHITE + gm.getState().name());
                sender.sendMessage(ChatColor.YELLOW + "Lebende Spieler: " + ChatColor.GREEN + gm.getAliveCount());
                for (Player p : gm.getAlivePlayers()) {
                    sender.sendMessage(ChatColor.GRAY + "  • " + p.getName()
                            + ChatColor.YELLOW + " (Kills: " + gm.getKills(p) + ")");
                }
            }

            case "kick" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Verwendung: /game kick <Spieler>");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Spieler nicht gefunden.");
                    return true;
                }
                if (!gm.isAlive(target)) {
                    sender.sendMessage(ChatColor.RED + target.getName() + " ist nicht im Spiel.");
                    return true;
                }
                gm.eliminatePlayer(target, null);
                sender.sendMessage(ChatColor.GREEN + target.getName() + " wurde aus dem Spiel entfernt.");
            }

            case "reset" -> {
                gm.reset();
                sender.sendMessage(ChatColor.GREEN + "Spielstatus zurückgesetzt (kein Weltrestart).");
            }

            default -> sendUsage(sender);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(PERM)) return List.of();
        if (args.length == 1) {
            List<String> subs = Arrays.asList("start", "countdown", "stop", "info", "kick", "reset");
            List<String> result = new ArrayList<>();
            for (String s : subs) {
                if (s.startsWith(args[0].toLowerCase())) result.add(s);
            }
            return result;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("kick")) {
            List<String> players = new ArrayList<>();
            for (Player p : plugin.getGameManager().getAlivePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    players.add(p.getName());
                }
            }
            return players;
        }
        return List.of();
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "Verwendung:");
        sender.sendMessage(ChatColor.YELLOW + "/game start §7- Spiel sofort starten");
        sender.sendMessage(ChatColor.YELLOW + "/game countdown §7- Countdown starten");
        sender.sendMessage(ChatColor.YELLOW + "/game stop §7- Spiel beenden");
        sender.sendMessage(ChatColor.YELLOW + "/game info §7- Status anzeigen");
        sender.sendMessage(ChatColor.YELLOW + "/game kick <Spieler> §7- Spieler entfernen");
        sender.sendMessage(ChatColor.YELLOW + "/game reset §7- Zustand zurücksetzen");
    }
}
