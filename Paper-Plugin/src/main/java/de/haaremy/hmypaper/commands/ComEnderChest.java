package de.haaremy.hmypaper.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class ComEnderChest implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("hmy.enderchest")) {
            sender.sendMessage("§cDu hast keine Berechtigung, diesen Befehl zu verwenden.");
            return true;
        }

        Player target;
        if (args.length >= 1) {
            if (!sender.hasPermission("hmy.enderchest.other")) {
                sender.sendMessage("§cDu hast keine Berechtigung, die Endertruhe anderer Spieler zu öffnen.");
                return true;
            }
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§cSpieler §e" + args[0] + " §cnicht gefunden oder offline.");
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cAls Konsole muss ein Spieler angegeben werden: /enderchest <Spieler>");
                return true;
            }
            target = (Player) sender;
        }

        if (!(sender instanceof Player viewer)) {
            sender.sendMessage("§cNur Spieler können Inventare öffnen.");
            return true;
        }

        viewer.openInventory(target.getEnderChest());
        if (!target.equals(viewer)) {
            sender.sendMessage("§aEndertruhe von §e" + target.getName() + " §awird angezeigt.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("hmy.enderchest.other")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
