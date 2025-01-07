package de.haaremy.hmypaper;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ComSudo implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cBenutzung: /sudo <Spieler> <Befehl>");
            return true;
        }

        String targetName = args[0];
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            sender.sendMessage("§cSpieler " + targetName + " nicht gefunden.");
            return true;
        }

        // Befehl aus den Argumenten zusammenfügen (ohne den Spielernamen)
        String cmd = String.join(" ", args).substring(args[0].length()).trim();

        // Spieler den Befehl ausführen lassen
        boolean success = target.performCommand(cmd);

        if (success) {
            sender.sendMessage("§eSpieler " + target.getName() + " hat den Befehl ausgeführt: " + cmd);
        } else {
            sender.sendMessage("§cDer Befehl konnte nicht ausgeführt werden.");
        }
        return true;
    }
}
