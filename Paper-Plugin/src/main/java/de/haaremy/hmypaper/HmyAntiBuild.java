package de.haaremy.hmypaper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import de.haaremy.hmypaper.utils.WorldSettings;
import net.luckperms.api.LuckPerms;

public class HmyAntiBuild implements Listener {
    private final HmyPaperPlugin plugin;
    private List<String> worlds;
    private Map<String, WorldSettings> worldSettings;
    private static final WorldSettings DEFAULT_SETTINGS = new WorldSettings(List.of(), List.of(), List.of());

    public HmyAntiBuild(HmyPaperPlugin plugin, LuckPerms luckperms) {
        this.plugin = plugin;
        loadAntiBuildWorldSettings();
        
    }

private void loadAntiBuildWorldSettings() {
    File configFile = new File(plugin.getDataFolder().getParentFile(), "hmySettings/hmyServer.conf");

    if (!configFile.exists()) {
        plugin.getLogger().warning("Konfigurationsdatei 'hmyServer.conf' wurde nicht gefunden.");
        this.worlds = List.of();
        this.worldSettings = new HashMap<>();
        return;
    }

    try {
        String data = String.join("", Files.readAllLines(configFile.toPath())).trim();

        // Anti-Build-Welten auslesen
        worlds = extractGlobalSection(data, "antibuildworld");
        plugin.getLogger().info("Anti-Build-Welten erfolgreich geladen: " + worlds);

        // Einstellungen pro Welt laden
        worldSettings = new HashMap<>();
       for (String world : worlds) {
    List<String> disabledDamageTypes = extractSection(data, world, "disableddamagetypes");
    List<String> allowedPlace = extractSection(data, world, "allowedplace").stream()
    .map(String::toUpperCase) // Werte in Großbuchstaben umwandeln
    .toList();
    List<String> allowedBreak = extractSection(data, world, "allowedbreak").stream()
    .map(String::toUpperCase) // Werte in Großbuchstaben umwandeln
    .toList();

    WorldSettings settings = new WorldSettings(disabledDamageTypes, allowedPlace, allowedBreak);
    this.worldSettings.put(world, settings);

    plugin.getLogger().info("Einstellungen für Welt " + world + ": " + settings);
}


    } catch (IOException e) {
        plugin.getLogger().severe("Fehler beim Lesen der Konfigurationsdatei: " + e.getMessage());
    }
}

    private List<String> extractSection(String data, String world, String section) {
    try {
        // Dynamisches Regex: Suche nach "<world> = { <section> = { ... }}"
        String patternString = "\\b" + world + "\\s*=\\s*\\{.*?\\b" + section + "\\s*=\\s*\\{([^}]*)}";
        Pattern pattern = Pattern.compile(patternString, Pattern.DOTALL); // DOTALL für mehrzeilige Abschnitte
        Matcher matcher = pattern.matcher(data);
        if (matcher.find()) {
            String value = matcher.group(1).trim(); // Überflüssige Leerzeichen entfernen
            if (!value.isEmpty()) {
                return Arrays.asList(value.replace("\"", "").split("\\s*,\\s*")); // Entferne Leerzeichen um Kommas
            }
        }
        plugin.getLogger().warning("Sektion '" + section + "' in Welt '" + world + "' nicht gefunden oder leer.");
    } catch (Exception e) {
        plugin.getLogger().severe("Fehler beim Parsen der Sektion '" + section + "' in Welt '" + world + "': " + e.getMessage());
    }
    return List.of();
}

private List<String> extractGlobalSection(String data, String section) {
    try {
        Pattern pattern = Pattern.compile("\\b" + section + "\\s*=\\s*\\{([^}]*)}");
        Matcher matcher = pattern.matcher(data);
        if (matcher.find()) {
            String value = matcher.group(1).trim();
            if (!value.isEmpty()) {
                return Arrays.asList(value.replace("\"", "").split("\\s*,\\s*"));
            }
        }
        plugin.getLogger().warning("Globale Sektion '" + section + "' nicht gefunden oder leer.");
    } catch (Exception e) {
        plugin.getLogger().severe("Fehler beim Parsen der globalen Sektion '" + section + "': " + e.getMessage());
    }
    return List.of();
}




@EventHandler
public void onBlockPlace(BlockPlaceEvent event) {
    Player player = event.getPlayer();
    String worldName = event.getBlock().getWorld().getName();

    // 1. Wenn die Welt nicht geschützt ist, ignorieren wir das Event
    if (!worlds.contains(worldName)) return;

    // 2. Wenn der Spieler die Admin-Permission hat, darf er IMMER bauen
    if (player.hasPermission("hmy.world.edit")) return;

    Material block = event.getBlock().getType();
    WorldSettings settings = worldSettings.getOrDefault(worldName, DEFAULT_SETTINGS);

    // 3. Whitelist-Check für normale Spieler
    if (!settings.getAllowedPlace().contains(block.name().toUpperCase())) {
        event.setCancelled(true);
        player.sendMessage("&4Du darfst diesen Block hier nicht platzieren!");
    }
}

@EventHandler
public void onBlockBreak(BlockBreakEvent event) {
    Player player = event.getPlayer();
    String worldName = event.getBlock().getWorld().getName();

    // 1. Welt-Check
    if (!worlds.contains(worldName)) return;

    // 2. Admin-Check (Wichtig: Das fehlte bei dir noch!)
    if (player.hasPermission("hmy.world.edit")) return;

    Material block = event.getBlock().getType();
    WorldSettings settings = this.worldSettings.getOrDefault(worldName, DEFAULT_SETTINGS);

    // 3. Whitelist-Check
    if (!settings.getAllowedBreak().contains(block.name().toUpperCase())) {
        event.setCancelled(true);
        player.sendMessage( "&4Du darfst diesen Block hier nicht abbauen!");
    }
}

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        String worldName = player.getWorld().getName();

        if (!worlds.contains(worldName)) return;

        String disabledDamageType = event.getCause().name().toLowerCase();
        WorldSettings settings = worldSettings.getOrDefault(worldName, DEFAULT_SETTINGS);

        if (settings.getDisabledDamageTypes().contains(disabledDamageType)) {
            event.setCancelled(true);
        }
    }

    private void validateWorldSettings(String world, WorldSettings settings) {
        if (settings.getDisabledDamageTypes().isEmpty()) {
            plugin.getLogger().warning("Keine disableddamagetypes für Welt " + world + " gefunden.");
        }
        if (settings.getAllowedPlace().isEmpty()) {
            plugin.getLogger().warning("Keine allowedplace für Welt " + world + " gefunden.");
        }
        if (settings.getAllowedBreak().isEmpty()) {
            plugin.getLogger().warning("Keine allowedbreak für Welt " + world + " gefunden.");
        }
    }
}
