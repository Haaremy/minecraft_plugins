package de.haaremy.hmypaper;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ComSpeed implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2 || args.length > 3) {
            sender.sendMessage("Usage: /speed [walk|fly] <speed> [player]");
            return false;
        }

        String type = args[0];
        float speed;
        try {
            speed = Float.parseFloat(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage("Invalid speed value. Use a number between 0 and 10.");
            return false;
        }

        if (speed < 0 || speed > 10) {
            sender.sendMessage("Speed must be between 0 and 10.");
            return false;
        }

        Player target;
        if (args.length == 3) {
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage("Player not found.");
                return false;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage("Only players can use this command.");
            return false;
        }

        speed /= 10; // Convert to Bukkit speed scale (0.0 - 1.0)
        if (type.equalsIgnoreCase("fly")) {
            target.setFlySpeed(speed);
            sender.sendMessage(target.getName() + "'s fly speed set to " + (speed * 10) + ".");
        } else if (type.equalsIgnoreCase("walk")) {
            target.setWalkSpeed(speed);
            sender.sendMessage(target.getName() + "'s walk speed set to " + (speed * 10) + ".");
        } else {
            sender.sendMessage("Invalid type. Use 'walk' or 'fly'.");
        }

        return true;
    }
}
