package de.haaremy.hmyvelocityplugin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import javax.inject.Inject;

import org.slf4j.Logger;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;

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

    @Inject
    public HmyVelocityPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
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
        logger.info("HmyLobby erfolgreich initialisiert.");
    }
}
