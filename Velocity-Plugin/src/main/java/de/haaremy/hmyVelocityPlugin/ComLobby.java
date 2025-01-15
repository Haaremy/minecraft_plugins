package de.haaremy.hmyvelocityplugin;

import java.util.Optional;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import net.kyori.adventure.text.Component;

public class ComLobby implements SimpleCommand {

    private final Optional<RegisteredServer> lobbyServer;
    private final HmyLanguageManager language;


    public ComLobby( Optional<RegisteredServer> lobbyServer, HmyLanguageManager language) {
        this.lobbyServer = lobbyServer;
        this.language = language;

    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();

        if (!(source instanceof Player)) {
            source.sendMessage(Component.text(language.getMessage("l_player_only", "Dieser Befehl kann nur von Spielern ausgeführt werden.")));
            return;
        }

        Player player = (Player) source;

        if (lobbyServer.isPresent()) {
            player.createConnectionRequest(lobbyServer.get()).connect().thenAccept(result -> {
                if (result.isSuccessful()) {
                } else {
                    player.sendMessage(Component.text(language.getMessage(player, "p_lobby_connect_failed","Verbindung zur Lobby fehlgeschlagen.")));
                }
            });
        } else {
            player.sendMessage(Component.text(language.getMessage(player, "p_lobby_connect_failed","Verbindung zur Lobby fehlgeschlagen.")));
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return true; // Berechtigungsprüfung kann hier angepasst werden
    }
}
