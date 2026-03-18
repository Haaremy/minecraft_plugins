package de.haaremy.hmykitsunesegen;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Manages chest spawning on the game map.
 * Chest-spawn-block positions are scanned at startup and stored.
 * On game start, a random subset of those positions becomes actual chests.
 */
public class ChestManager {

    private final HmyKitsuneSegen plugin;

    // location → pre-generated inventory (populated before game starts)
    private final Map<Location, Inventory> chestInventories = new HashMap<>();

    public ChestManager(HmyKitsuneSegen plugin) {
        this.plugin = plugin;
    }

    // ── Spawn chests on the map ────────────────────────────────────────────────

    public void spawnChests(World world) {
        chestInventories.clear();
        List<Location> chestSpots = plugin.getChestSpots();
        Random random = new Random();

        for (Location loc : chestSpots) {
            Block block = world.getBlockAt(loc);
            block.setType(Material.AIR); // Clear marker block first

            if (random.nextDouble() > 0.6) continue; // 60% chance of spawning

            Material chestType;
            Inventory inv;
            if (random.nextDouble() < 0.2) {
                chestType = Material.ENDER_CHEST; // special chest
                inv = plugin.getLuckItem().createSpecialChest(
                        Bukkit.createInventory(null, 27, "KitsuneSegen"));
            } else {
                chestType = Material.CHEST;
                inv = plugin.getLuckItem().createNormalChest(
                        Bukkit.createInventory(null, 27, "KitsuneSegen"));
            }

            block.setType(chestType);
            chestInventories.put(loc.clone(), inv);

            // Particle marker
            world.spawnParticle(Particle.END_ROD, loc.clone().add(0.5, 0.5, 0.5), 10);
        }
    }

    // ── Get inventory at location ──────────────────────────────────────────────

    public Inventory getChestInventory(Location loc) {
        return chestInventories.get(normalise(loc));
    }

    /** Drop all items and remove the chest block. */
    public void openChest(Location loc) {
        Location key = normalise(loc);
        Inventory inv = chestInventories.remove(key);
        if (inv == null) return;

        for (ItemStack item : inv.getContents()) {
            if (item != null) loc.getWorld().dropItemNaturally(loc, item);
        }
        inv.clear();
        loc.getBlock().setType(Material.AIR);
    }

    public boolean hasChest(Location loc) {
        return chestInventories.containsKey(normalise(loc));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Scan a world for blocks of a specific type (used at startup). */
    public static List<Location> findBlocks(World world, Material material, boolean playerOffset) {
        List<Location> found = new ArrayList<>();
        int range = 250;
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight();

        for (int x = -range; x <= range; x++) {
            for (int z = -range; z <= range; z++) {
                for (int y = minY; y < maxY; y++) {
                    if (world.getBlockAt(x, y, z).getType() == material) {
                        double sx = playerOffset ? (x >= 0 ? x + 0.5 : x) : x;
                        double sz = playerOffset ? (z >= 0 ? z + 0.5 : z) : z;
                        found.add(new Location(world, sx, y + 1, sz));
                    }
                }
            }
        }
        return found;
    }

    private Location normalise(Location loc) {
        return new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }
}
