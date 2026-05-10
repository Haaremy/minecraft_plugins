package de.haaremy.hmytntrun;

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

public class TNTRunArena {

    private final HmyTNTRun plugin;
    private final File configFile;
    private FileConfiguration config;

    private final List<ArenaData> arenas = new ArrayList<>();

    public TNTRunArena(HmyTNTRun plugin) {
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

            Location spawn = deserializeLocation(arenaSection.getConfigurationSection("spawn"));
            Location lobby = deserializeLocation(arenaSection.getConfigurationSection("lobby"));

            List<Integer> layers = arenaSection.getIntegerList("layers");
            int voidY = arenaSection.getInt("voidY", -10);

            if (spawn != null && lobby != null && !layers.isEmpty()) {
                arenas.add(new ArenaData(key, spawn, lobby, layers, voidY));
            }
        }

        plugin.getLogger().info(arenas.size() + " TNTRun-Arena(s) geladen.");
    }

    public void saveArenaSpawn(String name, Location spawn) {
        ArenaData existing = getArena(name);
        if (existing != null) {
            // Aktualisiere nur Spawn
            arenas.remove(existing);
            arenas.add(new ArenaData(name, spawn, existing.getLobby(), existing.getLayers(), existing.getVoidY()));
        } else {
            // Neue Arena mit Defaults
            List<Integer> defaultLayers = List.of(100, 90, 80);
            arenas.add(new ArenaData(name, spawn, spawn, defaultLayers, 70));
        }
        saveConfig();
    }

    public void saveArenaLobby(String name, Location lobby) {
        ArenaData existing = getArena(name);
        if (existing != null) {
            arenas.remove(existing);
            arenas.add(new ArenaData(name, existing.getSpawn(), lobby, existing.getLayers(), existing.getVoidY()));
        } else {
            List<Integer> defaultLayers = List.of(100, 90, 80);
            arenas.add(new ArenaData(name, lobby, lobby, defaultLayers, 70));
        }
        saveConfig();
    }

    public void saveArenaLayers(String name, List<Integer> layers, int voidY) {
        ArenaData existing = getArena(name);
        if (existing != null) {
            arenas.remove(existing);
            arenas.add(new ArenaData(name, existing.getSpawn(), existing.getLobby(), layers, voidY));
        }
        saveConfig();
    }

    private void saveConfig() {
        config = new YamlConfiguration();
        for (ArenaData arena : arenas) {
            String path = "arenas." + arena.getName();
            serializeLocation(config.createSection(path + ".spawn"), arena.getSpawn());
            serializeLocation(config.createSection(path + ".lobby"), arena.getLobby());
            config.set(path + ".layers", arena.getLayers());
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
     * Datenklasse fuer eine einzelne TNTRun-Arena.
     */
    public static class ArenaData {
        private final String name;
        private final Location spawn;
        private final Location lobby;
        private final List<Integer> layers; // Y-Positionen der 3 Ebenen
        private final int voidY; // Unter dieser Y-Position wird eliminiert
        private boolean inUse;

        public ArenaData(String name, Location spawn, Location lobby, List<Integer> layers, int voidY) {
            this.name = name;
            this.spawn = spawn;
            this.lobby = lobby;
            this.layers = layers;
            this.voidY = voidY;
            this.inUse = false;
        }

        public String getName() { return name; }
        public Location getSpawn() { return spawn.clone(); }
        public Location getLobby() { return lobby.clone(); }
        public List<Integer> getLayers() { return layers; }
        public int getVoidY() { return voidY; }
        public boolean isInUse() { return inUse; }
        public void setInUse(boolean inUse) { this.inUse = inUse; }
    }
}
