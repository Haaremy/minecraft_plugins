package de.haaremy.hmypaper;

import de.haaremy.hmypaper.utils.WorldSettings;
import net.luckperms.api.LuckPerms;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HmyAntiBuild implements Listener {

    private final HmyPaperPlugin plugin;
    private List<String> worlds;
    private Map<String, WorldSettings> worldSettings;
    private static final WorldSettings DEFAULT_SETTINGS = new WorldSettings(List.of(), List.of(), List.of());

    public HmyAntiBuild(HmyPaperPlugin plugin, LuckPerms luckPerms) {
        this.plugin = plugin;
        loadSettings(plugin.getConfigManager());
    }

    private void loadSettings(HmyConfigManager config) {
        this.worlds = config.getAntiBuildWorlds();
        this.worldSettings = new HashMap<>();

        for (Map.Entry<String, Map<String, List<String>>> entry : config.getAntiBuildWorldSettings().entrySet()) {
            String world = entry.getKey();
            Map<String, List<String>> s = entry.getValue();

            List<String> allowedPlace = s.getOrDefault("allowed-place", List.of())
                    .stream().map(String::toUpperCase).toList();
            List<String> allowedBreak = s.getOrDefault("allowed-break", List.of())
                    .stream().map(String::toUpperCase).toList();
            List<String> disabledDamage = s.getOrDefault("disabled-damage-types", List.of())
                    .stream().map(String::toUpperCase).toList();

            worldSettings.put(world, new WorldSettings(disabledDamage, allowedPlace, allowedBreak));
        }

        plugin.getLogger().info("AntiBuild: " + worlds.size() + " Welten geladen: " + worlds);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        String world = event.getBlock().getWorld().getName();
        if (!worlds.contains(world)) return;
        if (player.hasPermission("hmy.world.edit")) return;

        WorldSettings settings = worldSettings.getOrDefault(world, DEFAULT_SETTINGS);
        if (!settings.getAllowedPlace().contains(event.getBlock().getType().name())) {
            event.setCancelled(true);
            player.sendMessage(plugin.getLanguageManager().getMessage(player, "no_place", "§cDu darfst diesen Block hier nicht platzieren!"));
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        String world = event.getBlock().getWorld().getName();
        if (!worlds.contains(world)) return;
        if (player.hasPermission("hmy.world.edit")) return;

        WorldSettings settings = worldSettings.getOrDefault(world, DEFAULT_SETTINGS);
        if (!settings.getAllowedBreak().contains(event.getBlock().getType().name())) {
            event.setCancelled(true);
            player.sendMessage(plugin.getLanguageManager().getMessage(player, "no_break", "§cDu darfst diesen Block hier nicht abbauen!"));
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        String world = player.getWorld().getName();
        if (!worlds.contains(world)) return;

        WorldSettings settings = worldSettings.getOrDefault(world, DEFAULT_SETTINGS);
        if (settings.getDisabledDamageTypes().contains(event.getCause().name().toUpperCase())) {
            event.setCancelled(true);
        }
    }
}
