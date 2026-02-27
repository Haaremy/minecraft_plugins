package de.haaremy.hmyvelocityplugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import net.luckperms.api.LuckPerms;

public class HmyLobby {

    private final ProxyServer server;
    private final Logger logger;
    private final HmyLanguageManager language;
    private final Map<String, String> placeholders;

    private final LuckPerms luckPerms;
    private final Map<String, SimpleCommand> commands = new HashMap<>();

    public HmyLobby(ProxyServer server, Logger logger, HmyLanguageManager languageManager, LuckPerms luckPerms) {
        this.server = server;
        this.logger = logger;
        this.language = languageManager;
        this.luckPerms = luckPerms;
        this.placeholders = new HashMap<>();


        registerCommands();
    }

    private void registerCommands() {
        // Lobby-Befehl mit spezifischen Nachrichten und Lobby-Server
        Optional<RegisteredServer> lobbyServer = server.getServer("lobby");
            commands.put("lobby", new ComLobby(
                    lobbyServer, language
            ));

        // Allgemeiner Server-Befehl
        commands.put("hmy", new ComServer(server, logger, luckPerms, language));

        // Alle Befehle registrieren
        commands.forEach((name, command) -> server.getCommandManager().register(
            server.getCommandManager().metaBuilder(name).build(), command
        ));
        logger.info("Commands erfolgreich registriert: {}", commands.keySet());
    }


}
