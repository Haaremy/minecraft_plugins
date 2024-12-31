package de.haaremy.hmyvelocityplugin;

import java.util.Optional;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import net.kyori.adventure.text.Component;

public class LobbyC implements SimpleCommand {

    private final String connectSuccessMessage;
    private final String connectFailureMessage;
    private final Optional<RegisteredServer> lobbyServer;

    public LobbyC(String connectSuccessMessage, String connectFailureMessage, Optional<RegisteredServer> lobbyServer) {
        this.connectSuccessMessage = connectSuccessMessage;
        this.connectFailureMessage = connectFailureMessage;
        this.lobbyServer = lobbyServer;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();

        if (!(source instanceof Player)) {
            source.sendMessage(Component.text("Dieser Befehl kann nur von Spielern ausgeführt werden."));
            return;
        }

        Player player = (Player) source;

        if (lobbyServer.isPresent()) {
            player.createConnectionRequest(lobbyServer.get()).connect().thenAccept(result -> {
                if (result.isSuccessful()) {
                    player.sendMessage(Component.text(connectSuccessMessage));
                } else {
                    player.sendMessage(Component.text(connectFailureMessage));
                }
            });
        } else {
            player.sendMessage(Component.text("Lobby-Server wurde nicht gefunden."));
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return true; // Berechtigungsprüfung kann hier angepasst werden
    }
}
