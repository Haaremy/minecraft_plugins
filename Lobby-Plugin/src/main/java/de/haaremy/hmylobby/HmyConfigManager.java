package de.haaremy.hmylobby;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
    private YamlConfiguration lobbyConfig;

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
        generalConfig = loadOrCreate("general.yml");
        lobbyConfig   = loadOrCreate("lobby.yml");
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

    // ── lobby.yml ────────────────────────────────────────────────────────────

    public String getLobbyWorld() {
        return lobbyConfig.getString("lobby.world", "lobby");
    }

    public boolean getLobbyRule(String key, boolean defaultValue) {
        return lobbyConfig.getBoolean("lobby.rules." + key, defaultValue);
    }

    public ConfigurationSection getServerSelectorSection() {
        return lobbyConfig.getConfigurationSection("server-selector.entries");
    }

    // ── Teleport-Punkte ──────────────────────────────────────────────────────

    public record TeleportPoint(String name, String world, double x, double y, double z, float yaw, float pitch) {}

    public List<TeleportPoint> getTeleportPoints() {
        List<TeleportPoint> result = new ArrayList<>();
        ConfigurationSection section = lobbyConfig.getConfigurationSection("teleport-points");
        if (section == null) return result;
        for (String key : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) continue;
            result.add(new TeleportPoint(
                    entry.getString("name", key),
                    entry.getString("world", "lobby"),
                    entry.getDouble("x", 0),
                    entry.getDouble("y", 64),
                    entry.getDouble("z", 0),
                    (float) entry.getDouble("yaw", 0),
                    (float) entry.getDouble("pitch", 0)
            ));
        }
        return result;
    }
}
