package de.haaremy.hmyantibuild;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class ConfigManager {
    private final FileConfiguration config;
    private final FileConfiguration worldConfig;

    public ConfigManager(JavaPlugin plugin) {
        plugin.saveDefaultConfig();
        config = plugin.getConfig();

        File worldConfigFile = new File(plugin.getDataFolder(), "worlds.yml");
        if (!worldConfigFile.exists()) {
            plugin.saveResource("worlds.yml", false);
        }
        worldConfig = YamlConfiguration.loadConfiguration(worldConfigFile);
    }

    public boolean isBlockAllowed(String world, Material block, boolean isPlace) {
        List<String> allowedBlocks = worldConfig.getStringList("worlds." + world + ".allowed-blocks." + (isPlace ? "place" : "destroy"));
        return allowedBlocks.contains(block.name());
    }

    public boolean isDamageProtected(String world, String damageType) {
        return worldConfig.getBoolean("worlds." + world + ".damage-protection." + damageType, false);
    }

    public void saveWorldConfig() {
        try {
            worldConfig.save(new File("worlds.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
