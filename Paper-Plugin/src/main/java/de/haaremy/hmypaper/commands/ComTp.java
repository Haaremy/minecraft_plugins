package de.haaremy.hmypaper.commands;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

/**
 * /tp <Spieler>                     → zu Spieler teleportieren
 * /tp <x> <y> <z>                   → zu Koordinaten teleportieren
 * /tp <Spieler> <Ziel-Spieler>      → Spieler zu Spieler (hmy.tp.other)
 * /tp <Spieler> <x> <y> <z>         → Spieler zu Koordinaten (hmy.tp.other)
 */
public class ComTp implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("hmy.tp")) {
            sender.sendMessage("§cDu hast keine Berechtigung, diesen Befehl zu verwenden.");
            return true;
        }

        // ── /tp <Spieler>  ────────────────────────────────────────────────────
        if (args.length == 1) {
            if (!(sender instanceof Player self)) {
                sender.sendMessage("§cAls Konsole: /tp <Spieler> <Ziel> oder /tp <Spieler> <x> <y> <z>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§cSpieler §e" + args[0] + " §cnicht gefunden oder offline.");
                return true;
            }
            self.teleport(target.getLocation());
            sender.sendMessage("§aDu wurdest zu §e" + target.getName() + " §ateleportiert.");
            return true;
        }

        // ── /tp <x> <y> <z>  ─────────────────────────────────────────────────
        if (args.length == 3 && isDouble(args[0]) && isDouble(args[1]) && isDouble(args[2])) {
            if (!(sender instanceof Player self)) {
                sender.sendMessage("§cAls Konsole: /tp <Spieler> <x> <y> <z>");
                return true;
            }
            Location origin = self.getLocation();
            if (origin.getWorld() == null) {
                sender.sendMessage("§cDeine aktuelle Welt ist nicht geladen.");
                return true;
            }
            Location dest = parseCoords(origin, args[0], args[1], args[2]);
            self.teleport(dest);
            sender.sendMessage("§aDu wurdest zu §e" + fmt(dest.getX()) + " " + fmt(dest.getY()) + " " + fmt(dest.getZ()) + " §ateleportiert.");
            return true;
        }

        // ── /tp <Spieler> <Ziel-Spieler>  ────────────────────────────────────
        if (args.length == 2) {
            if (!sender.hasPermission("hmy.tp.other")) {
                sender.sendMessage("§cDu hast keine Berechtigung, andere Spieler zu teleportieren.");
                return true;
            }
            Player player = Bukkit.getPlayer(args[0]);
            Player target = Bukkit.getPlayer(args[1]);
            if (player == null) {
                sender.sendMessage("§cSpieler §e" + args[0] + " §cnicht gefunden oder offline.");
                return true;
            }
            if (target == null) {
                sender.sendMessage("§cSpieler §e" + args[1] + " §cnicht gefunden oder offline.");
                return true;
            }
            player.teleport(target.getLocation());
            sender.sendMessage("§e" + player.getName() + " §awurde zu §e" + target.getName() + " §ateleportiert.");
            player.sendMessage("§aDu wurdest von §e" + sender.getName() + " §azu §e" + target.getName() + " §ateleportiert.");
            return true;
        }

        // ── /tp <Spieler> <x> <y> <z>  ───────────────────────────────────────
        if (args.length == 4 && isDouble(args[1]) && isDouble(args[2]) && isDouble(args[3])) {
            if (!sender.hasPermission("hmy.tp.other")) {
                sender.sendMessage("§cDu hast keine Berechtigung, andere Spieler zu teleportieren.");
                return true;
            }
            Player player = Bukkit.getPlayer(args[0]);
            if (player == null) {
                sender.sendMessage("§cSpieler §e" + args[0] + " §cnicht gefunden oder offline.");
                return true;
            }
            Location playerLoc = player.getLocation();
            if (playerLoc.getWorld() == null) {
                sender.sendMessage("§cDie Welt von §e" + player.getName() + " §cist nicht geladen.");
                return true;
            }
            Location dest = parseCoords(playerLoc, args[1], args[2], args[3]);
            player.teleport(dest);
            sender.sendMessage("§e" + player.getName() + " §awurde zu §e" + fmt(dest.getX()) + " " + fmt(dest.getY()) + " " + fmt(dest.getZ()) + " §ateleportiert.");
            player.sendMessage("§aDu wurdest von §e" + sender.getName() + " §azu den Koordinaten §e" + fmt(dest.getX()) + " " + fmt(dest.getY()) + " " + fmt(dest.getZ()) + " §ateleportiert.");
            return true;
        }

        sender.sendMessage("§cVerwendung: /tp <Spieler> | /tp <x> <y> <z> | /tp <Spieler> <Ziel> | /tp <Spieler> <x> <y> <z>");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            List<String> players = Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(input))
                    .collect(Collectors.toList());
            // Koordinaten-Hinweis wenn Eingabe nach Zahl aussieht
            if (input.isEmpty() || input.startsWith("-") || isPartialDouble(input)) {
                players.add("<x>");
            }
            return players;
        }
        if (args.length == 2 && sender.hasPermission("hmy.tp.other")) {
            // Zweites Arg: entweder Spieler oder x-Koordinate
            String input = args[1].toLowerCase();
            if (isDouble(args[0])) {
                return List.of("<y>");
            }
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(input))
                    .collect(Collectors.toList());
        }
        if (args.length == 3) {
            if (isDouble(args[0]) && isDouble(args[1])) return List.of("<z>");
            if (!isDouble(args[0]) && isDouble(args[1])) return List.of("<z>");
        }
        if (args.length == 4 && !isDouble(args[0]) && isDouble(args[1]) && isDouble(args[2])) {
            return List.of("<z>");
        }
        return List.of();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Parses a coordinate value, supporting relative (~) notation. */
    private Location parseCoords(Location origin, String sx, String sy, String sz) {
        double x = parseCoord(origin.getX(), sx);
        double y = parseCoord(origin.getY(), sy);
        double z = parseCoord(origin.getZ(), sz);
        return new Location(origin.getWorld(), x, y, z, origin.getYaw(), origin.getPitch());
    }

    private double parseCoord(double origin, String s) {
        if (s.startsWith("~")) {
            double offset = s.length() > 1 ? Double.parseDouble(s.substring(1)) : 0;
            return origin + offset;
        }
        return Double.parseDouble(s);
    }

    private boolean isDouble(String s) {
        if (s.startsWith("~")) return true;
        try { Double.parseDouble(s); return true; }
        catch (NumberFormatException e) { return false; }
    }

    private boolean isPartialDouble(String s) {
        return s.matches("-?[0-9]*\\.?[0-9]*");
    }

    private String fmt(double d) {
        return d == Math.floor(d) ? String.valueOf((int) d) : String.format("%.2f", d);
    }
}
