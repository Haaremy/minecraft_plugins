package de.haaremy.hmypaper.commands;

import de.haaremy.hmypaper.parkour.ParkourListener;
import de.haaremy.hmypaper.parkour.ParkourManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * /parkour <create|delete|setstart|setgoal|setcheckpoint|quit|list> [args]
 *
 * Permissions:
 *   hmy.parkour.admin  – create/delete/setstart/setgoal/setcheckpoint
 *   hmy.parkour        – quit/list
 */
public class ComParkour implements CommandExecutor, TabCompleter {

    private final ParkourManager  manager;
    private final ParkourListener listener;

    public ComParkour(ParkourManager manager, ParkourListener listener) {
        this.manager  = manager;
        this.listener = listener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        // ── Player-only sub-commands ────────────────────────────────────────
        if (sub.equals("quit")) {
            if (!(sender instanceof Player player)) { sender.sendMessage("§cNur Spieler."); return true; }
            listener.quitParkour(player);
            return true;
        }

        if (sub.equals("list")) {
            Set<String> names = manager.getParkourNames();
            if (names.isEmpty()) { sender.sendMessage("§7Keine Parkours vorhanden."); return true; }
            sender.sendMessage("§6§lParkours: §e" + String.join("§7, §e", names));
            return true;
        }

        // ── Admin sub-commands ──────────────────────────────────────────────
        if (!sender.hasPermission("hmy.parkour.admin")) {
            sender.sendMessage("§cDu hast keine Berechtigung.");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cNur Spieler können Parkour-Blöcke setzen.");
            return true;
        }

        switch (sub) {
            case "create" -> {
                if (args.length < 2) { sender.sendMessage("§cVerwendung: /parkour create <Name>"); return true; }
                String name = args[1];
                if (manager.parkourExists(name)) { sender.sendMessage("§cEin Parkour namens §e" + name + " §cexistiert bereits."); return true; }
                manager.createParkour(name);
                sender.sendMessage("§aParkour §e" + name + " §aerstellt.");
            }
            case "delete" -> {
                if (args.length < 2) { sender.sendMessage("§cVerwendung: /parkour delete <Name>"); return true; }
                String name = args[1];
                if (!manager.parkourExists(name)) { sender.sendMessage("§cParkour §e" + name + " §cexistiert nicht."); return true; }
                manager.deleteParkour(name);
                sender.sendMessage("§cParkour §e" + name + " §cgelöscht.");
            }
            case "setstart" -> {
                if (args.length < 2) { sender.sendMessage("§cVerwendung: /parkour setstart <Name>"); return true; }
                String name = args[1];
                requireExists(sender, name);
                manager.setStart(name, player.getLocation());
                sender.sendMessage("§aStartblock für §e" + name + " §agesetzt.");
            }
            case "setgoal" -> {
                if (args.length < 2) { sender.sendMessage("§cVerwendung: /parkour setgoal <Name>"); return true; }
                String name = args[1];
                requireExists(sender, name);
                manager.setGoal(name, player.getLocation());
                sender.sendMessage("§aZielblock für §e" + name + " §agesetzt.");
            }
            case "setcheckpoint" -> {
                if (args.length < 3) { sender.sendMessage("§cVerwendung: /parkour setcheckpoint <Name> <ID>"); return true; }
                String name = args[1];
                requireExists(sender, name);
                int id;
                try { id = Integer.parseInt(args[2]); } catch (NumberFormatException e) {
                    sender.sendMessage("§cID muss eine Zahl sein."); return true;
                }
                manager.setCheckpoint(name, id, player.getLocation());
                sender.sendMessage("§aCheckpoint §e" + id + " §afür §e" + name + " §agesetzt.");
            }
            default -> sendUsage(sender);
        }
        return true;
    }

    private boolean requireExists(CommandSender sender, String name) {
        if (!manager.parkourExists(name)) {
            sender.sendMessage("§cParkour §e" + name + " §cexistiert nicht. Erstelle ihn mit §e/parkour create " + name + "§c.");
            return false;
        }
        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("§6/parkour §ecreate §7<Name> §8| §ecreate §8| §edelete §8| §esetstart §8| §esetgoal §8| §esetCheckpoint §8| §equit §8| §elist");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = List.of("create", "delete", "setstart", "setgoal", "setcheckpoint", "quit", "list");
            return subs.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 2 && !args[0].equalsIgnoreCase("create")) {
            return manager.getParkourNames().stream()
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
