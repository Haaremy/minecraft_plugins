package de.haaremy.hmytntrun;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class TNTRunListener implements Listener {

    private final HmyTNTRun plugin;

    public TNTRunListener(HmyTNTRun plugin) {
        this.plugin = plugin;
    }

    /**
     * Bewegung: Block unter Spieler nach Delay entfernen + Void-Detection.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        TNTRunManager manager = plugin.getTNTRunManager();

        if (!manager.isInGame(uuid)) return;

        TNTRunGame game = manager.getGame(uuid);
        if (game == null) return;

        // Waehrend Countdown Bewegung erlauben, aber keine Block-Entfernung
        if (game.getState() == TNTRunGame.GameState.STARTING) {
            return;
        }

        if (game.getState() != TNTRunGame.GameState.RUNNING) return;
        if (!game.isAlive(uuid)) return;

        // Void Detection
        if (event.getTo().getY() < game.getArena().getVoidY()) {
            game.onPlayerEliminate(player, false);
            return;
        }

        // Nur bei tatsaechlicher Blockposition-Aenderung
        if (!event.hasChangedBlock()) return;

        // Block unter dem Spieler pruefen
        Location playerLoc = event.getTo().clone();
        Block blockUnder = playerLoc.getBlock().getRelative(BlockFace.DOWN);

        // Block auf dem der Spieler steht
        game.onPlayerMove(player, blockUnder);

        // Auch den Block direkt unter den Fuessen pruefen (falls zwischen zwei Bloecken)
        Block blockUnder2 = new Location(playerLoc.getWorld(),
                playerLoc.getBlockX(), playerLoc.getBlockY() - 1, playerLoc.getBlockZ()).getBlock();
        if (!blockUnder2.getLocation().equals(blockUnder.getLocation())) {
            game.onPlayerMove(player, blockUnder2);
        }
    }

    /**
     * Schaden komplett verhindern fuer TNTRun-Spieler.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        UUID uuid = player.getUniqueId();
        if (plugin.getTNTRunManager().isInGame(uuid)) {
            event.setCancelled(true);
        }
    }

    /**
     * Bloecke abbauen verhindern.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        if (plugin.getTNTRunManager().isInGame(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    /**
     * Bloecke platzieren verhindern.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (plugin.getTNTRunManager().isInGame(event.getPlayer().getUniqueId())) {
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
        TNTRunManager manager = plugin.getTNTRunManager();

        if (manager.isInQueue(uuid)) {
            manager.leaveQueue(player);
            return;
        }

        if (manager.isInGame(uuid)) {
            TNTRunGame game = manager.getGame(uuid);
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
        if (plugin.getTNTRunManager().isInGame(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    /**
     * Item-Drop verhindern.
     */
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (plugin.getTNTRunManager().isInGame(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    /**
     * Interaktionen verhindern.
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (plugin.getTNTRunManager().isInGame(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
