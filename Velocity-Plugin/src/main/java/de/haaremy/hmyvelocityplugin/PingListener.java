package de.haaremy.hmyvelocityplugin;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PingListener {

    private final ProxyServer server;
    private final MiniMessage mm = MiniMessage.miniMessage();

    // Animation frames – cycle every 700 ms based on system clock
    private static final long FRAME_DURATION_MS = 700;
    private static final String[] MOTD_FRAMES = {
        "<gradient:#ffaa00:#ff5500><bold>Server by @Haaremy</bold></gradient>",
        "<gradient:#ff5500:#ff0080><bold>Server by @Haaremy</bold></gradient>",
        "<gradient:#ff0080:#aa00ff><bold>Server by @Haaremy</bold></gradient>",
        "<gradient:#aa00ff:#0088ff><bold>Server by @Haaremy</bold></gradient>",
        "<gradient:#0088ff:#00ffcc><bold>Server by @Haaremy</bold></gradient>",
        "<gradient:#00ffcc:#ffaa00><bold>Server by @Haaremy</bold></gradient>",
    };

    public PingListener(ProxyServer server) {
        this.server = server;
    }

    @Subscribe
    public void onProxyPing(ProxyPingEvent event) {
        ServerPing ping = event.getPing();
        int online = server.getPlayerCount();

        // Animated line 1 – frame based on current time
        int frame = (int) ((System.currentTimeMillis() / FRAME_DURATION_MS) % MOTD_FRAMES.length);
        Component line1 = mm.deserialize(MOTD_FRAMES[frame] + " <dark_gray>| <gray>mc.haaremy.de");

        // Dynamic line 2 – live player count
        Component line2 = mm.deserialize(
                "<gray>✦ <green>" + online + " Spieler online <dark_gray>» <aqua>Komm spiel mit uns!");

        Component finalMotd = line1.append(Component.newline()).append(line2);

        // Hover sample – title + per-server counts + total
        List<ServerPing.SamplePlayer> sample = new ArrayList<>();
        sample.add(new ServerPing.SamplePlayer("§b§lServer by @Haaremy", UUID.randomUUID()));
        sample.add(new ServerPing.SamplePlayer("§7", UUID.randomUUID()));

        server.getAllServers().forEach(s -> {
            int count = s.getPlayersConnected().size();
            if (count > 0) {
                String serverName = s.getServerInfo().getName();
                sample.add(new ServerPing.SamplePlayer(
                        "§e" + serverName + " §8» §a" + count + " Spieler", UUID.randomUUID()));
            }
        });

        sample.add(new ServerPing.SamplePlayer("§7", UUID.randomUUID()));
        sample.add(new ServerPing.SamplePlayer("§7Gesamt online: §f" + online, UUID.randomUUID()));

        // Version string
        ServerPing.Version version = new ServerPing.Version(
                ping.getVersion().getProtocol(), "§b✦ mc.haaremy.de");

        event.setPing(ping.asBuilder()
                .description(finalMotd)
                .onlinePlayers(online)
                .maximumPlayers(online + 1)
                .samplePlayers(sample.toArray(new ServerPing.SamplePlayer[0]))
                .version(version)
                .build());
    }
}
