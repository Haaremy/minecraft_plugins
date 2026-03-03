package de.haaremy.hmyvelocityplugin;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import org.slf4j.Logger;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import net.kyori.adventure.text.Component;
import net.luckperms.api.LuckPerms;

@Plugin(
    id = "hmyvelocityplugin",
    name = "hmyVelocity",
    version = "1.1",
    authors = {"Haaremy"},
    dependencies = {@Dependency(id = "luckperms")}
)
public class HmyVelocityPlugin {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private LuckPerms luckPerms;
    private HmyLobby hmyLobby;
    private HmyLanguageManager languageManager;
    private HmyConfigManager configManager;

    // Channels
    private static final MinecraftChannelIdentifier TRIGGER_CHANNEL = MinecraftChannelIdentifier.create("hmy", "trigger");
    private static final MinecraftChannelIdentifier STATUS_CHANNEL = MinecraftChannelIdentifier.create("hmy", "status");

    @Inject
    public HmyVelocityPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        // LuckPerms-Integration
        try {
            this.luckPerms = net.luckperms.api.LuckPermsProvider.get();
            logger.info("LuckPerms erfolgreich eingebunden.");
        } catch (IllegalStateException e) {
            logger.error("LuckPerms-Integration fehlgeschlagen: " + e.getMessage());
            return;
        }

        // Manager initialisieren
        this.configManager = new HmyConfigManager(logger, dataDirectory);
        this.languageManager = new HmyLanguageManager(logger, dataDirectory, configManager, luckPerms);
        languageManager.loadAllLanguageFiles();

        // Listener und Commands registrieren
        registerListeners();
        initializePluginFeatures();

        // --- NEU: Status Update Task starten ---
        startStatusUpdateTask();
    }

    private void registerListeners() {
        // Bestehende Listener
        server.getEventManager().register(this, new PlayerJoinListener(server, "lobby", languageManager));
        
        // NEU: Ping/MOTD Listener (für dynamische Slots/MOTD)
        server.getEventManager().register(this, new PingListener(server));

        // Kanäle registrieren
        server.getChannelRegistrar().register(TRIGGER_CHANNEL);
        server.getChannelRegistrar().register(STATUS_CHANNEL);
        
        logger.info("Haaremy: Velocity Listeners & Channels geladen.");
    }

    private void initializePluginFeatures() {
        this.hmyLobby = new HmyLobby(server, logger, languageManager, luckPerms);

        // Commands registrieren
        server.getCommandManager().register(
            server.getCommandManager().metaBuilder("broadcast").build(),
            new ComBroadcast(server, languageManager)
        );

        server.getCommandManager().register(
            server.getCommandManager().metaBuilder("hmy language").build(),
            new ComHmyLanguage(luckPerms, languageManager)
        );
    }

    // --- NEU: Der Task, der die Schilder in der Lobby füttert ---
    private void startStatusUpdateTask() {
        server.getScheduler().buildTask(this, () -> {
            // Wir suchen den Lobby-Server (muss in velocity.toml so heißen)
            server.getServer("lobby").ifPresent(lobby -> {
                // Nur senden, wenn jemand da ist (Plugin Messages brauchen Spieler als Träger)
                if (lobby.getPlayersConnected().isEmpty()) return;

                for (RegisteredServer rs : server.getAllServers()) {
                    String name = rs.getServerInfo().getName();
                    int online = rs.getPlayersConnected().size();
                    int max = 100; // Hier könntest du einen Wert aus deiner Config nehmen

                    ByteArrayDataOutput out = ByteStreams.newDataOutput();
                    out.writeUTF(name);
                    out.writeInt(online);
                    out.writeInt(max);

                    lobby.sendPluginMessage(STATUS_CHANNEL, out.toByteArray());
                }
            });
        }).repeat(5, TimeUnit.SECONDS).schedule();
        
        logger.info("Haaremy: Status-Update-Task (5s) gestartet.");
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(TRIGGER_CHANNEL)) return;

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(event.getData()))) {
            String playerName = in.readUTF();
            String command = in.readUTF();

            server.getPlayer(playerName).ifPresent(player -> {
                server.getCommandManager().executeAsync(player, command);
            });
        } catch (Exception e) {
            logger.error("Fehler beim Lesen der Plugin-Nachricht: ", e);
        }
    }
}