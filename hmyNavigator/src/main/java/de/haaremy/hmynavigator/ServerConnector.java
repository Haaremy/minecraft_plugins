package de.haaremy.hmynavigator;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

public class ServerConnector {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private final HmyNavigator plugin;
    private final NavigatorConfig config;

    public ServerConnector(HmyNavigator plugin, NavigatorConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void sendToServer(Player player, String gameMode) {
        String serverName = config.getServer(gameMode);

        if (serverName == null) {
            player.sendMessage(MINI.deserialize("<red>Unbekannter Spielmodus: " + gameMode + "</red>"));
            return;
        }

        sendBungeeConnect(player, serverName);
        player.sendMessage(MINI.deserialize("<gray>Verbinde mit <gradient:#ff6b35:#ff8c61>" + serverName + "</gradient>...</gray>"));
    }

    public void sendToServerDirect(Player player, String serverName) {
        sendBungeeConnect(player, serverName);
        player.sendMessage(MINI.deserialize("<gray>Verbinde mit <gradient:#ff6b35:#ff8c61>" + serverName + "</gradient>...</gray>"));
    }

    @SuppressWarnings("UnstableApiUsage")
    private void sendBungeeConnect(Player player, String serverName) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(serverName);
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }
}
