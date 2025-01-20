package de.haaremy.hmypaper.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import de.haaremy.hmypaper.HmyLanguageManager;
import de.haaremy.hmypaper.utils.PermissionUtils;

public class ComBroadcast implements CommandExecutor {

    private final HmyLanguageManager language;

    public ComBroadcast(HmyLanguageManager language) {
        this.language = language;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Argumentprüfung
        if (args.length < 2) {
            sender.sendMessage("Usage: /broadcast <proxy|server|world> <message>");
            return false;
        }

        String scope = args[0].toLowerCase();
        String message = String.join(" ", args).substring(scope.length() + 1);

        // Berechtigungsprüfung
        if (sender instanceof Player player) {
            if (!PermissionUtils.hasPermission(player, "hmy.broadcast")) {
                sender.sendMessage(language.getMessage("p_no_permission", "Keine Berechtigung."));
                return true;
            }
        }

        // Broadcast nach Scope
        switch (scope) {
            case "world":
                if (sender instanceof Player player) {
                    player.getWorld().getPlayers().forEach(p -> p.sendMessage("§e[Broadcast] " + message));
                } else {
                    sender.sendMessage("Only players can broadcast to a specific world.");
                }
                break;
        }

        return true;
    }
}
