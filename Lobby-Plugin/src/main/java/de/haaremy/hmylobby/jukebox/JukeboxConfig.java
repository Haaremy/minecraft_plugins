package de.haaremy.hmylobby.jukebox;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Logger;

public class JukeboxConfig {

    private final File configFile;
    private final Logger logger;

    public JukeboxConfig(Path hmySettingsDir, Logger logger) {
        this.configFile = hmySettingsDir.resolve("jukeboxes.yml").toFile();
        this.logger = logger;
    }

    public void save(Map<String, JukeboxData> jukeboxes) {
        YamlConfiguration config = new YamlConfiguration();
        for (JukeboxData data : jukeboxes.values()) {
            String base = "jukeboxes." + data.id;
            saveLocation(config, base + ".jukebox", data.jukeboxLoc);
            if (data.chestLoc != null) {
                saveLocation(config, base + ".chest", data.chestLoc);
            }
            if (data.streamUrl != null) {
                config.set(base + ".streamUrl", data.streamUrl);
            }
            config.set(base + ".streamEndless", data.streamEndless);
        }
        try {
            config.save(configFile);
        } catch (IOException e) {
            logger.warning("Haaremy: Konnte jukeboxes.yml nicht speichern: " + e.getMessage());
        }
    }

    public void load(Map<String, JukeboxData> jukeboxes) {
        if (!configFile.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        ConfigurationSection section = config.getConfigurationSection("jukeboxes");
        if (section == null) return;
        for (String id : section.getKeys(false)) {
            Location jukeboxLoc = loadLocation(section, id + ".jukebox");
            if (jukeboxLoc == null) continue;
            JukeboxData data = new JukeboxData(id, jukeboxLoc);
            data.chestLoc   = loadLocation(section, id + ".chest");
            data.streamUrl  = section.getString(id + ".streamUrl", null);
            data.streamEndless = section.getBoolean(id + ".streamEndless", false);
            jukeboxes.put(id, data);
        }
        logger.info("Haaremy: " + jukeboxes.size() + " Jukeboxen geladen.");
    }

    private void saveLocation(YamlConfiguration config, String path, Location loc) {
        config.set(path + ".world", loc.getWorld().getName());
        config.set(path + ".x", loc.getBlockX());
        config.set(path + ".y", loc.getBlockY());
        config.set(path + ".z", loc.getBlockZ());
    }

    private Location loadLocation(ConfigurationSection section, String path) {
        String worldName = section.getString(path + ".world");
        if (worldName == null) return null;
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world,
                section.getInt(path + ".x"),
                section.getInt(path + ".y"),
                section.getInt(path + ".z"));
    }
}
