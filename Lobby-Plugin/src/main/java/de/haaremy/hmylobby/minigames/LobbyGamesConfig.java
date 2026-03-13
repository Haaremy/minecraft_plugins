package de.haaremy.hmylobby.minigames;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class LobbyGamesConfig {

    public record TicTacToeField(String fieldId, String name, Location corner1, Location corner2) {}

    private final Path file;
    private YamlConfiguration config;
    private final Logger logger;

    public LobbyGamesConfig(Path hmySettingsDir, Logger logger) {
        this.file   = hmySettingsDir.resolve("lobbygames.yml");
        this.logger = logger;
        load();
    }

    public void load() {
        if (!Files.exists(file)) {
            config = new YamlConfiguration();
        } else {
            config = YamlConfiguration.loadConfiguration(file.toFile());
        }
    }

    public void saveField(String gameType, String fieldId, String name, Location c1, Location c2) {
        String path = gameType + ".fields." + fieldId;
        config.set(path + ".name",      name);
        config.set(path + ".world",     c1.getWorld().getName());
        config.set(path + ".corner1.x", c1.getBlockX());
        config.set(path + ".corner1.y", c1.getBlockY());
        config.set(path + ".corner1.z", c1.getBlockZ());
        config.set(path + ".corner2.x", c2.getBlockX());
        config.set(path + ".corner2.y", c2.getBlockY());
        config.set(path + ".corner2.z", c2.getBlockZ());
        try {
            config.save(file.toFile());
        } catch (IOException e) {
            logger.severe("Fehler beim Speichern von lobbygames.yml: " + e.getMessage());
        }
    }

    public List<TicTacToeField> getTicTacToeFields() {
        List<TicTacToeField> result = new ArrayList<>();
        ConfigurationSection section = config.getConfigurationSection("tiktaktoe.fields");
        if (section == null) return result;

        for (String fieldId : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(fieldId);
            if (entry == null) continue;

            String name  = entry.getString("name", fieldId);
            String wName = entry.getString("world", "lobby");
            World  world = Bukkit.getWorld(wName);
            if (world == null) {
                logger.warning("Welt '" + wName + "' für TicTacToe-Feld '" + fieldId + "' nicht geladen.");
                continue;
            }

            Location c1 = new Location(world,
                    entry.getInt("corner1.x"), entry.getInt("corner1.y"), entry.getInt("corner1.z"));
            Location c2 = new Location(world,
                    entry.getInt("corner2.x"), entry.getInt("corner2.y"), entry.getInt("corner2.z"));

            result.add(new TicTacToeField(fieldId, name, c1, c2));
        }
        return result;
    }
}
