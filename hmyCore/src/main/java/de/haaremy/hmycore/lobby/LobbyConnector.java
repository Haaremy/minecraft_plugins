package de.haaremy.hmycore.lobby;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import de.haaremy.hmycore.HmyCore;
import de.haaremy.hmycore.lang.Lang;
import org.bukkit.entity.Player;

public class LobbyConnector {

    private static final String BUNGEECORD_CHANNEL = "BungeeCord";
    private final HmyCore plugin;

    public LobbyConnector(HmyCore plugin) {
        this.plugin = plugin;
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, BUNGEECORD_CHANNEL);
    }

    public void sendToLobby(Player player) {
        sendToServer(player, "lobby");
    }

    public void sendToServer(Player player, String serverName) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(serverName);
        player.sendPluginMessage(plugin, BUNGEECORD_CHANNEL, out.toByteArray());

        Lang.send(player, "core.lobby.connecting", "server", serverName);
    }
}
