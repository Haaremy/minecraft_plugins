package de.haaremy.hmylobby;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
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

        if (title.contains("Mounts")) {
            event.setCancelled(true);
            handleMountClick(player, event.getCurrentItem());
        } else if (title.contains("Partikel")) {
            event.setCancelled(true);
            handleParticleClick(player, event.getCurrentItem());
        }
    }

    private void handleMountClick(Player player, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return;
        String name = LegacyComponentSerializer.legacySection().serialize(item.getItemMeta().displayName());

        // Permission Abfrage
        String perm = "hmy.lobby.mount." + name.toLowerCase().replace("§", "").substring(1);
        if (!player.hasPermission(perm)) {
            player.sendMessage("§cDu hast keine Berechtigung für dieses Mount!");
            player.playSound(player, Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // Mount Logik (Beispiel Schwein)
        if (name.contains("Schwein")) {
            Pig pig = (Pig) player.getWorld().spawnEntity(player.getLocation(), EntityType.PIG);
            pig.setSaddle(true);
            pig.setAI(false); // Damit es nicht wegläuft
            pig.addPassenger(player);
            player.sendMessage("§aViel Spaß auf deinem Schwein!");
        }
        player.closeInventory();
    }

    private void handleParticleClick(Player player, ItemStack item) {
        // Ähnliche Logik wie Mounts: Permission prüfen, dann Effekt in einer Map speichern
        // (Effekte müssen in einem BukkitRunnable abgespielt werden)
        player.sendMessage("§aPartikel ausgewählt!");
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
}