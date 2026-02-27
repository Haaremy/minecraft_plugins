package de.haaremy.hmypaper.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class ComInvSee implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cDieser Befehl kann nur von Spielern verwendet werden.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§cBenutzung: /invsee <Spieler>");
            return true;
        }

        Player viewer = (Player) sender;
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target != null) {
            Inventory targetInventory = target.getInventory();
            viewer.openInventory(targetInventory);
            viewer.sendMessage("§eDu siehst jetzt das Inventar von " + target.getName() + ".");
        } else {
            viewer.sendMessage("§cSpieler " + args[0] + " nicht gefunden.");
        }
        return true;
    }
}
