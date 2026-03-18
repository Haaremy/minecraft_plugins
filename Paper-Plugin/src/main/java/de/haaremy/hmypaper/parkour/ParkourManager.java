package de.haaremy.hmypaper.parkour;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Manages parkour tracks. Data is stored in hmySettings/parkour.yml.
 *
 * Each parkour has:
 *   parkours.<name>.start     → single location
 *   parkours.<name>.goal      → single location
 *   parkours.<name>.checkpoints.<id> → location
 */
public class ParkourManager {

    private final File file;
    private YamlConfiguration config;
    private final Logger logger;

    public ParkourManager(File pluginsDir, Logger logger) {
        File hmySettings = new File(pluginsDir.getParentFile().getParentFile(), "hmySettings");
        hmySettings.mkdirs();
        this.file   = new File(hmySettings, "parkour.yml");
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
            logger.severe("Fehler beim Speichern von parkour.yml: " + e.getMessage());
        }
    }

    // ── Creation / deletion ────────────────────────────────────────────────────

    public void createParkour(String name) {
        if (!config.contains("parkours." + name)) {
            config.set("parkours." + name + ".created", true);
            save();
        }
    }

    public void deleteParkour(String name) {
        config.set("parkours." + name, null);
        save();
    }

    public boolean parkourExists(String name) {
        return config.contains("parkours." + name);
    }

    public Set<String> getParkourNames() {
        ConfigurationSection sec = config.getConfigurationSection("parkours");
        return sec == null ? Set.of() : sec.getKeys(false);
    }

    // ── Block assignment ───────────────────────────────────────────────────────

    public void setStart(String name, Location loc) {
        writeLocation("parkours." + name + ".start", loc);
        save();
    }

    public void setGoal(String name, Location loc) {
        writeLocation("parkours." + name + ".goal", loc);
        save();
    }

    public void setCheckpoint(String name, int id, Location loc) {
        writeLocation("parkours." + name + ".checkpoints." + id, loc);
        save();
    }

    // ── Block lookups ─────────────────────────────────────────────────────────

    /** Returns the parkour name whose start block matches this location, or null. */
    public String getParkourByStart(Location loc) {
        for (String name : getParkourNames()) {
            Location start = readLocation("parkours." + name + ".start");
            if (sameBlock(start, loc)) return name;
        }
        return null;
    }

    /** Returns the parkour name whose goal block matches this location, or null. */
    public String getParkourByGoal(Location loc) {
        for (String name : getParkourNames()) {
            Location goal = readLocation("parkours." + name + ".goal");
            if (sameBlock(goal, loc)) return name;
        }
        return null;
    }

    /** Returns [parkourName, checkpointId] if this location is a checkpoint, else null. */
    public String[] getParkourByCheckpoint(Location loc) {
        for (String name : getParkourNames()) {
            ConfigurationSection cps = config.getConfigurationSection("parkours." + name + ".checkpoints");
            if (cps == null) continue;
            for (String idStr : cps.getKeys(false)) {
                Location cp = readLocation("parkours." + name + ".checkpoints." + idStr);
                if (sameBlock(cp, loc)) return new String[]{name, idStr};
            }
        }
        return null;
    }

    public int getCheckpointCount(String name) {
        ConfigurationSection sec = config.getConfigurationSection("parkours." + name + ".checkpoints");
        return sec == null ? 0 : sec.getKeys(false).size();
    }

    public Location getStart(String name) {
        return readLocation("parkours." + name + ".start");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void writeLocation(String path, Location loc) {
        config.set(path + ".world", loc.getWorld().getName());
        config.set(path + ".x",     loc.getBlockX());
        config.set(path + ".y",     loc.getBlockY());
        config.set(path + ".z",     loc.getBlockZ());
    }

    private Location readLocation(String path) {
        if (!config.contains(path + ".world")) return null;
        String worldName = config.getString(path + ".world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world,
                config.getInt(path + ".x") + 0.5,
                config.getInt(path + ".y"),
                config.getInt(path + ".z") + 0.5);
    }

    private boolean sameBlock(Location a, Location b) {
        if (a == null || b == null) return false;
        return a.getWorld().equals(b.getWorld())
                && a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ();
    }
}
