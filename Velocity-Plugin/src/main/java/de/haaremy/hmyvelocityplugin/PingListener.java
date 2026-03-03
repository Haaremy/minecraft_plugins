package de.haaremy.hmyvelocityplugin;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PingListener {

    private final ProxyServer server;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public PingListener(ProxyServer server) {
        this.server = server;
    }

    @Subscribe
    public void onProxyPing(ProxyPingEvent event) {
        ServerPing ping = event.getPing();
        int online = server.getPlayerCount();

        // 1. FANCY MOTD (mit Gradienten/RGB)
        // MiniMessage nutzt <gradient:farbe1:farbe2>Text</gradient>
        Component line1 = mm.deserialize("<gradient:#ffaa00:#ff5500><bold>MC.HAAREMY.DE</bold></gradient> <dark_gray>| <gray>Dein Netzwerk");
        Component line2 = mm.deserialize("<gray>Status: <green>Online <dark_gray>» <aqua>Komm spiel mit uns!");
        
        Component finalMotd = line1.append(Component.newline()).append(line2);

        // 2. PLAYER HOVER (Was man sieht, wenn man über die Spielerzahl fährt)
        List<ServerPing.SamplePlayer> sample = new ArrayList<>();
        sample.add(new ServerPing.SamplePlayer("§6§lHAAREMY NETZWERK", UUID.randomUUID()));
        sample.add(new ServerPing.SamplePlayer("§7", UUID.randomUUID()));
        sample.add(new ServerPing.SamplePlayer("§eAktuell online: §f" + online, UUID.randomUUID()));
        sample.add(new ServerPing.SamplePlayer("§7", UUID.randomUUID()));
        sample.add(new ServerPing.SamplePlayer("§bKlick zum Beitreten!", UUID.randomUUID()));

        // 3. VERSION TEXT (Anstatt "1.21", steht dort z.B. "Wartungen" oder "1.8-1.21")
        ServerPing.Version version = new ServerPing.Version(ping.getVersion().getProtocol(), "§bUnterstützt 1.21.11");

        event.setPing(ping.asBuilder()
                .description(finalMotd)
                .onlinePlayers(online)
                .maximumPlayers(online + 1) // Dynamischer Slot
                .samplePlayers(sample.toArray(new ServerPing.SamplePlayer[0]))
                .version(version)
                .build());
    }
}