package de.haaremy.hmypaper;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ComFly implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl kann nur von Spielern ausgeführt werden.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            // Fly für den ausführenden Spieler toggeln
            boolean isFlying = player.getAllowFlight();
            player.setAllowFlight(!isFlying);
            player.sendMessage("§eFlugmodus: " + (!isFlying ? "Aktiviert" : "Deaktiviert"));
        } else {
            // Fly für anderen Spieler toggeln
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                player.sendMessage("§cSpieler nicht gefunden.");
                return true;
            }
            boolean isFlying = target.getAllowFlight();
            target.setAllowFlight(!isFlying);
            target.sendMessage("§eFlugmodus: " + (!isFlying ? "Aktiviert" : "Deaktiviert"));
            player.sendMessage("§aFlugmodus für " + target.getName() + ": " + (!isFlying ? "Aktiviert" : "Deaktiviert"));
        }
        return true;
    }
}
