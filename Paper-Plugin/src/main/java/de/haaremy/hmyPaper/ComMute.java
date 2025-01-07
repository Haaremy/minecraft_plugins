package de.haaremy.hmypaper;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ComMute implements CommandExecutor {

    private static final Set<Player> mutedPlayers = new HashSet<>();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("hmy.mute")) {
            sender.sendMessage("§cDu hast keine Berechtigung, diesen Befehl zu verwenden.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage("§cVerwendung: /mute [player]");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);

        if (target == null) {
            sender.sendMessage("§cSpieler nicht gefunden.");
            return true;
        }

        if (mutedPlayers.contains(target)) {
            mutedPlayers.remove(target);
            sender.sendMessage("§a" + target.getName() + " wurde entmutet.");
        } else {
            mutedPlayers.add(target);
            sender.sendMessage("§a" + target.getName() + " wurde gemutet.");
        }
        return true;
    }

    public static boolean isMuted(Player player) {
        return mutedPlayers.contains(player);
    }
}
