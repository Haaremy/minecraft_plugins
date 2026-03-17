package de.haaremy.hmypaper.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class ComFeed implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("hmy.feed")) {
            sender.sendMessage("§cDu hast keine Berechtigung, diesen Befehl zu verwenden.");
            return true;
        }

        Player target;
        if (args.length >= 1) {
            if (!sender.hasPermission("hmy.feed.other")) {
                sender.sendMessage("§cDu hast keine Berechtigung, andere Spieler zu sättigen.");
                return true;
            }
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§cSpieler §e" + args[0] + " §cnicht gefunden oder offline.");
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cAls Konsole muss ein Spieler angegeben werden: /feed <Spieler>");
                return true;
            }
            target = (Player) sender;
        }

        target.setFoodLevel(20);
        target.setSaturation(20f);
        target.setExhaustion(0f);

        if (target.equals(sender)) {
            sender.sendMessage("§aDu wurdest gesättigt.");
        } else {
            sender.sendMessage("§e" + target.getName() + " §awurde gesättigt.");
            target.sendMessage("§aDu wurdest von §e" + sender.getName() + " §agesättigt.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("hmy.feed.other")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
