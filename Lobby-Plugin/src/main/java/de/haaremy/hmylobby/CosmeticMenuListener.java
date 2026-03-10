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

import java.util.List;
import java.util.Map;

public class CosmeticMenuListener implements Listener {

    private final HmyLobby plugin;

    // Custom head textures (Base64) – können in hmySettings/heads.yml angepasst werden
    private static final String[][] HEADS = {
        {"§aGras-Block",   "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2VkMWFlOWFhNDQ4YTJlNWRiMGI1YTk5NGEzYzA4ZTNjOTI2MGQ4NmYzNDlkODgwYWVmYTdiYWMxOGRiYTIifX19"},
        {"§6Krone",        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjk3ZThlNzM0MjdjYzkxNWI2NWIyNzFlOTUyMzNkNzE1MTk4OTc2NmFhNzUzNGQ2MjEzZTNjMTFkOGE5OCJ9fX0="},
        {"§cTNT",          "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTgwOGZhMmY2MjIyMmVkMzM3NmY1YWRhOTA5YjAyMDY1MDQyZmVlYWRhOWEzMjM3NTU0ODg3ZDZjMDEwIn19fQ=="},
        {"§9Diamant-Block","eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzM1NTgzOGJkZjk5ZWQxOGIzMzU0YWFhMjZmNjc0ZDg0YTVlMWE0NzEwZGZhNjZiNjJjYmYxNTE3MWU4In19fQ=="},
        {"§eKürbis",       "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTk2NzIwNzM5ZTA4MDYzMmVmZjk2MjFlZGJiMjlmN2Y4MTZhYTVmMTE0ZTdlYzQ3NWM1M2M4MzYxMDcwIn19fQ=="},
        {"§bEis-Block",    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2ZkMzc5MTA1NjEwYzViMGVmZjhiMDJiOTk5NjBjZjNmM2QwYTY5YTZhNWI5OWQ2MzEyOTkxOTZkOTAifX19"},
    };

    public CosmeticMenuListener(HmyLobby plugin) {
        this.plugin = plugin;
    }

    // ── Particle Menu ─────────────────────────────────────────────────────────

    public void openParticleMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("§8» §e§lPartikel"));
        plugin.getPlayerEventListener().fillGlass(inv);

        inv.setItem(10, createGuiItem(Material.BLAZE_POWDER,    "§eFlammen",    "hmy.lobby.particle.fire"));
        inv.setItem(11, createGuiItem(Material.WATER_BUCKET,    "§bWasser",     "hmy.lobby.particle.water"));
        inv.setItem(12, createGuiItem(Material.TOTEM_OF_UNDYING,"§aHappy",      "hmy.lobby.particle.happy"));
        inv.setItem(13, createGuiItem(Material.NOTE_BLOCK,      "§dNoten",      "hmy.lobby.particle.note"));
        inv.setItem(14, createGuiItem(Material.PINK_DYE,        "§cHerzen",     "hmy.lobby.particle.heart"));
        inv.setItem(15, createGuiItem(Material.WHITE_WOOL,      "§fWolken",     "hmy.lobby.particle.cloud"));
        inv.setItem(16, createGuiItem(Material.WITCH_SPAWN_EGG, "§5Hexerei",    "hmy.lobby.particle.witch"));
        inv.setItem(19, createGuiItem(Material.RED_DYE,         "§cAus",        ""));
        inv.setItem(22, createGuiItem(Material.ARROW,           "§7Zurück",     ""));

        player.openInventory(inv);
    }

    // ── Mount Menu ────────────────────────────────────────────────────────────

    public void openMountMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("§8» §c§lMounts"));
        plugin.getPlayerEventListener().fillGlass(inv);

        inv.setItem(11, createGuiItem(Material.PIG_SPAWN_EGG,    "§dSchwein", "hmy.lobby.mount.pig"));
        inv.setItem(13, createGuiItem(Material.HORSE_SPAWN_EGG,  "§6Pferd",   "hmy.lobby.mount.horse"));
        inv.setItem(15, createGuiItem(Material.SPIDER_SPAWN_EGG, "§8Spinne",  "hmy.lobby.mount.spider"));
        inv.setItem(19, createGuiItem(Material.RED_DYE,          "§cAus",     ""));
        inv.setItem(22, createGuiItem(Material.ARROW,            "§7Zurück",  ""));

        player.openInventory(inv);
    }

    // ── Heads Menu ────────────────────────────────────────────────────────────

    public void openHeadsMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("§8» §a§lKöpfe"));
        plugin.getPlayerEventListener().fillGlass(inv);

        // Slots 10–15 für Köpfe (Zeile 2, Mitte)
        int[] slots = {10, 11, 12, 13, 14, 15};
        for (int i = 0; i < Math.min(HEADS.length, slots.length); i++) {
            String name    = HEADS[i][0];
            String base64  = HEADS[i][1];
            ItemStack head = plugin.getPlayerEventListener().createCustomHead(base64, name);
            head.editMeta(meta -> {
                meta.lore(List.of(
                        Component.text("§7Klicke um aufzusetzen!"),
                        Component.text("§8Benötigt: §ehmy.lobby.head.wear")));
            });
            inv.setItem(slots[i], head);
        }

        inv.setItem(22, createGuiItem(Material.ARROW, "§7Zurück", ""));
        player.openInventory(inv);
    }

    // ── Placeholder Menu ──────────────────────────────────────────────────────

    public void openPlaceholderMenu(Player player, String featureName) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("§8» " + featureName));
        plugin.getPlayerEventListener().fillGlass(inv);

        ItemStack info = plugin.getPlayerEventListener().createItem(Material.BARRIER, "§c§lIn Arbeit...",
                List.of("§7Dieses Feature wird", "§7noch entwickelt."));
        inv.setItem(13, info);
        inv.setItem(22, createGuiItem(Material.ARROW, "§7Zurück", ""));

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
            player.playSound(player, Sound.UI_BUTTON_CLICK, 1f, 1f);
            return;
        }

        // Ausschalten (Red Dye)
        if (item.getType() == Material.RED_DYE) {
            handleReset(player, title);
            return;
        }

        if      (title.contains("Mounts"))  handleMountClick(player, item);
        else if (title.contains("Partikel")) handleParticleClick(player, item);
        else if (title.contains("Köpfe"))   handleHeadClick(player, item, event.getRawSlot());
    }

    // ── Mount logic ───────────────────────────────────────────────────────────

    private void handleMountClick(Player player, ItemStack item) {
        if (!item.hasItemMeta()) return;
        String name = LegacyComponentSerializer.legacySection().serialize(item.getItemMeta().displayName());

        if (!player.hasPermission("hmy.lobby.mount." + mountKey(name))) {
            player.sendMessage(plugin.getLanguageManager().getMessage(player, "no_permission_mount", "§cKeine Rechte für dieses Mount!"));
            return;
        }

        if      (name.contains("Schwein")) spawnMount(player, EntityType.PIG,    "Schwein");
        else if (name.contains("Pferd"))   spawnMount(player, EntityType.HORSE,  "Pferd");
        else if (name.contains("Spinne"))  spawnMount(player, EntityType.SPIDER, "Spinne");

        player.closeInventory();
    }

    private String mountKey(String name) {
        if (name.contains("Schwein")) return "pig";
        if (name.contains("Pferd"))   return "horse";
        if (name.contains("Spinne"))  return "spider";
        return name.toLowerCase().replaceAll("[^a-z]", "");
    }

    // ── Particle logic ────────────────────────────────────────────────────────

    private void handleParticleClick(Player player, ItemStack item) {
        if (!item.hasItemMeta()) return;
        String name = LegacyComponentSerializer.legacySection().serialize(item.getItemMeta().displayName());

        String key = particleKey(name);
        if (!player.hasPermission("hmy.lobby.particle." + key)) {
            player.sendMessage(plugin.getLanguageManager().getMessage(player, "no_permission_particle", "§cKeine Rechte! §8(hmy.lobby.particle.{particle})", Map.of("particle", key)));
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
        player.sendMessage(plugin.getLanguageManager().getMessage(player, "particle_activated", "§aEffekt aktiviert!"));
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
            player.sendMessage(plugin.getLanguageManager().getMessage(player, "no_permission_head", "§cKeine Rechte zum Aufsetzen von Köpfen!"));
            return;
        }
        // Kopf-Slots: 10–15 → Array-Index 0–5
        int[] slots = {10, 11, 12, 13, 14, 15};
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == slot && i < HEADS.length) {
                ItemStack head = plugin.getPlayerEventListener()
                        .createCustomHead(HEADS[i][1], HEADS[i][0]);
                player.getInventory().setHelmet(head);
                player.sendMessage(plugin.getLanguageManager().getMessage(player, "head_equipped", "§aDu trägst jetzt: {head}", Map.of("head", HEADS[i][0])));
                player.playSound(player, Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1f);
                player.closeInventory();
                return;
            }
        }
    }

    // ── Reset ─────────────────────────────────────────────────────────────────

    private void handleReset(Player player, String title) {
        if (title.contains("Partikel")) {
            plugin.getEffectManager().remove(player);
            player.sendMessage(plugin.getLanguageManager().getMessage(player, "particle_disabled", "§cPartikel deaktiviert!"));
        } else if (title.contains("Mounts")) {
            if (player.getVehicle() != null) {
                player.getVehicle().remove();
                player.sendMessage(plugin.getLanguageManager().getMessage(player, "mount_removed", "§cMount entfernt!"));
            } else {
                player.sendMessage(plugin.getLanguageManager().getMessage(player, "no_mount", "§7Du sitzt aktuell auf keinem Mount."));
            }
        } else if (title.contains("Köpfe")) {
            player.getInventory().setHelmet(null);
            player.sendMessage(plugin.getLanguageManager().getMessage(player, "head_removed", "§cKopf entfernt!"));
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
        player.sendMessage(plugin.getLanguageManager().getMessage(player, "mount_riding", "§aDu reitest auf einem §e{mount}§a!", Map.of("mount", name)));
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
        player.sendMessage(plugin.getLanguageManager().getMessage(player, "mount_stabled", "§7Dein Mount wurde in den Stall geschickt."));
        player.playSound(player, Sound.ENTITY_CHICKEN_EGG, 1f, 1f);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private ItemStack createGuiItem(Material material, String name, String permission) {
        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> {
            meta.displayName(Component.text(name));
            if (!permission.isEmpty()) {
                meta.lore(List.of(Component.text("§7Benötigt: §e" + permission)));
            }
        });
        return item;
    }
}
