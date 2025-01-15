package de.haaremy.hmypaper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class HmySpawn implements Listener {

    private final HmyPaperPlugin plugin;
    private List<String> worlds;

    public HmySpawn(HmyPaperPlugin plugin) {
        this.plugin = plugin;
        loadSpawnWorldNames();
    }

    private void loadSpawnWorldNames() {
    File configFile = new File(plugin.getDataFolder().getParentFile(), "hmySettings/hmyServer.conf");

    if (!configFile.exists()) {
        plugin.getLogger().warning("Konfigurationsdatei 'hmyServer.conf' wurde nicht gefunden.");
        this.worlds = List.of(); // Leere Liste als Fallback
        return;
    }

    try {
        List<String> lines = Files.readAllLines(configFile.toPath());
        StringBuilder content = new StringBuilder();

        // Dateiinhalt in einen String zusammenfügen
        for (String line : lines) {
            content.append(line.trim());
        }

        String data = content.toString();

        // Suche nach der spawn_worlds-Liste und entferne unnötige Zeichen
        if (data.contains("spawn_worlds")) {
            String value = data.split("spawn_worlds = \\[")[1].split("]")[0];
            value = value.replace("\"", "").trim();
            this.worlds = Arrays.asList(value.split(","));
            plugin.getLogger().info("Spawn-Welten erfolgreich geladen: " + worlds);
        } else {
            plugin.getLogger().warning("Keine 'spawn_worlds'-Definition in der Konfigurationsdatei gefunden.");
            this.worlds = List.of();
        }

    } catch (IOException e) {
        plugin.getLogger().severe("Fehler beim Lesen der Konfigurationsdatei: " + e.getMessage());
        this.worlds = List.of(); // Leere Liste als Fallback
    }
}


    @EventHandler
    public void onPlayerJoinEvent(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();

        // Spieler nur teleportieren, wenn die Welt in der Liste ist
        if (worlds.contains(world.getName())) {
            Location worldSpawn = world.getSpawnLocation().add(0.5, 0, 0.5); // Spawn-Koordinaten anpassen
            player.teleport(worldSpawn);
            player.sendMessage("§eWillkommen auf dem Server! Du wurdest zum Weltspawn teleportiert.");
        }
    }
}
