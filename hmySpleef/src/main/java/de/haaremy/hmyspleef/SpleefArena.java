package de.haaremy.hmyspleef;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SpleefArena {

    private final HmySpleef plugin;
    private final File configFile;
    private FileConfiguration config;

    private final List<ArenaData> arenas = new ArrayList<>();

    public SpleefArena(HmySpleef plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "arenas.yml");
        loadArenas();
    }

    private void loadArenas() {
        if (!configFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Konnte arenas.yml nicht erstellen: " + e.getMessage());
            }
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        arenas.clear();

        ConfigurationSection section = config.getConfigurationSection("arenas");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection arenaSection = section.getConfigurationSection(key);
            if (arenaSection == null) continue;

            // Spawn-Punkte laden
            List<Location> spawnPoints = new ArrayList<>();
            ConfigurationSection spawnsSection = arenaSection.getConfigurationSection("spawns");
            if (spawnsSection != null) {
                for (String spawnKey : spawnsSection.getKeys(false)) {
                    Location loc = deserializeLocation(spawnsSection.getConfigurationSection(spawnKey));
                    if (loc != null) {
                        spawnPoints.add(loc);
                    }
                }
            }

            int floorY = arenaSection.getInt("floorY", 100);
            int voidY = arenaSection.getInt("voidY", 80);

            if (!spawnPoints.isEmpty()) {
                arenas.add(new ArenaData(key, spawnPoints, floorY, voidY));
            }
        }

        plugin.getLogger().info(arenas.size() + " Spleef-Arena(s) geladen.");
    }

    public void addSpawnPoint(String name, Location spawn) {
        ArenaData existing = getArena(name);
        if (existing != null) {
            existing.getSpawnPoints().add(spawn);
        } else {
            List<Location> spawns = new ArrayList<>();
            spawns.add(spawn);
            arenas.add(new ArenaData(name, spawns, spawn.getBlockY() - 1, spawn.getBlockY() - 20));
        }
        saveConfig();
    }

    public void setFloorY(String name, int floorY, int voidY) {
        ArenaData existing = getArena(name);
        if (existing != null) {
            arenas.remove(existing);
            arenas.add(new ArenaData(name, existing.getSpawnPoints(), floorY, voidY));
        }
        saveConfig();
    }

    private void saveConfig() {
        config = new YamlConfiguration();
        for (ArenaData arena : arenas) {
            String path = "arenas." + arena.getName();

            // Spawn-Punkte speichern
            for (int i = 0; i < arena.getSpawnPoints().size(); i++) {
                serializeLocation(config.createSection(path + ".spawns.spawn" + i), arena.getSpawnPoints().get(i));
            }

            config.set(path + ".floorY", arena.getFloorY());
            config.set(path + ".voidY", arena.getVoidY());
        }
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Konnte arenas.yml nicht speichern: " + e.getMessage());
        }
    }

    private void serializeLocation(ConfigurationSection section, Location loc) {
        section.set("world", loc.getWorld().getName());
        section.set("x", loc.getX());
        section.set("y", loc.getY());
        section.set("z", loc.getZ());
        section.set("yaw", (double) loc.getYaw());
        section.set("pitch", (double) loc.getPitch());
    }

    private Location deserializeLocation(ConfigurationSection section) {
        if (section == null) return null;
        String worldName = section.getString("world");
        if (worldName == null) return null;
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(
                world,
                section.getDouble("x"),
                section.getDouble("y"),
                section.getDouble("z"),
                (float) section.getDouble("yaw"),
                (float) section.getDouble("pitch")
        );
    }

    public ArenaData getFreeArena() {
        for (ArenaData arena : arenas) {
            if (!arena.isInUse()) {
                return arena;
            }
        }
        return null;
    }

    public ArenaData getArena(String name) {
        for (ArenaData arena : arenas) {
            if (arena.getName().equalsIgnoreCase(name)) {
                return arena;
            }
        }
        return null;
    }

    public List<ArenaData> getArenas() {
        return arenas;
    }

    /**
     * Datenklasse fuer eine einzelne Spleef-Arena.
     */
    public static class ArenaData {
        private final String name;
        private final List<Location> spawnPoints;
        private final int floorY;
        private final int voidY;
        private boolean inUse;

        public ArenaData(String name, List<Location> spawnPoints, int floorY, int voidY) {
            this.name = name;
            this.spawnPoints = spawnPoints;
            this.floorY = floorY;
            this.voidY = voidY;
            this.inUse = false;
        }

        public String getName() { return name; }
        public List<Location> getSpawnPoints() { return spawnPoints; }
        public int getFloorY() { return floorY; }
        public int getVoidY() { return voidY; }
        public boolean isInUse() { return inUse; }
        public void setInUse(boolean inUse) { this.inUse = inUse; }

        /**
         * Gibt den Spawn-Punkt fuer einen bestimmten Spieler-Index zurueck.
         */
        public Location getSpawnForPlayer(int index) {
            if (spawnPoints.isEmpty()) return null;
            return spawnPoints.get(index % spawnPoints.size()).clone();
        }
    }
}
