package de.haaremy.hmypaper;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

/**
 * Lädt Konfigurationsdateien aus minecraftServers/hmySettings/.
 *
 * Pfad-Berechnung (Plugin liegt in minecraftServers/subserver/plugins/):
 *   pluginsDir  = minecraftServers/subserver/plugins/
 *   hmySettings = pluginsDir/../../hmySettings
 *             = minecraftServers/hmySettings/
 */
public class HmyConfigManager {

    private final Logger logger;
    private final Path hmySettingsDir;

    private YamlConfiguration generalConfig;
    private YamlConfiguration helpBookConfig;

    public HmyConfigManager(Logger logger, Path pluginsDir) {
        this.logger = logger;
        this.hmySettingsDir = pluginsDir.getParent().getParent().resolve("hmySettings");
        load();
    }

    private void load() {
        try {
            Files.createDirectories(hmySettingsDir);
        } catch (IOException e) {
            logger.severe("Konnte hmySettings-Verzeichnis nicht erstellen: " + e.getMessage());
        }
        generalConfig  = loadOrCreate("general.yml");
        helpBookConfig = loadOrCreate("helpBook.yml");
    }

    /**
     * Lädt eine YAML-Datei aus hmySettings/. Existiert sie noch nicht,
     * wird die mitgelieferte Standarddatei aus dem JAR kopiert.
     */
    private YamlConfiguration loadOrCreate(String filename) {
        Path file = hmySettingsDir.resolve(filename);
        if (!Files.exists(file)) {
            try (InputStream in = getClass().getResourceAsStream("/" + filename)) {
                if (in != null) {
                    Files.copy(in, file);
                    logger.info("Standard-Config erstellt: hmySettings/" + filename);
                } else {
                    logger.warning("Keine eingebettete Standard-Config gefunden für: " + filename);
                }
            } catch (IOException e) {
                logger.severe("Fehler beim Erstellen von " + filename + ": " + e.getMessage());
            }
        }
        return YamlConfiguration.loadConfiguration(file.toFile());
    }

    // ── general.yml ─────────────────────────────────────────────────────────

    public String getLang() {
        return generalConfig.getString("language", "de");
    }

    // ── helpBook.yml ─────────────────────────────────────────────────────────

    public List<String> getHelpBookPages() {
        return helpBookConfig.getStringList("help-book.pages");
    }

    public String getHelpBookTitle() {
        return helpBookConfig.getString("help-book.title", "§6Server Hilfe");
    }

    public String getHelpBookAuthor() {
        return helpBookConfig.getString("help-book.author", "Haaremy");
    }
}
