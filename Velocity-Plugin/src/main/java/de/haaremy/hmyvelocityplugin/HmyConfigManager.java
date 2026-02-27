package de.haaremy.hmyvelocityplugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import org.slf4j.Logger;

public class HmyConfigManager {
    private final Logger logger;
    private final Path dataDirectory;

    public HmyConfigManager(Logger logger, Path dataDirectory) {
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    public void createDefaultConfig() {
        try {
            Path configFile = dataDirectory.resolve("hmyVelocity.conf");
            if (!Files.exists(configFile)) {
                logger.info("Konfigurationsdatei nicht gefunden. Erstelle eine neue...");
                Files.createDirectories(dataDirectory); // Sicherstellen, dass der Ordner existiert

                String defaultConfig = "ServerLanguage = \"de\"\n";

                Files.writeString(configFile, defaultConfig, StandardOpenOption.CREATE);
                logger.info("Konfigurationsdatei 'hmyVelocity.conf' erfolgreich erstellt.");
            }
        } catch (IOException e) {
            logger.error("Fehler beim Erstellen der Konfigurationsdatei: " + e.getMessage(), e);
        }

        
    }

public String getLang() {
    Path configFile = dataDirectory.resolve("hmyVelocity.conf");
    if (!Files.exists(configFile)){
        createDefaultConfig();
        configFile = dataDirectory.resolve("hmyVelocity.conf");
    }
    String defaultLanguage = "de"; // Standardwert

    try {
        // Prüfen, ob die Konfigurationsdatei existiert
        if (Files.exists(configFile)) {
            // Zeilen der Konfigurationsdatei lesen
            List<String> configLines = Files.readAllLines(configFile);
            for (String line : configLines) {
                // Zeile für "ServerLanguage" suchen
                if (line.startsWith("ServerLanguage")) {
                    // Wert extrahieren und zurückgeben
                    return line.split("=")[1].trim().replace("\"", "");
                }
            }
        }
    } catch (IOException e) {
        logger.error("Fehler beim Lesen der Konfigurationsdatei: " + e.getMessage(), e);
    }

    // Fallback auf Standardwert
    return defaultLanguage;
}
}
