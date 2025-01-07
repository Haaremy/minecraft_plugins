package de.haaremy.hmyvelocityplugin;

import java.util.Optional;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import net.kyori.adventure.text.Component;

public class KickC implements SimpleCommand {

    private final ProxyServer server;

    public KickC(ProxyServer server) {
        this.server = server;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length < 2) {
            source.sendMessage(Component.text("Usage: /kick <player> <reason>"));
            return;
        }

        String playerName = args[0];
        String reason = String.join(" ", args).substring(playerName.length() + 1);

        Optional<Player> targetPlayer = server.getPlayer(playerName);

        if (targetPlayer.isPresent()) {
            Player player = targetPlayer.get();
            player.disconnect(Component.text("You have been kicked: " + reason));
            source.sendMessage(Component.text(player.getUsername() + " has been kicked for: " + reason));
        } else {
            source.sendMessage(Component.text("Player not found."));
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("hmy.kick");
    }
}
