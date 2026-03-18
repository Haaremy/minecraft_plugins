package de.haaremy.hmykitsunesegen;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Set;

/**
 * Handles all in-game events in the game world:
 *  – Block break/place protection (whitelist from config)
 *  – Hunger disabled, no natural regen
 *  – Elytra removed on ground contact (flight spawn mode)
 *  – Player death → delegate to GameManager
 *  – Spectator leave/report button interaction
 *  – Item drop/move protection (axe slot 0 locked)
 *  – Mob spawning disabled in game world
 */
public class GameListener implements Listener {

    private final HmyKitsuneSegen plugin;

    public GameListener(HmyKitsuneSegen plugin) {
        this.plugin = plugin;
    }

    // ── Block protection ──────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!isInGame(event.getPlayer())) return;
        Set<Material> breakable = plugin.getGameConfig().getBreakableBlocks();
        if (!breakable.contains(event.getBlock().getType())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!isInGame(event.getPlayer())) return;
        // Only allow placing blocks that are in the breakable list (same whitelist)
        Set<Material> breakable = plugin.getGameConfig().getBreakableBlocks();
        if (!breakable.contains(event.getBlock().getType())) {
            event.setCancelled(true);
        }
    }

    // ── No hunger / no natural regen ──────────────────────────────────────────

    @EventHandler
    public void onHunger(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isInGame(player)) return;
        // Keep food level at max so no damage and no regen
        if (event.getFoodLevel() < 20) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onNaturalRegen(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isInGame(player)) return;
        if (event.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED
                || event.getRegainReason() == EntityRegainHealthEvent.RegainReason.EATING) {
            event.setCancelled(true);
        }
    }

    // ── Elytra removal on ground contact ──────────────────────────────────────

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!isInGame(player)) return;
        if (!plugin.getGameConfig().getSpawnMode().equals("flight")) return;
        if (player.getGameMode() == GameMode.SPECTATOR) return;

        // Check if player is on the ground and has elytra equipped
        ItemStack chestplate = player.getInventory().getChestplate();
        if (chestplate == null || chestplate.getType() != Material.ELYTRA) return;

        if (player.isOnGround()) {
            player.getInventory().setChestplate(null);
            player.setGliding(false);
        }
    }

    // ── Death handling ────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        Player killed = event.getEntity();
        if (!isInGame(killed)) return;

        event.setDeathMessage(null);
        event.getDrops().clear();
        event.setDroppedExp(0);
        event.setKeepInventory(true); // GameManager clears it

        Player killer = killed.getKiller();
        plugin.getGameManager().eliminatePlayer(killed, killer);

        // Trigger respawn screen immediately
        Bukkit.getScheduler().runTaskLater(plugin,
                () -> killed.spawnAt(killed.getWorld().getSpawnLocation()), 1L);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!isInGame(player)) return;
        if (plugin.getGameManager().isSpectator(player)) {
            // Respawn at world spawn; spectator mode was already set by GameManager
            World game = plugin.getServer().getWorld(plugin.getGameConfig().getGameWorld());
            if (game != null) event.setRespawnLocation(game.getSpawnLocation());
        }
    }

    // ── Spectator leave / report buttons ─────────────────────────────────────

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!isInGame(player)) return;
        if (player.getGameMode() != GameMode.SPECTATOR) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (!meta.hasDisplayName()) return;
        String name = meta.getDisplayName();

        if (name.contains("Verlassen")) {
            event.setCancelled(true);
            plugin.getGameManager().sendToLobby(player);
        } else if (name.contains("Report")) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.YELLOW + "Nutze §e/report <Spieler> <Grund> §ezum Melden.");
        }
    }

    // ── Inventory protection ──────────────────────────────────────────────────

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!isInGame(player)) return;
        if (player.getGameMode() == GameMode.SPECTATOR) {
            event.setCancelled(true);
            return;
        }
        // Slot 0 (axe) cannot be moved
        if (event.getSlot() == 0 || event.getRawSlot() == 0) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!isInGame(player)) return;
        // Prevent dropping the axe from slot 0
        if (player.getInventory().getHeldItemSlot() == 0) {
            event.setCancelled(true);
        }
    }

    // ── Mob spawning disabled ─────────────────────────────────────────────────

    @EventHandler
    public void onMobSpawn(org.bukkit.event.entity.CreatureSpawnEvent event) {
        if (event.getSpawnReason() == org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.NATURAL
                || event.getSpawnReason() == org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.CHUNK_GEN) {
            World game = plugin.getServer().getWorld(plugin.getGameConfig().getGameWorld());
            if (game != null && game.equals(event.getLocation().getWorld())) {
                event.setCancelled(true);
            }
        }
    }

    // ── Player name / glow hidden ─────────────────────────────────────────────

    @EventHandler
    public void onJoinGame(PlayerChangedWorldEvent event) {
        Player p = event.getPlayer();
        World game = plugin.getServer().getWorld(plugin.getGameConfig().getGameWorld());
        if (game == null || !game.equals(p.getWorld())) return;

        // Alive players: hide name tags (use scoreboards for that in ScoreboardManager)
        // Spectators: already hidden from alive
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isInGame(Player p) {
        World game = plugin.getServer().getWorld(plugin.getGameConfig().getGameWorld());
        return game != null && game.equals(p.getWorld());
    }
}
