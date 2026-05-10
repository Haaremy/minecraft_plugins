package de.haaremy.hmyspleef;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class SpleefListener implements Listener {

    private final HmySpleef plugin;

    public SpleefListener(HmySpleef plugin) {
        this.plugin = plugin;
    }

    /**
     * Block abbauen: Nur SNOW_BLOCK erlauben fuer Spleef-Spieler.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        SpleefManager manager = plugin.getSpleefManager();

        if (!manager.isInGame(uuid)) return;

        SpleefGame game = manager.getGame(uuid);
        if (game == null) return;

        // Waehrend Countdown: Kein Abbauen
        if (game.getState() != SpleefGame.GameState.RUNNING) {
            event.setCancelled(true);
            return;
        }

        // Nur wenn der Spieler lebt
        if (!game.isAlive(uuid)) {
            event.setCancelled(true);
            return;
        }

        // Nur SNOW_BLOCK erlauben
        if (event.getBlock().getType() != Material.SNOW_BLOCK) {
            event.setCancelled(true);
            return;
        }

        // Block-Drop verhindern (nur Block entfernen)
        event.setDropItems(false);
    }

    /**
     * Block platzieren verhindern fuer Spleef-Spieler.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (plugin.getSpleefManager().isInGame(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    /**
     * Void-Detection: Spieler faellt unter die Arena.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        SpleefManager manager = plugin.getSpleefManager();

        if (!manager.isInGame(uuid)) return;

        SpleefGame game = manager.getGame(uuid);
        if (game == null) return;

        if (game.getState() != SpleefGame.GameState.RUNNING) return;
        if (!game.isAlive(uuid)) return;

        // Void Detection
        if (event.getTo().getY() < game.getArena().getVoidY()) {
            game.onPlayerEliminate(player, false);
        }
    }

    /**
     * Schaden komplett verhindern (kein PvP, kein Fallschaden).
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (plugin.getSpleefManager().isInGame(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    /**
     * Spieler verlaesst den Server.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        SpleefManager manager = plugin.getSpleefManager();

        if (manager.isInQueue(uuid)) {
            manager.leaveQueue(player);
            return;
        }

        if (manager.isInGame(uuid)) {
            SpleefGame game = manager.getGame(uuid);
            if (game != null) {
                game.onPlayerEliminate(player, true);
            }
        }
    }

    /**
     * Hunger deaktivieren.
     */
    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (plugin.getSpleefManager().isInGame(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    /**
     * Item-Drop verhindern.
     */
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (plugin.getSpleefManager().isInGame(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
