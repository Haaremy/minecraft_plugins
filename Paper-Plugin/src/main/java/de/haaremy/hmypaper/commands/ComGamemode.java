package de.haaremy.hmypaper.commands;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class ComGamemode implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("hmy.gm")) {
            sender.sendMessage("§cDu hast keine Berechtigung, diesen Befehl zu verwenden.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage("§cVerwendung: /gm <name|id> [Spieler]");
            return true;
        }

        // Determine target
        Player target;
        if (args.length >= 2) {
            if (!sender.hasPermission("hmy.gm.other")) {
                sender.sendMessage("§cDu hast keine Berechtigung, den Spielmodus anderer Spieler zu ändern.");
                return true;
            }
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cSpieler §e" + args[1] + " §cnicht gefunden oder offline.");
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cAls Konsole muss ein Spieler angegeben werden: /gm <name|id> <Spieler>");
                return true;
            }
            target = (Player) sender;
        }

        GameMode mode;
        try {
            int number = Integer.parseInt(args[0]);
            switch (number) {
                case 0:  mode = GameMode.SURVIVAL;   break;
                case 1:  mode = GameMode.CREATIVE;   break;
                case 2:  mode = GameMode.ADVENTURE;  break;
                case 3:  mode = GameMode.SPECTATOR;  break;
                default:
                    sender.sendMessage("§cUngültige ID. Nutze 0–3 oder den Namen.");
                    return true;
            }
        } catch (NumberFormatException e) {
            try {
                mode = GameMode.valueOf(args[0].toUpperCase());
            } catch (IllegalArgumentException ex) {
                sender.sendMessage("§cUngültiger Spielmodus: §e" + args[0]);
                return true;
            }
        }

        target.setGameMode(mode);
        String modeName = mode.name().charAt(0) + mode.name().substring(1).toLowerCase();
        if (target.equals(sender)) {
            sender.sendMessage("§aSpielm­odus auf §e" + modeName + " §agesetzt.");
        } else {
            sender.sendMessage("§aSpielm­odus von §e" + target.getName() + " §aauf §e" + modeName + " §agesetzt.");
            target.sendMessage("§aDein Spielm­odus wurde auf §e" + modeName + " §agesetzt.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("survival", "creative", "adventure", "spectator", "0", "1", "2", "3");
        }
        if (args.length == 2 && sender.hasPermission("hmy.gm.other")) {
            String prefix = args[1].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(prefix))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
