package de.haaremy.hmypaper.commands;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ComGive implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cBenutzung: /give <Spieler> <Item> [Menge]");
            return true;
        }

        String playerName = args[0];
        Player target = Bukkit.getPlayerExact(playerName);
        if (target == null) {
            sender.sendMessage("§cSpieler " + playerName + " nicht gefunden.");
            return true;
        }

        Material material = Material.matchMaterial(args[1]);
        if (material == null) {
            sender.sendMessage("§cDas Item \"" + args[1] + "\" existiert nicht.");
            return true;
        }

        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
                if (amount <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                sender.sendMessage("§cDie Menge muss eine positive Zahl sein.");
                return true;
            }
        }

        ItemStack item = new ItemStack(material, amount);
        target.getInventory().addItem(item);
        sender.sendMessage("§a" + amount + " " + material.name() + " wurde an " + target.getName() + " gegeben.");
        target.sendMessage("§aDu hast " + amount + " " + material.name() + " erhalten.");
        return true;
    }
}
