package de.haaremy.hmyvelocityplugin;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.nio.file.Path;

import javax.inject.Inject;

import org.slf4j.Logger;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;

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

    private static final MinecraftChannelIdentifier CHANNEL = MinecraftChannelIdentifier.create("hmy", "trigger");

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
            logger.error("Plugin wird eingeschränkt funktionieren.");
            return;
        }

        // Initialisiere Sprach- und Konfigurationsmanager
        
        this.configManager = new HmyConfigManager(logger, dataDirectory);
        configManager.getLang();
        logger.info("Haaremy: Velocity Config geladen.");
        this.languageManager = new HmyLanguageManager(logger, dataDirectory, configManager, luckPerms);
        logger.info("Haaremy: Velocity Sprachen initialisiert.");

        // Sprachdateien laden
        languageManager.loadAllLanguageFiles();
        logger.info("Haaremy: Velocity Sprachen geladen.");

        // Listener und Commands registrieren
        registerListeners();
        initializePluginFeatures();
    }

    private void registerListeners() { // "lobby" ist der default server beim beitreten
        server.getEventManager().register(this, new PlayerJoinListener(server, "lobby", languageManager));
        server.getChannelRegistrar().register(CHANNEL);
        logger.info("Haaremy: Velocity Listeners geladen.");
    }

    private void initializePluginFeatures() {
        // HmyLobby initialisieren
        this.hmyLobby = new HmyLobby(server, logger, languageManager,luckPerms);
        logger.info("Haaremy: Velocity HmyLobby geladen.");

        // Broadcast-Befehl registrieren
        server.getCommandManager().register(
            server.getCommandManager().metaBuilder("broadcast").build(),
            new ComBroadcast(server, languageManager)
        );

         // Broadcast-Befehl registrieren
        server.getCommandManager().register(
            server.getCommandManager().metaBuilder("ban").build(),
            new ComBroadcast(server, languageManager)
        );

        // Sprachen-Befehl registrieren
        server.getCommandManager().register(
            server.getCommandManager().metaBuilder("hmy language").build(),
            new ComHmyLanguage(luckPerms,languageManager)
        );

    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(CHANNEL)) {
            return;
        }

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(event.getData()))) {
            String playerName = in.readUTF();
            String command = in.readUTF();

            logger.info("Nachricht empfangen: Spieler = " + playerName + ", Befehl = " + command);

            server.getPlayer(playerName).ifPresentOrElse(player -> {
                server.getCommandManager().executeAsync(player, command).whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        player.sendMessage(Component.text("Fehler beim Ausführen des Befehls: " + command));
                    }
                });
            }, () -> {
                logger.warn("Spieler nicht gefunden: " + playerName);
            });
        } catch (Exception e) {
            logger.error("Fehler beim Lesen der Plugin-Nachricht: ", e);
        }
    }
}
