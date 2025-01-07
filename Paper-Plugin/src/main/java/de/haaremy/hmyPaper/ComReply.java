package de.haaremy.hmypaper;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ComReply implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("hmy.r")) {
            sender.sendMessage("§cDu hast keine Berechtigung, diesen Befehl zu verwenden.");
            return true;
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
