package de.haaremy.hmyantibuild;

import java.net.http.WebSocket.Listener;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class AntiBuildListener implements Listener {
    private final ConfigManager configManager;

    public AntiBuildListener(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("hmy.antibuild.override")) return;

        Material block = event.getBlock().getType();
        String world;
        world = event.getBlock().getWorld().getName();
        if (!configManager.isBlockAllowed(world, block, true)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Du darfst diesen Block hier nicht platzieren!");
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("hmy.antibuild.override")) return;

        Material block = event.getBlock().getType();
        String world = event.getBlock().getWorld().getName();
        if (!configManager.isBlockAllowed(world, block, false)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Du darfst diesen Block hier nicht abbauen!");
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        String world = player.getWorld().getName();
        String damageType = event.getCause().name().toLowerCase();

        if (configManager.isDamageProtected(world, damageType)) {
            event.setCancelled(true);
        }
    }
}
