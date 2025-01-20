package de.haaremy.hmypaper.commands;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ComLightning implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("hmy.lightning")) {
            sender.sendMessage("§cDu hast keine Berechtigung, diesen Befehl zu verwenden.");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("§cNur Spieler können diesen Befehl verwenden.");
            return true;
        }

        Player player = (Player) sender;

        Location location = player.getTargetBlock(null, 100).getLocation();
        location.getWorld().strikeLightning(location);
        sender.sendMessage("§aBlitz wurde am Zielpunkt beschworen.");

        return true;
    }
}
