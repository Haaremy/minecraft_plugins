package de.haaremy.hmyvelocityplugin;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import java.util.Optional;

public class PlayerJoinListener {

    private final ProxyServer server;
    private final String defaultServerName;

    public PlayerJoinListener(ProxyServer server, String defaultServerName, HmyLanguageManager languageManager) {
        this.server = server;
        this.defaultServerName = defaultServerName;
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        // Nur beim ersten Join (kein vorheriger Server)
        if (event.getPreviousServer().isPresent()) return;

        Player player = event.getPlayer();
        String connectedServer = event.getServer().getServerInfo().getName();

        // Bereits auf dem Zielserver → nichts tun
        if (connectedServer.equalsIgnoreCase(defaultServerName)) return;

        Optional<RegisteredServer> defaultServer = server.getServer(defaultServerName);
        defaultServer.ifPresent(target ->
            player.createConnectionRequest(target).fireAndForget()
        );
    }
}
