package de.haaremy.hmypaper.commands;

import de.haaremy.hmypaper.HomeManager;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class ComHome implements CommandExecutor, TabCompleter {

    private final HomeManager homeManager;

    public ComHome(HomeManager homeManager) {
        this.homeManager = homeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cNur Spieler können diesen Befehl verwenden.");
            return true;
        }

        int slot = 1;
        if (args.length >= 1) {
            try {
                slot = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cUngültiger Slot. Verwende eine Zahl von 1 bis 5.");
                return true;
            }
        }

        if (slot < 1 || slot > 5) {
            sender.sendMessage("§cSlot muss zwischen §e1 §cund §e5 §cliegen.");
            return true;
        }

        if (!player.getWorld().getName().startsWith("survival_")) {
            player.sendMessage("§c/home kann nur in Survival-Welten verwendet werden.");
            return true;
        }

        if (!player.hasPermission("hmy.home." + slot)) {
            player.sendMessage("§cDu hast keine Berechtigung für Home-Slot §e" + slot + "§c.");
            return true;
        }

        Location home = homeManager.getHome(player.getUniqueId(), slot);
        if (home == null) {
            player.sendMessage("§cHome §e" + slot + " §cist nicht gesetzt. Nutze §e/sethome " + slot + "§c.");
            return true;
        }

        player.teleport(home);
        player.sendMessage("§aDu wurdest zu Home §e" + slot + " §ateleportiert.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("1", "2", "3", "4", "5").stream()
                    .filter(s -> s.startsWith(args[0]))
                    .toList();
        }
        return List.of();
    }
}
