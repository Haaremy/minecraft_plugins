package de.haaremy.hmypaper;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ComBroadcast implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("Usage: /broadcast <proxy|server|world> <message>");
            return false;
        }

        String scope = args[0].toLowerCase();
        String message = String.join(" ", args).substring(scope.length() + 1);

        switch (scope) {
            case "world":
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    player.getWorld().getPlayers().forEach(p -> p.sendMessage("§e[Broadcast] " + message));
                } else {
                    sender.sendMessage("Only players can broadcast to a specific world.");
                }
                break;
            default:
                sender.sendMessage("Invalid scope. Use 'proxy', 'server', or 'world'.");
                return false;
        }
        return true;
    }
}
