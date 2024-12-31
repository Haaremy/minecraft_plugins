package de.haaremy.hmyvelocityplugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.slf4j.Logger;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import net.luckperms.api.LuckPerms;

public class HmyLobby {

    private final ProxyServer server;
    private final Logger logger;
    private final Properties language;
    private final LuckPerms luckPerms;
    private final Map<String, SimpleCommand> commands = new HashMap<>();

    public HmyLobby(ProxyServer server, Logger logger, Properties language, LuckPerms luckPerms) {
        this.server = server;
        this.logger = logger;
        this.language = language;
        this.luckPerms = luckPerms;

        registerCommands();
        logger.info("HmyLobby-Modul geladen.");
    }

    private void registerCommands() {
        // Lobby-Befehl mit spezifischen Nachrichten und Lobby-Server
        Optional<RegisteredServer> lobbyServer = server.getServer("lobby");
        if (lobbyServer.isPresent()) {
            commands.put("lobby", new LobbyC(
                    getLocalizedMessage("language.config.connectedToLobby", "Du wurdest zur Lobby verbunden."),
                    getLocalizedMessage("language.config.failed", "Verbindung zur Lobby fehlgeschlagen."),
                    lobbyServer
            ));
        } else {
            logger.warn("Lobby-Server wurde nicht gefunden. Der 'lobby'-Befehl wird nicht registriert.");
        }

        // Allgemeiner Server-Befehl
        commands.put("hmy", new ServerC(server, logger, luckPerms, language));

        // Alle Befehle registrieren
        commands.forEach((name, command) -> server.getCommandManager().register(
            server.getCommandManager().metaBuilder(name).build(), command
        ));
        logger.info("Commands erfolgreich registriert: {}", commands.keySet());
    }

    private String getLocalizedMessage(String key, String defaultMessage) {
        return language.getProperty(key, defaultMessage);
    }
}
