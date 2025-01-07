package de.haaremy.hmypaper;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ComVanish implements CommandExecutor {

    private static final Set<Player> vanishedPlayers = new HashSet<>();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("hmy.vanish")) {
            sender.sendMessage("§cDu hast keine Berechtigung, diesen Befehl zu verwenden.");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("§cNur Spieler können diesen Befehl verwenden.");
            return true;
        }

        Player player = (Player) sender;

        if (vanishedPlayers.contains(player)) {
            vanishedPlayers.remove(player);
            for (Player other : Bukkit.getOnlinePlayers()) {
                other.showPlayer(player);
            }
            player.sendMessage("§aDu bist jetzt sichtbar.");
        } else {
            vanishedPlayers.add(player);
            for (Player other : Bukkit.getOnlinePlayers()) {
                other.hidePlayer(player);
            }
            player.sendMessage("§aDu bist jetzt unsichtbar.");
        }
        return true;
    }
}
