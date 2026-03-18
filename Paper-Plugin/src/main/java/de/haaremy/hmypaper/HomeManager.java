package de.haaremy.hmypaper;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Logger;

public class HomeManager {

    private final File file;
    private YamlConfiguration config;
    private final Logger logger;

    public HomeManager(File pluginsDir, Logger logger) {
        File hmySettings = new File(pluginsDir.getParentFile().getParentFile(), "hmySettings");
        hmySettings.mkdirs();
        this.file   = new File(hmySettings, "homes.yml");
        this.logger = logger;
        load();
    }

    private void load() {
        config = YamlConfiguration.loadConfiguration(file);
    }

    private void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            logger.severe("Fehler beim Speichern von homes.yml: " + e.getMessage());
        }
    }

    /** Stores a home for a player at slot 1-5. */
    public void setHome(UUID uuid, int slot, Location loc) {
        String path = uuid + "." + slot;
        config.set(path + ".world", loc.getWorld().getName());
        config.set(path + ".x",     loc.getX());
        config.set(path + ".y",     loc.getY());
        config.set(path + ".z",     loc.getZ());
        config.set(path + ".yaw",   loc.getYaw());
        config.set(path + ".pitch", loc.getPitch());
        save();
    }

    /** Returns the home Location for a player at slot 1-5, or null if not set. */
    public Location getHome(UUID uuid, int slot) {
        String path = uuid + "." + slot;
        ConfigurationSection sec = config.getConfigurationSection(path);
        if (sec == null) return null;
        String worldName = sec.getString("world");
        if (worldName == null) return null;
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world,
                sec.getDouble("x"), sec.getDouble("y"), sec.getDouble("z"),
                (float) sec.getDouble("yaw"), (float) sec.getDouble("pitch"));
    }

    public boolean hasHome(UUID uuid, int slot) {
        return config.contains(uuid + "." + slot);
    }
}
