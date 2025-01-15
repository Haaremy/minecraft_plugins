package de.haaremy.hmypaper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

public class HmyConfigManager {
    private final Logger logger;
    private final Path dataDirectory;

    public HmyConfigManager(Logger logger, Path dataDirectory) {
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        createDefaultConfig();
    }


    public void createDefaultConfig() {
        try {
            Path targetPath = dataDirectory.resolve("./hmySettings/hmyServer.conf").normalize();

            // Prüfen, ob die Datei bereits existiert
                try (InputStream inputStream = getClass().getResourceAsStream("/hmyServer.conf")) {
                    if (inputStream == null) {
                        throw new IOException("Resource not found: hmyServer.conf");
                    }

                    // Zielverzeichnis erstellen, falls nicht vorhanden
                    Files.createDirectories(targetPath.getParent());

                    // Datei kopieren
                    Files.copy(inputStream, targetPath);
                    logger.info("Config File Updated.");
                } catch (IOException e) {
                    logger.severe("Error on copy: " + e.getMessage());
                }
        
    } catch (Exception e) {
        logger.severe("Error on creating files: " + e.getMessage());
    }

        
    }

public String getLang() {
    Path configFile = dataDirectory.resolve("hmyServer.conf");
    if (!Files.exists(configFile)){
        createDefaultConfig();
        configFile = dataDirectory.resolve("hmyServer.conf");
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
        logger.severe("Fehler beim Lesen der Konfigurationsdatei: " + e.getMessage());
    }

    // Fallback auf Standardwert
    return defaultLanguage;
}
}
