package de.haaremy.hmypaper;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ComRules implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("hmy.rules")) {
            sender.sendMessage("§cDu hast keine Berechtigung, diesen Befehl zu verwenden.");
            return true;
        }

        sender.sendMessage("§eServer-Regeln:");
        sender.sendMessage("§a1. Kein Griefing.");
        sender.sendMessage("§a2. Keine Beleidigungen.");
        sender.sendMessage("§a3. Keine Hacks.");
        // Weitere Regeln hinzufügen...
        return true;
    }
}
