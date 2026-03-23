package de.haaremy.hmykitsunesegen;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /agb accept – Spieler stimmt den AGB zu und wird in den normalen Hub-Zustand versetzt.
 */
public class ComAgb implements CommandExecutor {

    private final HmyKitsuneSegen plugin;

    public ComAgb(HmyKitsuneSegen plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Nur Spieler können diesen Befehl nutzen.");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("accept")) {
            if (plugin.getAgbManager().hasAccepted(player.getUniqueId())) {
                player.sendMessage(ChatColor.YELLOW + "Du hast die AGB bereits akzeptiert.");
                return true;
            }

            plugin.getAgbManager().accept(player.getUniqueId());
            player.closeInventory();
            player.getInventory().clear();

            player.sendMessage(ChatColor.GREEN + "✔ Du hast die AGB akzeptiert. Willkommen auf mc.haaremy.de!");
            player.sendTitle(
                    ChatColor.GREEN + "Willkommen!",
                    ChatColor.GRAY + "Viel Spaß auf mc.haaremy.de",
                    10, 60, 20
            );

            // Jetzt den normalen Join-Ablauf ausführen
            plugin.getGameManager().onPlayerJoin(player);
            plugin.getHubListener().refreshBar();
            return true;
        }

        player.sendMessage(ChatColor.RED + "Verwendung: /agb accept");
        return true;
    }
}
