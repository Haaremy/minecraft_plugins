package de.haaremy.hmylobby;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class ComNavigator implements CommandExecutor, TabCompleter {

    private final HmyLobby plugin;

    public ComNavigator(HmyLobby plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Dieser Befehl kann nur als Spieler genutzt werden.");
            return true;
        }

        String name = command.getName().toLowerCase(Locale.ROOT);
        if (name.equals("navigator")) {
            plugin.openNavigatorMenu(player);
            return true;
        }

        if (name.equals("play")) {
            if (args.length == 0) {
                plugin.openNavigatorMenu(player);
                return true;
            }
            String server = args[0];
            if (!plugin.getServerSelectorConfig().hasServer(server)) {
                player.sendMessage("§cUnbekannter Server: §e" + server);
                return true;
            }
            plugin.connectToServer(player, server);
            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return List.of();
        String prefix = args[0].toLowerCase(Locale.ROOT);
        return plugin.getServerSelectorConfig().getServerNames().stream()
                .filter(server -> server.toLowerCase(Locale.ROOT).startsWith(prefix))
                .collect(Collectors.toList());
    }
}
