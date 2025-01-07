package de.haaremy.hmypaper;

import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ComGamemode implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("hmy.gm")) {
            sender.sendMessage("§cDu hast keine Berechtigung, diesen Befehl zu verwenden.");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("§cNur Spieler können diesen Befehl ausführen.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 1) {
            sender.sendMessage("§cVerwendung: /gm [name|id]");
            return true;
        }

        try {
            GameMode mode = GameMode.valueOf(args[0].toUpperCase());
            player.setGameMode(mode);
            sender.sendMessage("§aSpielmodus geändert zu: " + mode.name());
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§cUngültiger Spielmodus.");
        }
        return true;
    }
}
