package de.haaremy.hmyvelocityplugin;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import de.haaremy.hmyvelocityplugin.friends.FriendManager;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Verfolgt, auf welchem Server sich jeder Spieler befindet.
 * Wird für Friends-Follow-Feature verwendet.
 */
public class PlayerTracker {

    private final ProxyServer server;
    private final FriendManager friendManager;
    private final Map<UUID, String> playerServer = new ConcurrentHashMap<>();

    public PlayerTracker(ProxyServer server, FriendManager friendManager) {
        this.server        = server;
        this.friendManager = friendManager;
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        Player player  = event.getPlayer();
        String newServer = event.getServer().getServerInfo().getName();
        playerServer.put(player.getUniqueId(), newServer);

        // Notify friends that this player changed server
        notifyFriendsOfServerChange(player, newServer);

        // Move followers to same server
        for (UUID followerUUID : friendManager.getFollowers(player.getUniqueId())) {
            server.getPlayer(followerUUID).ifPresent(follower -> {
                if (!follower.getCurrentServer()
                        .map(s -> s.getServerInfo().getName()).orElse("").equals(newServer)) {
                    server.getServer(newServer).ifPresent(s ->
                            follower.createConnectionRequest(s).connectWithIndication());
                    follower.sendMessage(net.kyori.adventure.text.Component.text(
                            "§6Freund §e" + player.getUsername() + " §6ist zu §e" + newServer + " §6gewechselt. Du folgst!"));
                }
            });
        }
    }

    private void notifyFriendsOfServerChange(Player player, String newServer) {
        for (UUID friendUUID : friendManager.getFriends(player.getUniqueId())) {
            server.getPlayer(friendUUID).ifPresent(friend ->
                    friend.sendMessage(net.kyori.adventure.text.Component.text(
                            "§8[§6Freunde§8] §e" + player.getUsername()
                                    + " §7ist jetzt auf §a" + newServer + "§7.")));
        }
    }

    public Optional<String> getPlayerServer(UUID uuid) {
        String s = playerServer.get(uuid);
        if (s != null) return Optional.of(s);
        // Fallback: check Velocity directly
        return server.getPlayer(uuid)
                .flatMap(p -> p.getCurrentServer().map(cs -> cs.getServerInfo().getName()));
    }

    public void remove(UUID uuid) {
        playerServer.remove(uuid);
    }
}
