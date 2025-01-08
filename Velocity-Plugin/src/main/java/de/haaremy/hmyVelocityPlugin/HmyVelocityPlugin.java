package de.haaremy.hmyvelocityplugin;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import javax.inject.Inject;

import org.slf4j.Logger;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
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
    authors = {"Haaremy"}
)
public class HmyVelocityPlugin {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private Properties language;
    private LuckPerms luckPerms;
    private HmyLobby hmyLobby;
    private final ProxyServer proxyServer;
    private static final MinecraftChannelIdentifier CHANNEL = MinecraftChannelIdentifier.create("hmy", "trigger");

    @Inject
    public HmyVelocityPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.proxyServer = server;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        try {
            // LuckPerms-Integration
            this.luckPerms = net.luckperms.api.LuckPermsProvider.get();
            logger.info("LuckPerms erfolgreich eingebunden.");
        } catch (IllegalStateException e) {
            logger.error("LuckPerms-Integration fehlgeschlagen: " + e.getMessage());
            logger.error("Plugin wird eingeschränkt funktionieren.");
            return;
        }

         
        // Sprachdatei laden
        this.language = loadLanguageFile();
        if (this.language == null) {
            logger.error("Sprachdatei konnte nicht geladen werden. Plugin wird eingeschränkt funktionieren.");
            return;
        }

        // standard-Server
         String defaultServerName = "lobby"; // Standardservername
        server.getEventManager().register(this, new PlayerJoinListener(server, defaultServerName));

        // Listens to Commands From Bukkit
        server.getChannelRegistrar().register(CHANNEL);


        // Plugin-Features initialisieren
        initializePluginFeatures();
    }

    private Properties loadLanguageFile() {
        Properties properties = new Properties();
        try {
            Path langFile = dataDirectory.resolve("language.properties");
            if (!Files.exists(langFile)) {
                logger.info("Sprachdatei nicht gefunden, erstelle eine neue...");
                Files.createDirectories(dataDirectory);
                Files.copy(getClass().getResourceAsStream("/language.properties"), langFile);
            }
            properties.load(Files.newInputStream(langFile));
            logger.info("Sprachdatei erfolgreich geladen.");
        } catch (Exception e) {
            logger.error("Fehler beim Laden der Sprachdatei: " + e.getMessage(), e);
        }
        return properties;
    }

    private void initializePluginFeatures() {
        // HmyLobby initialisieren
        this.hmyLobby = new HmyLobby(server, logger, language, luckPerms);

        // Broadcast-Befehl registrieren
        server.getCommandManager().register(
        server.getCommandManager().metaBuilder("broadcast").build(),
        new BroadcastC(server)
        );
        
        logger.info("Velocity: HmyLobby-Modul erfolgreich initialisiert.");

    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(CHANNEL)) {
            return;
        }

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(event.getData()))) {
        // Spielername und Befehl auslesen
        String playerName = in.readUTF();
        String command = in.readUTF();

        logger.info("Nachricht empfangen: Spieler = " + playerName + ", Befehl = " + command);

        // Spieler suchen und Befehl ausführen
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

