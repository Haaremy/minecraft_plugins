package de.haaremy.hmypaper.commands;

import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import de.haaremy.hmypaper.HmyLanguageManager;
import de.haaremy.hmypaper.utils.PermissionUtils;

public class ComGamemode implements CommandExecutor {

    private final HmyLanguageManager language;

    public ComGamemode(HmyLanguageManager language) {
        this.language = language;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            if (! PermissionUtils.hasPermission(player, "hmy.gm")) {
                language.getMessage("p_no_permission", "Keine Berechtigung.");
                return false;
            }
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
            GameMode mode = GameMode.valueOf("SURVIVAL");
            try {
                int number = Integer.parseInt(args[0]); 
                switch (number){
                    case 0: mode = GameMode.valueOf("SURVIVAL"); break;
                    case 1: mode = GameMode.valueOf("CREATIVE"); break;
                    case 2: mode = GameMode.valueOf("SPECTATOR"); break;
            }
                
    } catch (NumberFormatException e) {
        // args[0] ist kein gültiger Integer
       mode = GameMode.valueOf(args[0].toUpperCase());
    }
            
            player.setGameMode(mode);
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§cUngültiger Spielmodus.");
        }
        return true;
    }
}
