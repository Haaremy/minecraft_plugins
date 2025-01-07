package de.haaremy.hmypaper;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ComGetPos implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cDieser Befehl kann nur von Spielern verwendet werden.");
            return true;
        }

        Player player = (Player) sender;
        Location loc = player.getLocation();
        player.sendMessage(String.format("§eDeine aktuelle Position: X: %.2f, Y: %.2f, Z: %.2f, Welt: %s",
                loc.getX(), loc.getY(), loc.getZ(), loc.getWorld().getName()));
        return true;
    }
}
