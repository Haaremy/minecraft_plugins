package de.haaremy.hmy1v1;

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

public class DuelArena {

    private final Hmy1v1 plugin;
    private final File configFile;
    private FileConfiguration config;

    private final List<ArenaData> arenas = new ArrayList<>();

    public DuelArena(Hmy1v1 plugin) {
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

            Location spawn1 = deserializeLocation(arenaSection.getConfigurationSection("spawn1"));
            Location spawn2 = deserializeLocation(arenaSection.getConfigurationSection("spawn2"));

            if (spawn1 != null && spawn2 != null) {
                arenas.add(new ArenaData(key, spawn1, spawn2));
            }
        }

        plugin.getLogger().info(arenas.size() + " Duel-Arena(s) geladen.");
    }

    public void saveArena(String name, Location spawn1, Location spawn2) {
        arenas.removeIf(a -> a.getName().equalsIgnoreCase(name));

        ArenaData arena = new ArenaData(name, spawn1, spawn2);
        arenas.add(arena);

        ConfigurationSection section = config.createSection("arenas." + name);
        serializeLocation(section, "spawn1", spawn1);
        serializeLocation(section, "spawn2", spawn2);

        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Konnte arenas.yml nicht speichern: " + e.getMessage());
        }
    }

    private void serializeLocation(ConfigurationSection parent, String key, Location loc) {
        ConfigurationSection section = parent.createSection(key);
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

    public static class ArenaData {
        private final String name;
        private final Location spawn1;
        private final Location spawn2;
        private boolean inUse;

        public ArenaData(String name, Location spawn1, Location spawn2) {
            this.name = name;
            this.spawn1 = spawn1;
            this.spawn2 = spawn2;
            this.inUse = false;
        }

        public String getName() {
            return name;
        }

        public Location getSpawn1() {
            return spawn1.clone();
        }

        public Location getSpawn2() {
            return spawn2.clone();
        }

        public boolean isInUse() {
            return inUse;
        }

        public void setInUse(boolean inUse) {
            this.inUse = inUse;
        }
    }
}
