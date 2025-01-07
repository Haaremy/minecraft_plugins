package de.haaremy.hmypaper;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ComHelp implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("hmy.help")) {
            sender.sendMessage("§cDu hast keine Berechtigung, diesen Befehl zu verwenden.");
            return true;
        }

        sender.sendMessage("§eVerfügbare Befehle:");
        sender.sendMessage("§a/help [Seite] - Zeigt eine Liste der Befehle.");
        sender.sendMessage("§a/rules [Seite] - Zeigt die Serverregeln.");
        // Weitere Befehle hinzufügen...
        return true;
    }
}
