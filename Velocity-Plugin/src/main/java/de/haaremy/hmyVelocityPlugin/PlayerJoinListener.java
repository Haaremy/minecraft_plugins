package de.haaremy.hmyvelocityplugin;

import org.slf4j.Logger;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;

public class PlayerJoinListener {

    private final Logger logger;

    public PlayerJoinListener( Logger logger) {
        this.logger = logger;
    }

    @Subscribe
    public void onPlayerJoin(PlayerChooseInitialServerEvent event) {
        String playerName = event.getPlayer().getUsername();
        logger.info("Spieler " + playerName + " beigetreten.");
    }
}
