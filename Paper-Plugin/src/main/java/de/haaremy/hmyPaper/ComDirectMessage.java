package de.haaremy.hmypaper;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ComDirectMessage implements CommandExecutor {

    private static final Map<Player, Player> lastMessaged = new HashMap<>();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("hmy.dm")) {
            sender.sendMessage("§cDu hast keine Berechtigung, diesen Befehl zu verwenden.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cVerwendung: /dm [player] [message]");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage("§cSpieler nicht gefunden.");
            return true;
        }

        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));


        target.sendMessage("§a[DM] " + sender.getName() + ": §f" + message);
        sender.sendMessage("§a[DM] An " + target.getName() + ": §f" + message);

        if (sender instanceof Player) {
            lastMessaged.put((Player) sender, target);
        }
        return true;
    }

    public static Player getLastMessaged(Player player) {
        return lastMessaged.get(player);
    }
}
