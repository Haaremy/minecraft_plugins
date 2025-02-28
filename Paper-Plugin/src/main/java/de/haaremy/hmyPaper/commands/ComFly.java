package de.haaremy.hmypaper.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import de.haaremy.hmypaper.HmyLanguageManager;
import de.haaremy.hmypaper.utils.PermissionUtils;

public class ComFly implements CommandExecutor {

    private final HmyLanguageManager language;

    public ComFly(HmyLanguageManager language) {
        this.language = language;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            if (! PermissionUtils.hasPermission(player, "hmy.fly")) {
                language.getMessage("p_no_permission", "Keine Berechtigung.");
                return false;
            }
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
