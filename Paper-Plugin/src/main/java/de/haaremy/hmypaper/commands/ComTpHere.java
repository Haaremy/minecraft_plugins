package de.haaremy.hmypaper.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class ComTpHere implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("hmy.tphere")) {
            sender.sendMessage("§cDu hast keine Berechtigung, diesen Befehl zu verwenden.");
            return true;
        }
        if (!(sender instanceof Player self)) {
            sender.sendMessage("§cNur Spieler können diesen Befehl ausführen.");
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage("§cVerwendung: /tphere <Spieler>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage("§cSpieler §e" + args[0] + " §cnicht gefunden oder offline.");
            return true;
        }

        target.teleport(self.getLocation());
        sender.sendMessage("§e" + target.getName() + " §awurde zu dir teleportiert.");
        target.sendMessage("§aDu wurdest von §e" + self.getName() + " §azu sich teleportiert.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
