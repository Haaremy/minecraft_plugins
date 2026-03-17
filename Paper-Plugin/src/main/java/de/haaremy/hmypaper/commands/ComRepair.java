package de.haaremy.hmypaper.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class ComRepair implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("hmy.repair")) {
            sender.sendMessage("§cDu hast keine Berechtigung, diesen Befehl zu verwenden.");
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cNur Spieler können diesen Befehl ausführen.");
            return true;
        }

        boolean all = args.length >= 1 && args[0].equalsIgnoreCase("all");

        if (all) {
            if (!sender.hasPermission("hmy.repair.all")) {
                sender.sendMessage("§cDu hast keine Berechtigung, alle Items zu reparieren.");
                return true;
            }
            int repaired = 0;
            for (ItemStack item : player.getInventory().getContents()) {
                if (repairItem(item)) repaired++;
            }
            sender.sendMessage("§a" + repaired + " Item(s) repariert.");
        } else {
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand.getType().isAir()) {
                sender.sendMessage("§cDu hältst kein Item in der Hand.");
                return true;
            }
            if (repairItem(hand)) {
                sender.sendMessage("§aDas Item in deiner Hand wurde repariert.");
            } else {
                sender.sendMessage("§cDieses Item kann nicht repariert werden.");
            }
        }
        return true;
    }

    private boolean repairItem(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof Damageable damageable)) return false;
        if (damageable.getDamage() == 0) return false;
        damageable.setDamage(0);
        item.setItemMeta(damageable);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("hmy.repair.all")) {
            return List.of("all");
        }
        return List.of();
    }
}
