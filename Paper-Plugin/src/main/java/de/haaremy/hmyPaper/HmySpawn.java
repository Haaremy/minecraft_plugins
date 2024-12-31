package de.haaremy.hmypaper;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class HmySpawn implements Listener {

    private final List<String> spawnWorldNames;

    public HmySpawn() {
        // Lade die SpawnWorldNames.yml
        File file = new File("plugins/HmyPaperPlugin/SpawnWorldNames.yml");
        if (!file.exists()) {
            try (InputStream in = getClass().getResourceAsStream("/SpawnWorldNames.yml")) {
                if (in != null) {
                    File pluginFolder = new File("plugins/HmyPaperPlugin");
                    if (!pluginFolder.exists()) pluginFolder.mkdirs();
                    java.nio.file.Files.copy(in, file.toPath());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        this.spawnWorldNames = config.getStringList("worlds");
    }

    @EventHandler
    public void onPlayerJoinEvent(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();

        Location worldSpawn = world.getSpawnLocation(); // Holt den Weltspawn
        if ("lobby".equalsIgnoreCase(world.getName())) { // Sicherer String-Vergleich
            player.teleport(worldSpawn); // Teleportiert den Spieler zum Weltspawn
            player.sendMessage("§eWillkommen auf dem Server! Du wurdest zum Spawn teleportiert.");
        }
    }
}
