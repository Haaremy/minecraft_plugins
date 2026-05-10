package de.haaremy.hmynavigator;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class NavigatorCommand implements CommandExecutor {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private final NavigatorGui gui;
    private final ServerConnector connector;

    public NavigatorCommand(NavigatorGui gui, ServerConnector connector) {
        this.gui = gui;
        this.connector = connector;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MINI.deserialize("<red>Dieser Befehl ist nur für Spieler!</red>"));
            return true;
        }

        if (command.getName().equalsIgnoreCase("navigator")) {
            gui.open(player);
            return true;
        }

        if (command.getName().equalsIgnoreCase("play")) {
            if (args.length < 1) {
                player.sendMessage(MINI.deserialize("<red>Verwendung: /play <servername></red>"));
                return true;
            }

            connector.sendToServerDirect(player, args[0]);
            return true;
        }

        return false;
    }
}
