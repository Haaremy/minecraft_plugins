package de.haaremy.hmypaper.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import de.haaremy.hmypaper.HmyLanguageManager;
import de.haaremy.hmypaper.utils.PermissionUtils;

public class ComReply implements CommandExecutor {

    private final HmyLanguageManager language;

    public ComReply(HmyLanguageManager language) {
        this.language = language;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            if (PermissionUtils.hasPermission(player, "hmy.r")) {
            } else {
                language.getMessage("p_no_permission", "Keine Berechtigung.");
                return false;
            }
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("§cNur Spieler können diesen Befehl verwenden.");
            return true;
        }

        Player player = (Player) sender;
        Player lastMessaged = ComDirectMessage.getLastMessaged(player);

        if (lastMessaged == null) {
            player.sendMessage("§cEs gibt keine letzte Nachricht, auf die geantwortet werden kann.");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage("§cVerwendung: /r [message]");
            return true;
        }

        String message = String.join(" ", args);

        lastMessaged.sendMessage("§a[DM] " + player.getName() + ": §f" + message);
        player.sendMessage("§a[DM] An " + lastMessaged.getName() + ": §f" + message);

        return true;
    }
}
