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

    public PlayerJoinListener(ProxyServer server, String defaultServerName) {
        this.server = server;
        this.defaultServerName = defaultServerName;
    }

    @Subscribe
    public void onPlayerPostLogin(PostLoginEvent event) {
        Player player = event.getPlayer();
        Optional<RegisteredServer> defaultServer = server.getServer(defaultServerName);

        if (defaultServer.isPresent() && player.getCurrentServer().isEmpty()) {
            player.createConnectionRequest(defaultServer.get()).connect();
        } else {
            player.sendMessage(Component.text("Willkommen! Du bist bereits mit einem Server verbunden."));
        }
    }
}
