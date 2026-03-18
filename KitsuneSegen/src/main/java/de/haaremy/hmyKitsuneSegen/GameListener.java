package de.haaremy.hmykitsunesegen;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;
import java.util.Set;

public class GameListener implements Listener {

    private static final String BUILD_PERM  = "hmy.kitsune.build";
    private static final String[] CATEGORIES = {"Multishot", "Speedshot", "Distanceshot", "Precisionshot"};

    private final HmyKitsuneSegen plugin;
    private final Random random = new Random();

    public GameListener(HmyKitsuneSegen plugin) {
        this.plugin = plugin;
    }

    // ── Block protection (game world) ─────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player p = event.getPlayer();
        if (!isInGame(p) && !isInHub(p)) return;
        if (p.hasPermission(BUILD_PERM)) return;
        if (isInGame(p)) {
            if (!plugin.getGameConfig().getBreakableBlocks().contains(event.getBlock().getType())) {
                event.setCancelled(true);
            }
        } else {
            event.setCancelled(true); // Hub: always locked
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player p = event.getPlayer();
        if (!isInGame(p) && !isInHub(p)) return;
        if (p.hasPermission(BUILD_PERM)) return;
        if (isInGame(p)) {
            if (!plugin.getGameConfig().getBreakableBlocks().contains(event.getBlock().getType())) {
                event.setCancelled(true);
            }
        } else {
            event.setCancelled(true);
        }
    }

    // ── No hunger / no natural regen ─────────────────────────────────────────

    @EventHandler
    public void onHunger(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        if (!isInGame(p)) return;
        if (event.getFoodLevel() < 20) event.setCancelled(true);
    }

    @EventHandler
    public void onNaturalRegen(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        if (!isInGame(p)) return;
        if (event.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED
                || event.getRegainReason() == EntityRegainHealthEvent.RegainReason.EATING) {
            event.setCancelled(true);
        }
    }

    // ── Elytra removal on ground contact (flight mode) ────────────────────────

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!isInGame(player) || player.getGameMode() == GameMode.SPECTATOR) return;

        // Remove launch elytra on landing (flight spawn mode)
        if (plugin.getGameConfig().getSpawnMode().equals("flight")) {
            ItemStack chest = player.getInventory().getChestplate();
            if (chest != null && chest.getType() == Material.ELYTRA
                    && !isLegendaryElytra(chest) && player.isOnGround()) {
                player.getInventory().setChestplate(null);
                player.setGliding(false);
            }
        }

        // Legendary boots: maintain speed effect
        ItemStack boots = player.getInventory().getBoots();
        boolean hasLegBoots = boots != null && isLegendaryBoots(boots);
        boolean hasSpeed = player.hasPotionEffect(PotionEffectType.SPEED);
        if (hasLegBoots && !hasSpeed) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0, false, false));
        } else if (!hasLegBoots && hasSpeed) {
            player.removePotionEffect(PotionEffectType.SPEED);
        }
    }

    // ── Arrow despawn on hit ──────────────────────────────────────────────────

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (arrow.getWorld().equals(plugin.getServer().getWorld(plugin.getGameConfig().getGameWorld()))) {
            Bukkit.getScheduler().runTaskLater(plugin, arrow::remove, 1L);
        }
    }

    // ── Arrow routing to inner inventory + rarity comparison on pickup ─────────

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isInGame(player)) return;

        ItemStack stack = event.getItem().getItemStack();

        // ── Arrow: route to slots 9-35 (inner inventory only) ─────────────────
        if (stack.getType() == Material.ARROW) {
            event.setCancelled(true);
            ItemStack toAdd = stack.clone();
            // Try to stack with existing arrows of same type in slots 9-35
            for (int slot = 9; slot < 36; slot++) {
                ItemStack existing = player.getInventory().getItem(slot);
                if (existing == null || existing.getType() != Material.ARROW) continue;
                if (!sameArrowType(existing, toAdd)) continue;
                int space = existing.getMaxStackSize() - existing.getAmount();
                if (space <= 0) continue;
                int take = Math.min(space, toAdd.getAmount());
                existing.setAmount(existing.getAmount() + take);
                toAdd.setAmount(toAdd.getAmount() - take);
                if (toAdd.getAmount() <= 0) {
                    event.getItem().remove();
                    return;
                }
            }
            // Place remainder in first empty slot in 9-35
            for (int slot = 9; slot < 36; slot++) {
                if (player.getInventory().getItem(slot) == null) {
                    player.getInventory().setItem(slot, toAdd);
                    event.getItem().remove();
                    return;
                }
            }
            // Inner inventory full – drop the item
            return;
        }

        // ── Crossbow: replace if better rarity (same category), drop if worse ──
        if (stack.getType() == Material.CROSSBOW && stack.hasItemMeta()) {
            String newCategory = extractCategory(stack);
            if (newCategory == null) return;
            int newRarity = extractRarity(stack);

            for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
                ItemStack existing = player.getInventory().getItem(slot);
                if (existing == null || existing.getType() != Material.CROSSBOW) continue;
                if (!newCategory.equals(extractCategory(existing))) continue;

                int existingRarity = extractRarity(existing);
                if (newRarity > existingRarity) {
                    player.getWorld().dropItemNaturally(player.getLocation(), existing);
                    player.getInventory().setItem(slot, stack.clone());
                    event.getItem().remove();
                    player.sendActionBar(ChatColor.GREEN + "Bessere Waffe: "
                            + stack.getItemMeta().getDisplayName());
                } else {
                    event.setCancelled(true);
                    player.sendActionBar(ChatColor.YELLOW + "Du hast bereits eine bessere " + newCategory + "-Waffe.");
                }
                return;
            }
            // No same-category weapon – allow normal pickup
        }
    }

    // ── Weapon-specific arrows: block loading wrong type ─────────────────────

    @EventHandler
    public void onCrossbowInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (!isInGame(player) || player.getGameMode() == GameMode.SPECTATOR) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.CROSSBOW || !item.hasItemMeta()) return;

        CrossbowMeta meta = (CrossbowMeta) item.getItemMeta();
        if (!meta.getChargedProjectiles().isEmpty()) return; // Already loaded – shooting

        String category = extractCategory(item);
        if (category == null) return;

        // Check if player has at least one matching arrow
        boolean hasMatch = false;
        for (ItemStack inv : player.getInventory().getContents()) {
            if (inv == null || inv.getType() != Material.ARROW) continue;
            if (category.equals(extractArrowCategory(inv))) { hasMatch = true; break; }
        }
        if (!hasMatch) {
            event.setCancelled(true);
            player.sendActionBar(ChatColor.RED + "Keine §e" + category
                    + "§c-Pfeile im Inventar!");
        }
    }

    // ── Damage multiplier + bigger spread (projectile launch) ─────────────────

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player shooter)) return;
        if (!isInGame(shooter)) return;

        ItemStack bow = shooter.getInventory().getItemInMainHand();
        if (bow.getType() != Material.CROSSBOW || !bow.hasItemMeta()) return;

        String category = extractCategory(bow);
        if (category == null) return;

        // Apply spread based on category
        switch (category) {
            case "Multishot" -> {
                // High spread: ±40° per axis
                var v = arrow.getVelocity();
                double spread = 0.4;
                v.setX(v.getX() + (random.nextDouble() - 0.5) * spread);
                v.setY(v.getY() + (random.nextDouble() - 0.5) * spread);
                v.setZ(v.getZ() + (random.nextDouble() - 0.5) * spread);
                arrow.setVelocity(v);
            }
            case "Speedshot" -> {
                // Medium spread + faster velocity
                var v = arrow.getVelocity();
                double spread = 0.2;
                v.setX(v.getX() + (random.nextDouble() - 0.5) * spread);
                v.setZ(v.getZ() + (random.nextDouble() - 0.5) * spread);
                v.multiply(1.4); // faster
                arrow.setVelocity(v);
            }
            case "Distanceshot" -> {
                // Slightly faster, minimal spread
                var v = arrow.getVelocity();
                v.multiply(1.2);
                arrow.setVelocity(v);
            }
            case "Precisionshot" -> {
                // Very fast, no spread
                var v = arrow.getVelocity();
                v.multiply(1.6);
                arrow.setVelocity(v);
            }
        }
    }

    @EventHandler
    public void onArrowDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player shooter)) return;
        if (!isInGame(shooter)) return;

        ItemStack bow = shooter.getInventory().getItemInMainHand();
        if (bow.getType() != Material.CROSSBOW || !bow.hasItemMeta()) return;

        String category = extractCategory(bow);
        int rarity = extractRarity(bow);

        double multiplier = switch (category != null ? category : "") {
            case "Multishot"      -> 0.5  + rarity * 0.15;
            case "Speedshot"      -> 0.9  + rarity * 0.25;
            case "Distanceshot"   -> 1.4  + rarity * 0.40;
            case "Precisionshot"  -> 2.0  + rarity * 0.70;
            default               -> 1.0;
        };
        event.setDamage(event.getDamage() * multiplier);
    }

    // ── Legendary armor effects ───────────────────────────────────────────────

    /** Legendary helmet: absorbs the first projectile hit, then breaks. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onProjectileHitPlayer(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isInGame(player)) return;
        if (!(event.getDamager() instanceof Projectile)) return;

        ItemStack helmet = player.getInventory().getHelmet();
        if (helmet == null || !isLegendaryHelmet(helmet)) return;

        event.setCancelled(true);
        player.getInventory().setHelmet(null);
        player.sendActionBar(ChatColor.GOLD + "§lVorhalte-Helm §ahat den Schuss abgefangen!");
        player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 1.2f);
    }

    /** Legendary elytra: cancels fall damage that would hurt, then breaks. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isInGame(player)) return;
        if (event.getDamage() < 1.0) return;

        ItemStack chest = player.getInventory().getChestplate();
        if (chest == null || !isLegendaryElytra(chest)) return;

        event.setCancelled(true);
        player.getInventory().setChestplate(null);
        player.sendActionBar(ChatColor.GOLD + "§lFallschutz-Elytra §ahat den Aufprall abgefangen!");
        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 0.8f);
    }

    // ── Death handling ────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        Player killed = event.getEntity();
        if (!isInGame(killed)) return;

        event.setDeathMessage(null);
        event.getDrops().clear();
        event.setDroppedExp(0);
        event.setKeepInventory(false); // cleared by eliminatePlayer + re-given on respawn

        Player killer = killed.getKiller();
        plugin.getGameManager().eliminatePlayer(killed, killer);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!isInGame(player) && !plugin.getGameManager().isSpectator(player)) return;
        if (!plugin.getGameManager().isSpectator(player)) return;

        World game = plugin.getServer().getWorld(plugin.getGameConfig().getGameWorld());
        if (game != null) event.setRespawnLocation(game.getSpawnLocation());

        // Re-give spectator items after respawn (1 tick later for inventory sync)
        Bukkit.getScheduler().runTaskLater(plugin,
                () -> plugin.getGameManager().giveSpectatorItems(player), 2L);
    }

    // ── Spectator leave / report buttons ─────────────────────────────────────

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.SPECTATOR) return;
        if (!plugin.getGameManager().isSpectator(player)) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasDisplayName()) return;
        String name = meta.getDisplayName();

        event.setCancelled(true);
        if (name.contains("Verlassen")) {
            plugin.getGameManager().sendToLobby(player);
        } else if (name.contains("Report")) {
            player.sendMessage(ChatColor.YELLOW + "Nutze §e/report <Spieler> <Grund> §ezum Melden.");
        }
    }

    // ── Inventory protection ──────────────────────────────────────────────────

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!isInGame(player)) return;
        if (player.getGameMode() == GameMode.SPECTATOR) { event.setCancelled(true); return; }
        if (event.getSlot() == 0 || event.getRawSlot() == 0) event.setCancelled(true);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!isInGame(player)) return;
        if (player.getInventory().getHeldItemSlot() == 0) event.setCancelled(true);
    }

    // ── Mob spawning disabled ─────────────────────────────────────────────────

    @EventHandler
    public void onMobSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL
                && event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.CHUNK_GEN) return;
        World game = plugin.getServer().getWorld(plugin.getGameConfig().getGameWorld());
        if (game != null && game.equals(event.getLocation().getWorld())) event.setCancelled(true);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isInGame(Player p) {
        World game = plugin.getServer().getWorld(plugin.getGameConfig().getGameWorld());
        return game != null && game.equals(p.getWorld());
    }

    private boolean isInHub(Player p) {
        World hub = plugin.getServer().getWorld(plugin.getGameConfig().getHubWorld());
        return hub != null && hub.equals(p.getWorld());
    }

    public static String extractCategory(ItemStack item) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return null;
        String name = item.getItemMeta().getDisplayName();
        for (String cat : CATEGORIES) { if (name.contains(cat)) return cat; }
        return null;
    }

    public static String extractArrowCategory(ItemStack item) {
        return extractCategory(item); // Same logic – display name contains the category
    }

    /** Returns rarity index 0-4 from the item's colour prefix. */
    public static int extractRarity(ItemStack item) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return 0;
        String name = item.getItemMeta().getDisplayName();
        if (name.contains(ChatColor.GOLD.toString()))       return 4;
        if (name.contains(ChatColor.DARK_PURPLE.toString())) return 3;
        if (name.contains(ChatColor.BLUE.toString()))       return 2;
        if (name.contains(ChatColor.GREEN.toString()))      return 1;
        return 0;
    }

    private boolean sameArrowType(ItemStack a, ItemStack b) {
        String ca = extractArrowCategory(a);
        String cb = extractArrowCategory(b);
        return ca != null && ca.equals(cb);
    }

    private boolean isLegendaryBoots(ItemStack item) {
        if (!item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
                .has(plugin.getLuckItem().KEY_LEG_BOOTS, PersistentDataType.BYTE);
    }

    private boolean isLegendaryHelmet(ItemStack item) {
        if (!item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
                .has(plugin.getLuckItem().KEY_LEG_HELMET, PersistentDataType.BYTE);
    }

    private boolean isLegendaryElytra(ItemStack item) {
        if (!item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
                .has(plugin.getLuckItem().KEY_LEG_ELYTRA, PersistentDataType.BYTE);
    }
}
