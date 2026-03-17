package de.haaremy.hmypaper.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class ComTp implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("hmy.tp")) {
            sender.sendMessage("§cDu hast keine Berechtigung, diesen Befehl zu verwenden.");
            return true;
        }

        // /tp <target>           → teleport self to target
        // /tp <player> <target>  → teleport player to target (hmy.tp.other)
        if (args.length == 1) {
            if (!(sender instanceof Player self)) {
                sender.sendMessage("§cAls Konsole: /tp <Spieler> <Ziel>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§cSpieler §e" + args[0] + " §cnicht gefunden oder offline.");
                return true;
            }
            self.teleport(target.getLocation());
            sender.sendMessage("§aDu wurdest zu §e" + target.getName() + " §ateleportiert.");
            return true;
        }

        if (args.length == 2) {
            if (!sender.hasPermission("hmy.tp.other")) {
                sender.sendMessage("§cDu hast keine Berechtigung, andere Spieler zu teleportieren.");
                return true;
            }
            Player player = Bukkit.getPlayer(args[0]);
            Player target = Bukkit.getPlayer(args[1]);
            if (player == null) {
                sender.sendMessage("§cSpieler §e" + args[0] + " §cnicht gefunden oder offline.");
                return true;
            }
            if (target == null) {
                sender.sendMessage("§cSpieler §e" + args[1] + " §cnicht gefunden oder offline.");
                return true;
            }
            player.teleport(target.getLocation());
            sender.sendMessage("§e" + player.getName() + " §awurde zu §e" + target.getName() + " §ateleportiert.");
            player.sendMessage("§aDu wurdest von §e" + sender.getName() + " §azu §e" + target.getName() + " §ateleportiert.");
            return true;
        }

        sender.sendMessage("§cVerwendung: /tp <Spieler> [Ziel]");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 || (args.length == 2 && sender.hasPermission("hmy.tp.other"))) {
            String prefix = args[args.length - 1].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(prefix))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
