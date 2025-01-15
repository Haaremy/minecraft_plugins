package de.haaremy.hmyvelocityplugin;

import java.util.Optional;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import net.kyori.adventure.text.Component;

public class PlayerJoinListener {

    private final ProxyServer server;
    private final String defaultServerName;
    private final HmyLanguageManager languageManager;

    public PlayerJoinListener(ProxyServer server, String defaultServerName, HmyLanguageManager languageManager) {
        this.server = server;
        this.defaultServerName = defaultServerName;
        this.languageManager = languageManager;
    }

    @Subscribe
    public void onPlayerPostLogin(PostLoginEvent event) {
        Player player = event.getPlayer();
        Optional<RegisteredServer> defaultServer = server.getServer(defaultServerName);

        // Verbinde den Spieler mit dem Standardserver, falls keine Verbindung besteht
        if (defaultServer.isPresent() && player.getCurrentServer().isEmpty()) {
            player.createConnectionRequest(defaultServer.get()).connect();
        }

        // Nachricht basierend auf Spieler-Sprache senden
        String welcomeMessage = languageManager.getMessage(player, "welcome_message", "Willkommen auf dem Server!");
        player.sendMessage(Component.text(welcomeMessage));
    }
}
