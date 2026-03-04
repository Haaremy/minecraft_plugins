package de.haaremy.hmylobby;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class CosmeticMenuListener implements Listener {

    private final HmyLobby plugin;

    public CosmeticMenuListener(HmyLobby plugin) {
        this.plugin = plugin;
    }

    // --- MOUNTS MENÜ ---
    public void openMountMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("§8» §c§lMounts"));
        
        // Beispiel: Schwein-Reittier
        inv.setItem(11, createGuiItem(Material.PIG_SPAWN_EGG, "§dSchwein", "hmy.lobby.mount.pig"));
        // Beispiel: Pferd-Reittier
        inv.setItem(13, createGuiItem(Material.HORSE_SPAWN_EGG, "§6Pferd", "hmy.lobby.mount.horse"));
        // Beispiel: Spinne
        inv.setItem(15, createGuiItem(Material.SPIDER_SPAWN_EGG, "§8Spinne", "hmy.lobby.mount.spider"));

        player.openInventory(inv);
    }

    // --- PARTIKEL MENÜ ---
    public void openParticleMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("§8» §e§lPartikel"));
        
        inv.setItem(11, createGuiItem(Material.BLAZE_POWDER, "§eFlammen-Effekt", "hmy.lobby.particle.fire"));
        inv.setItem(13, createGuiItem(Material.WATER_BUCKET, "§bWasser-Effekt", "hmy.lobby.particle.water"));
        inv.setItem(15, createGuiItem(Material.TOTEM_OF_UNDYING, "§aHappy-Effekt", "hmy.lobby.particle.happy"));

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = LegacyComponentSerializer.legacySection().serialize(event.getView().title());

        // Verhindert Editieren in ALLEN Cosmetic-Untermenüs
        if (title.contains("Mounts") || title.contains("Partikel") || title.contains("Köpfe")) {
        	event.setCancelled(true);
            
            ItemStack item = event.getCurrentItem();
            if (item == null || item.getType() == Material.AIR) return;
            
            if (item.getType() == Material.ARROW) {
                // Hier rufen wir die Methode aus dem PlayerEventListener auf 
                // oder öffnen das Hauptmenü direkt neu:
                plugin.getPlayerEventListener().openHeadMenu(player); 
                player.playSound(player, Sound.UI_BUTTON_CLICK, 1f, 1f);
                return;
            }

            // Reset-Logik (Slot 22 ist meistens gut für "Ausschalten")
            if (item.getType() == Material.RED_DYE) {
                handleReset(player, title);
                return;
            }

            if (title.contains("Mounts")) {
                handleMountClick(player, item);
            } else if (title.contains("Partikel")) {
                handleParticleClick(player, item);
            }
        }
    }

    private void handleReset(Player player, String title) {
        if (title.contains("Partikel")) {
            plugin.getEffectManager().remove(player);
            player.sendMessage("§cPartikel deaktiviert!");
        } else if (title.contains("Mounts")) {
            // Korrekte Syntax für das Entfernen des Fahrzeugs
            if (player.getVehicle() != null) {
                player.getVehicle().remove();
                player.sendMessage("§cMount entfernt!");
            } else {
                player.sendMessage("§7Du sitzt aktuell auf keinem Mount.");
            }
        }
        player.closeInventory();
        player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 1f);
    }

    private void handleMountClick(Player player, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return;
        String name = LegacyComponentSerializer.legacySection().serialize(item.getItemMeta().displayName());

        // Permission Check
        String perm = "hmy.lobby.mount." + name.toLowerCase().replace("§", "").substring(1);
        if (!player.hasPermission(perm)) {
            player.sendMessage("§cKeine Rechte!");
            return;
        }

        if (name.contains("Schwein")) spawnMount(player, EntityType.PIG, "Schwein");
        else if (name.contains("Spinne")) spawnMount(player, EntityType.SPIDER, "Spinne");
        else if (name.contains("Pferd")) spawnMount(player, EntityType.HORSE, "Pferd");
        else if (name.contains("Kuh")) spawnMount(player, EntityType.COW, "Kuh");

        player.closeInventory();
    }

 // In CosmeticMenuListener.java (Auszug der handleParticleClick)
    private void handleParticleClick(Player player, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return;
        String name = LegacyComponentSerializer.legacySection().serialize(item.getItemMeta().displayName());

        // Permission dynamisch: hmy.lobby.particle.fire, etc.
        String suffix = name.contains("Flammen") ? "fire" : name.contains("Wasser") ? "water" : "happy";
        String perm = "hmy.lobby.particle." + suffix;

        if (!player.hasPermission(perm)) {
            player.sendMessage("§cKeine Rechte! §7(" + perm + ")");
            return;
        }

        if (name.contains("Flammen")) plugin.getEffectManager().setParticle(player, Particle.FLAME);
        else if (name.contains("Wasser")) plugin.getEffectManager().setParticle(player, Particle.WATER_WAKE);
        else if (name.contains("Happy")) plugin.getEffectManager().setParticle(player, Particle.VILLAGER_HAPPY);

        player.sendMessage("§aEffekt aktiviert!");
        player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        player.closeInventory();
    }

    private ItemStack createGuiItem(Material material, String name, String permission) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        
        // Wir zeigen im Lore an, ob der Spieler das Item nutzen darf
        // (Wird später durch echte Permission-Prüfung ersetzt)
        meta.lore(List.of(Component.text("§7Benötigt: §e" + permission)));
        
        item.setItemMeta(meta);
        return item;
    }
    
 // In CosmeticMenuListener.java

    @EventHandler
    public void onDismount(org.bukkit.event.vehicle.VehicleExitEvent event) {
        if (event.getExited() instanceof Player) {
            // Das Fahrzeug (Schwein/Pferd) sofort entfernen, wenn der Spieler absteigt
            event.getVehicle().remove();
        }
    }
    
    public void openPlaceholderMenu(Player player, String featureName) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("§8» " + featureName));
        
        // Wir nutzen die fillGlass Methode aus der Hauptklasse oder definieren sie hier kurz:
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta m = glass.getItemMeta();
        m.displayName(Component.text(" "));
        glass.setItemMeta(m);
        for (int i = 0; i < 27; i++) inv.setItem(i, glass);

        // Info-Item in der Mitte
        ItemStack info = new ItemStack(Material.BARRIER);
        ItemMeta meta = info.getItemMeta();
        meta.displayName(Component.text("§c§lIn Arbeit..."));
        meta.lore(List.of(Component.text("§7Dieses Feature wird aktuell"), Component.text("§7noch entwickelt.")));
        info.setItemMeta(meta);
        
        inv.setItem(13, info);
        inv.setItem(22, createGuiItem(Material.ARROW, "§7Zurück", ""));

        player.openInventory(inv);
    }
    
    private void spawnMount(Player player, EntityType type, String name) {
        // 1. Altes Mount entfernen
        if (player.getVehicle() != null) {
            player.getVehicle().remove();
        }

        // 2. Entity spawnen
        org.bukkit.entity.Entity mount = player.getWorld().spawnEntity(player.getLocation(), type);
        
        // 3. Universelle Eigenschaften (für alle Tiere)
        if (mount instanceof org.bukkit.entity.LivingEntity living) {
            living.setAI(true); 
            living.setInvulnerable(true);
            living.setCustomNameVisible(false);
            
            // Speziell für Schweine/Pferde
            if (living instanceof org.bukkit.entity.Steerable steerable) {
                steerable.setSaddle(true);
            }
            if (living instanceof org.bukkit.entity.AbstractHorse horse) {
                horse.setTamed(true);
                horse.getInventory().setSaddle(new ItemStack(Material.SADDLE));
            }
        }

        // 4. Spieler draufsetzen
        mount.addPassenger(player);
        player.sendMessage("§aDu reitest nun auf einem §e" + name + "§a!");
        
        // 5. Steuerung aktivieren (Task)
        startMountControlTask(player, mount);
    }
    
    private void startMountControlTask(Player player, org.bukkit.entity.Entity mount) {
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (!mount.isValid() || mount.getPassengers().isEmpty() || !player.isOnline()) {
                    this.cancel();
                    if (mount.isValid()) mount.remove();
                    return;
                }

                // Wir nehmen die Blickrichtung des Spielers als Basis
                org.bukkit.util.Vector dir = player.getLocation().getDirection().setY(0).normalize();
                
                // WICHTIG: Wir prüfen, ob der Spieler sich bewegt. 
                // Da der Spieler auf dem Mob sitzt, ist seine eigene Velocity oft 0.
                // Wir nutzen daher einen "Input-Check":
                
                double speed = 0.25;
                if (player.isSprinting()) speed = 0.5;

                // Wir bewegen den Mob immer leicht in die Richtung, in die der Spieler schaut,
                // ABER nur wenn der Spieler nach vorne "drückt". 
                // In der Lobby reicht meistens die Blickrichtung-Steuerung ("Look-to-drive").
                Block ahead = mount.getLocation().add(dir.multiply(1.0)).getBlock();
                if (ahead.getType().isSolid() && mount.isOnGround()) {
                    mount.setVelocity(mount.getVelocity().setY(0.5));
                }
                
                mount.setVelocity(dir.multiply(speed).setY(-0.1));
                
                // Mount drehen
                mount.setRotation(player.getLocation().getYaw(), 0);
            }
        }.runTaskTimer(plugin, 1L, 1L); // Alle 1 Tick für flüssige Bewegung
    }
    
    @EventHandler
    public void onMountExit(org.bukkit.event.vehicle.VehicleExitEvent event) {
        if (event.getExited() instanceof Player) {
            // Das Mount (Vehicle) sofort entfernen
            event.getVehicle().remove();
            
            if (event.getExited() instanceof Player p) {
                p.sendMessage("§7Dein Mount wurde in den Stall geschickt.");
                p.playSound(p.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1f, 1f);
            }
        }
    }

}