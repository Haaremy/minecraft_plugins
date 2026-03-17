package de.haaremy.hmypaper.commands;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class ComHeal implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("hmy.heal")) {
            sender.sendMessage("§cDu hast keine Berechtigung, diesen Befehl zu verwenden.");
            return true;
        }

        Player target;
        if (args.length >= 1) {
            if (!sender.hasPermission("hmy.heal.other")) {
                sender.sendMessage("§cDu hast keine Berechtigung, andere Spieler zu heilen.");
                return true;
            }
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§cSpieler §e" + args[0] + " §cnicht gefunden oder offline.");
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cAls Konsole muss ein Spieler angegeben werden: /heal <Spieler>");
                return true;
            }
            target = (Player) sender;
        }

        double maxHealth = target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        target.setHealth(maxHealth);
        target.setFoodLevel(20);
        target.setSaturation(20f);
        target.setFireTicks(0);

        if (target.equals(sender)) {
            sender.sendMessage("§aDu wurdest geheilt.");
        } else {
            sender.sendMessage("§e" + target.getName() + " §awurde geheilt.");
            target.sendMessage("§aDu wurdest von §e" + sender.getName() + " §ageheilt.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("hmy.heal.other")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
