package de.haaremy.hmyantibuild;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class CommandManager implements CommandExecutor {

    public CommandManager(JavaPlugin plugin, ConfigManager configManager) {
        plugin.getCommand("hmy").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player) || !sender.hasPermission("hmy.antibuild.editconfig")) {
            sender.sendMessage(ChatColor.RED + "Du hast keine Berechtigung dafür!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Ungültiger Befehl!");
            return true;
        }

        // Command handling logic
        return true;
    }
}
