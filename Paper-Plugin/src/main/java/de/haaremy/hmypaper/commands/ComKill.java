package de.haaremy.hmypaper.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class ComKill implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§cBenutzung: /kill <Spieler|Entity>");
            return true;
        }

        String targetName = args[0];
        Player targetPlayer = Bukkit.getPlayerExact(targetName);
        if (targetPlayer != null) {
            targetPlayer.setHealth(0);
            sender.sendMessage("§eSpieler " + targetName + " wurde getötet.");
        } else {
            for (Entity entity : Bukkit.getWorlds().get(0).getEntities()) {
                if (entity.getName().equalsIgnoreCase(targetName)) {
                    entity.remove();
                    sender.sendMessage("§eEntity " + targetName + " wurde entfernt.");
                    return true;
                }
            }
            sender.sendMessage("§cKein Spieler oder Entity mit dem Namen " + targetName + " gefunden.");
        }
        return true;
    }
}
