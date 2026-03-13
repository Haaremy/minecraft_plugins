package de.haaremy.hmylobby;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class CosmeticMenuListener implements Listener {

    private final HmyLobby plugin;

    /** Aktive Tränke-Cosmetics pro Spieler (für "Aus"-Reset). */
    private final Map<UUID, Set<PotionEffectType>> activePotionCosmetics = new HashMap<>();

    // Custom head textures (Base64)
    private static final String[][] HEADS = {
        {"§aGras-Block",    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2VkMWFlOWFhNDQ4YTJlNWRiMGI1YTk5NGEzYzA4ZTNjOTI2MGQ4NmYzNDlkODgwYWVmYTdiYWMxOGRiYTIifX19"},
        {"§6Krone",          "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjk3ZThlNzM0MjdjYzkxNWI2NWIyNzFlOTUyMzNkNzE1MTk4OTc2NmFhNzUzNGQ2MjEzZTNjMTFkOGE5OCJ9fX0="},
        {"§cTNT",            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTgwOGZhMmY2MjIyMmVkMzM3NmY1YWRhOTA5YjAyMDY1MDQyZmVlYWRhOWEzMjM3NTU0ODg3ZDZjMDEwIn19fQ=="},
        {"§9Diamant-Block",  "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzM1NTgzOGJkZjk5ZWQxOGIzMzU0YWFhMjZmNjc0ZDg0YTVlMWE0NzEwZGZhNjZiNjJjYmYxNTE3MWU4In19fQ=="},
        {"§eKürbis",         "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTk2NzIwNzM5ZTA4MDYzMmVmZjk2MjFlZGJiMjlmN2Y4MTZhYTVmMTE0ZTdlYzQ3NWM1M2M4MzYxMDcwIn19fQ=="},
        {"§bEis-Block",      "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2ZkMzc5MTA1NjEwYzViMGVmZjhiMDJiOTk5NjBjZjNmM2QwYTY5YTZhNWI5OWQ2MzEyOTkxOTZkOTAifX19"},
    };

    public CosmeticMenuListener(HmyLobby plugin) {
        this.plugin = plugin;
    }

    // ── Particle Menu ─────────────────────────────────────────────────────────

    public void openParticleMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("§8» §e§lPartikel"));
        plugin.getPlayerEventListener().fillGlass(inv);

        inv.setItem(10, createGuiItem(Material.BLAZE_POWDER,    "§eFlammen",  "hmy.lobby.particle.fire"));
        inv.setItem(11, createGuiItem(Material.WATER_BUCKET,    "§bWasser",   "hmy.lobby.particle.water"));
        inv.setItem(12, createGuiItem(Material.TOTEM_OF_UNDYING,"§aHappy",    "hmy.lobby.particle.happy"));
        inv.setItem(13, createGuiItem(Material.NOTE_BLOCK,      "§dNoten",    "hmy.lobby.particle.note"));
        inv.setItem(14, createGuiItem(Material.PINK_DYE,        "§cHerzen",   "hmy.lobby.particle.heart"));
        inv.setItem(15, createGuiItem(Material.WHITE_WOOL,      "§fWolken",   "hmy.lobby.particle.cloud"));
        inv.setItem(16, createGuiItem(Material.WITCH_SPAWN_EGG, "§5Hexerei",  "hmy.lobby.particle.witch"));

        inv.setItem(19, createGuiItem(Material.RED_DYE,  "§cAus",    ""));
        inv.setItem(22, createGuiItem(Material.ARROW,    "§7Zurück", ""));

        player.openInventory(inv);
    }

    // ── Cosmetics Menu ────────────────────────────────────────────────────────

    public void openCosmeticsMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("§8» §d§lCosmetics"));
        plugin.getPlayerEventListener().fillGlass(inv);

        // Tränke-basierte Cosmetics
        inv.setItem(10, createGuiItem(Material.GLOWSTONE_DUST, "§eGlühen",
                "hmy.lobby.cosmetic.glow",
                List.of("§7Leuchte für andere Spieler.")));
        inv.setItem(11, createGuiItem(Material.ENDER_EYE, "§9Nachtsicht",
                "hmy.lobby.cosmetic.nightvision",
                List.of("§7Sieh im Dunkeln.")));
        inv.setItem(12, createGuiItem(Material.FEATHER, "§7Federfall",
                "hmy.lobby.cosmetic.slowfall",
                List.of("§7Falle langsam.")));
        inv.setItem(13, createGuiItem(Material.SLIME_BALL, "§aFroschsprung",
                "hmy.lobby.cosmetic.jumpboost",
                List.of("§7Springe höher.")));

        // Partikel-basierte Auren
        inv.setItem(14, createGuiItem(Material.BLUE_ICE, "§bEis-Aura",
                "hmy.lobby.cosmetic.iceaura",
                List.of("§7Schneeflocken umgeben dich.")));
        inv.setItem(15, createGuiItem(Material.END_ROD, "§dMagie-Aura",
                "hmy.lobby.cosmetic.magicaura",
                List.of("§7Magische Stäbe schweben um dich.")));

        inv.setItem(19, createGuiItem(Material.RED_DYE, "§cAus",    ""));
        inv.setItem(22, createGuiItem(Material.ARROW,   "§7Zurück", ""));

        player.openInventory(inv);
    }

    // ── Mount Menu ────────────────────────────────────────────────────────────

    public void openMountMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("§8» §c§lMounts"));
        plugin.getPlayerEventListener().fillGlass(inv);

        inv.setItem(10, createGuiItem(Material.PIG_SPAWN_EGG,    "§dSchwein", "hmy.lobby.mount.pig"));
        inv.setItem(11, createGuiItem(Material.HORSE_SPAWN_EGG,  "§6Pferd",   "hmy.lobby.mount.horse"));
        inv.setItem(12, createGuiItem(Material.SPIDER_SPAWN_EGG, "§8Spinne",  "hmy.lobby.mount.spider"));
        inv.setItem(13, createGuiItem(Material.COW_SPAWN_EGG,    "§fKuh",     "hmy.lobby.mount.cow"));
        inv.setItem(14, createGuiItem(Material.LLAMA_SPAWN_EGG,  "§eLama",    "hmy.lobby.mount.llama"));
        inv.setItem(15, createGuiItem(Material.BEE_SPAWN_EGG,    "§6Biene",   "hmy.lobby.mount.bee"));

        inv.setItem(19, createGuiItem(Material.RED_DYE,  "§cAus",    ""));
        inv.setItem(22, createGuiItem(Material.ARROW,    "§7Zurück", ""));

        player.openInventory(inv);
    }

    // ── Heads Menu ────────────────────────────────────────────────────────────

    public void openHeadsMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("§8» §a§lKöpfe"));
        plugin.getPlayerEventListener().fillGlass(inv);

        int[] slots = {10, 11, 12, 13, 14, 15};
        for (int i = 0; i < Math.min(HEADS.length, slots.length); i++) {
            ItemStack head = plugin.getPlayerEventListener().createCustomHead(HEADS[i][1], HEADS[i][0]);
            final int fi = i;
            head.editMeta(meta -> meta.lore(List.of(
                    Component.text("§7Klicke um aufzusetzen!"),
                    Component.text("§8Benoetigt: §ehmy.lobby.head.wear"),
                    Component.text("§8Kopf §7" + (fi + 1) + "/" + HEADS.length))));
            inv.setItem(slots[i], head);
        }

        inv.setItem(19, createGuiItem(Material.RED_DYE, "§cAus",    ""));
        inv.setItem(22, createGuiItem(Material.ARROW,   "§7Zurück", ""));
        player.openInventory(inv);
    }

    // ── Inventory Click Handler ───────────────────────────────────────────────

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = LegacyComponentSerializer.legacySection().serialize(event.getView().title());

        boolean isCosmeticMenu = title.contains("Mounts") || title.contains("Partikel")
                || title.contains("Köpfe") || title.contains("Cosmetics");
        if (!isCosmeticMenu) return;

        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return;

        // Zurück-Pfeil
        if (item.getType() == Material.ARROW) {
            plugin.getPlayerEventListener().openHeadMenu(player);
            player.playSound(player, Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
            return;
        }

        // Ausschalten
        if (item.getType() == Material.RED_DYE) {
            handleReset(player, title);
            return;
        }

        if      (title.contains("Mounts"))   handleMountClick(player, item);
        else if (title.contains("Partikel")) handleParticleClick(player, item);
        else if (title.contains("Köpfe"))    handleHeadClick(player, item, event.getRawSlot());
        else if (title.contains("Cosmetics")) handleCosmeticsClick(player, item);
    }

    // ── Mount logic ───────────────────────────────────────────────────────────

    private void handleMountClick(Player player, ItemStack item) {
        if (!item.hasItemMeta()) return;
        String name = LegacyComponentSerializer.legacySection().serialize(item.getItemMeta().displayName());

        if (!player.hasPermission("hmy.lobby.mount." + mountKey(name))) {
            player.sendMessage(plugin.getLanguageManager().getMessage(player,
                    "no_permission_mount", "§cKeine Rechte fuer dieses Mount!"));
            return;
        }

        if      (name.contains("Schwein")) spawnMount(player, EntityType.PIG,    "Schwein");
        else if (name.contains("Pferd"))   spawnMount(player, EntityType.HORSE,  "Pferd");
        else if (name.contains("Spinne"))  spawnMount(player, EntityType.SPIDER, "Spinne");
        else if (name.contains("Kuh"))     spawnMount(player, EntityType.COW,    "Kuh");
        else if (name.contains("Lama"))    spawnMount(player, EntityType.LLAMA,  "Lama");
        else if (name.contains("Biene"))   spawnMount(player, EntityType.BEE,    "Biene");

        player.closeInventory();
    }

    private String mountKey(String name) {
        if (name.contains("Schwein")) return "pig";
        if (name.contains("Pferd"))   return "horse";
        if (name.contains("Spinne"))  return "spider";
        if (name.contains("Kuh"))     return "cow";
        if (name.contains("Lama"))    return "llama";
        if (name.contains("Biene"))   return "bee";
        return name.toLowerCase().replaceAll("[^a-z]", "");
    }

    // ── Particle logic ────────────────────────────────────────────────────────

    private void handleParticleClick(Player player, ItemStack item) {
        if (!item.hasItemMeta()) return;
        String name = LegacyComponentSerializer.legacySection().serialize(item.getItemMeta().displayName());
        String key  = particleKey(name);

        if (!player.hasPermission("hmy.lobby.particle." + key)) {
            player.sendMessage(plugin.getLanguageManager().getMessage(player,
                    "no_permission_particle", "§cKeine Rechte! §8(hmy.lobby.particle.{particle})",
                    Map.of("particle", key)));
            return;
        }

        Particle p = switch (key) {
            case "fire"  -> Particle.FLAME;
            case "water" -> Particle.WATER_SPLASH;
            case "happy" -> Particle.VILLAGER_HAPPY;
            case "note"  -> Particle.NOTE;
            case "heart" -> Particle.HEART;
            case "cloud" -> Particle.CLOUD;
            case "witch" -> Particle.SPELL_WITCH;
            default      -> null;
        };

        if (p != null) plugin.getEffectManager().setParticle(player, p);
        player.sendMessage(plugin.getLanguageManager().getMessage(player,
                "particle_activated", "§aEffekt aktiviert!"));
        player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        player.closeInventory();
    }

    private String particleKey(String name) {
        if (name.contains("Flammen") || name.contains("Flamme")) return "fire";
        if (name.contains("Wasser"))  return "water";
        if (name.contains("Happy"))   return "happy";
        if (name.contains("Noten"))   return "note";
        if (name.contains("Herzen"))  return "heart";
        if (name.contains("Wolken"))  return "cloud";
        if (name.contains("Hexerei")) return "witch";
        return "unknown";
    }

    // ── Head logic ────────────────────────────────────────────────────────────

    private void handleHeadClick(Player player, ItemStack item, int slot) {
        if (!player.hasPermission("hmy.lobby.head.wear")) {
            player.sendMessage(plugin.getLanguageManager().getMessage(player,
                    "no_permission_head", "§cKeine Rechte zum Aufsetzen von Koepfen!"));
            return;
        }
        int[] slots = {10, 11, 12, 13, 14, 15};
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == slot && i < HEADS.length) {
                ItemStack head = plugin.getPlayerEventListener()
                        .createCustomHead(HEADS[i][1], HEADS[i][0]);
                player.getInventory().setHelmet(head);
                player.sendMessage(plugin.getLanguageManager().getMessage(player,
                        "head_equipped", "§aDu traegst jetzt: {head}",
                        Map.of("head", HEADS[i][0])));
                player.playSound(player, Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1f);
                player.closeInventory();
                return;
            }
        }
    }

    // ── Cosmetics logic ───────────────────────────────────────────────────────

    private void handleCosmeticsClick(Player player, ItemStack item) {
        if (!item.hasItemMeta()) return;
        String name = LegacyComponentSerializer.legacySection().serialize(item.getItemMeta().displayName());

        if      (name.contains("Glühen"))      applyCosmeticPotion(player, PotionEffectType.GLOWING,       "glow");
        else if (name.contains("Nachtsicht"))   applyCosmeticPotion(player, PotionEffectType.NIGHT_VISION,  "nightvision");
        else if (name.contains("Federfall"))    applyCosmeticPotion(player, PotionEffectType.SLOW_FALLING,  "slowfall");
        else if (name.contains("Froschsprung")) applyCosmeticPotion(player, PotionEffectType.JUMP, "jumpboost", 4);
        else if (name.contains("Eis-Aura"))     applyCosmeticParticle(player, Particle.SNOWBALL,            "iceaura");
        else if (name.contains("Magie-Aura"))   applyCosmeticParticle(player, Particle.END_ROD,             "magicaura");
        else return;

        player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
        player.closeInventory();
    }

    private void applyCosmeticPotion(Player player, PotionEffectType type, String permKey) {
        applyCosmeticPotion(player, type, permKey, 0);
    }

    private void applyCosmeticPotion(Player player, PotionEffectType type, String permKey, int amplifier) {
        if (!player.hasPermission("hmy.lobby.cosmetic." + permKey)) {
            player.sendMessage(plugin.getLanguageManager().getMessage(player,
                    "no_permission_cosmetic", "§cKeine Rechte fuer diesen Cosmetic!"));
            return;
        }
        player.addPotionEffect(new PotionEffect(type, Integer.MAX_VALUE, amplifier, true, false));
        activePotionCosmetics.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(type);
        player.sendMessage(plugin.getLanguageManager().getMessage(player,
                "cosmetic_activated", "§aCosmetic aktiviert!"));
    }

    private void applyCosmeticParticle(Player player, Particle particle, String permKey) {
        if (!player.hasPermission("hmy.lobby.cosmetic." + permKey)) {
            player.sendMessage(plugin.getLanguageManager().getMessage(player,
                    "no_permission_cosmetic", "§cKeine Rechte fuer diesen Cosmetic!"));
            return;
        }
        plugin.getEffectManager().setParticle(player, particle);
        player.sendMessage(plugin.getLanguageManager().getMessage(player,
                "cosmetic_activated", "§aCosmetic aktiviert!"));
    }

    /** Entfernt alle aktiven Cosmetics (Tränke + Partikel) eines Spielers. */
    public void removeAllCosmetics(Player player) {
        Set<PotionEffectType> effects = activePotionCosmetics.remove(player.getUniqueId());
        if (effects != null) effects.forEach(player::removePotionEffect);
        plugin.getEffectManager().remove(player);
    }

    // ── Reset ─────────────────────────────────────────────────────────────────

    private void handleReset(Player player, String title) {
        if (title.contains("Partikel")) {
            plugin.getEffectManager().remove(player);
            player.sendMessage(plugin.getLanguageManager().getMessage(player,
                    "particle_disabled", "§cPartikel deaktiviert!"));
        } else if (title.contains("Mounts")) {
            if (player.getVehicle() != null) {
                player.getVehicle().remove();
                player.sendMessage(plugin.getLanguageManager().getMessage(player,
                        "mount_removed", "§cMount entfernt!"));
            } else {
                player.sendMessage(plugin.getLanguageManager().getMessage(player,
                        "no_mount", "§7Du sitzt aktuell auf keinem Mount."));
            }
        } else if (title.contains("Köpfe")) {
            player.getInventory().setHelmet(null);
            player.sendMessage(plugin.getLanguageManager().getMessage(player,
                    "head_removed", "§cKopf entfernt!"));
        } else if (title.contains("Cosmetics")) {
            removeAllCosmetics(player);
            player.sendMessage(plugin.getLanguageManager().getMessage(player,
                    "cosmetic_disabled", "§cCosmetics deaktiviert!"));
        }
        player.closeInventory();
        player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 1f);
    }

    // ── Mount spawning / control ──────────────────────────────────────────────

    private void spawnMount(Player player, EntityType type, String name) {
        if (player.getVehicle() != null) player.getVehicle().remove();

        org.bukkit.entity.Entity mount = player.getWorld().spawnEntity(player.getLocation(), type);
        if (mount instanceof org.bukkit.entity.LivingEntity living) {
            living.setAI(true);
            living.setInvulnerable(true);
            living.setCustomNameVisible(false);
            if (living instanceof org.bukkit.entity.Steerable s) s.setSaddle(true);
            if (living instanceof org.bukkit.entity.AbstractHorse h) {
                h.setTamed(true);
                h.getInventory().setSaddle(new ItemStack(Material.SADDLE));
            }
        }
        mount.addPassenger(player);
        player.sendMessage(plugin.getLanguageManager().getMessage(player,
                "mount_riding", "§aDu reitest auf einem §e{mount}§a!",
                Map.of("mount", name)));
        startMountControlTask(player, mount);
    }

    private void startMountControlTask(Player player, org.bukkit.entity.Entity mount) {
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override public void run() {
                if (!mount.isValid() || mount.getPassengers().isEmpty() || !player.isOnline()) {
                    cancel();
                    if (mount.isValid()) mount.remove();
                    return;
                }
                org.bukkit.util.Vector dir = player.getLocation().getDirection().setY(0).normalize();
                double speed = player.isSprinting() ? 0.5 : 0.25;
                org.bukkit.block.Block ahead = mount.getLocation().add(dir.clone().multiply(1.0)).getBlock();
                if (ahead.getType().isSolid() && mount.isOnGround()) {
                    mount.setVelocity(mount.getVelocity().setY(0.5));
                }
                mount.setVelocity(dir.multiply(speed).setY(-0.1));
                mount.setRotation(player.getLocation().getYaw(), 0);
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    @EventHandler
    public void onMountExit(VehicleExitEvent event) {
        if (!(event.getExited() instanceof Player player)) return;
        event.getVehicle().remove();
        player.sendMessage(plugin.getLanguageManager().getMessage(player,
                "mount_stabled", "§7Dein Mount wurde in den Stall geschickt."));
        player.playSound(player, Sound.ENTITY_CHICKEN_EGG, 1f, 1f);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private ItemStack createGuiItem(Material material, String name, String permission) {
        return createGuiItem(material, name, permission, List.of());
    }

    private ItemStack createGuiItem(Material material, String name, String permission, List<String> extraLore) {
        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> {
            meta.displayName(Component.text(name));
            List<Component> lore = new ArrayList<>(extraLore.stream().map(Component::text).toList());
            if (!permission.isEmpty())
                lore.add(Component.text("§8Berechtigung: §e" + permission));
            meta.lore(lore);
        });
        return item;
    }
}
