package de.haaremy.hmylobby;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class PlayerEventListener implements Listener {

    private final HmyLobby plugin;

    public PlayerEventListener(HmyLobby plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getLogger().info(player.getName() + " hat die Lobby betreten.");

        // Schuhe und andere Items ins Inventar legen
        giveBoots(player);
        giveLobbyItems(player);

        // Partikel und Sounds mit Verzögerung und Wiederholung abspielen
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1.5, 0), 10, 0.5, 0.5, 0.5, 0.1);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }, 60L, 20L); // Startet nach 3 Sekunden und wiederholt sich jede Sekunde
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getLogger().info(player.getName() + " hat die Lobby verlassen.");
    }

    private void giveBoots(Player player) {
        ItemStack boots = createItem(Material.LEATHER_BOOTS, "§bWechselschuhe", List.of(
            "§7Klicke mit den Schuhen in der Hand, um",
            "§7die Geschwindigkeit zu ändern."
        ));
        player.getInventory().setItem(8, boots); // Schuhe in den letzten Slot legen
    }

    private void giveLobbyItems(Player player) {
        // Nether Stern (Position Mitte)
        ItemStack netherStar = createItem(Material.NETHER_STAR, "§6Lobby-Menü", List.of("§7Öffnet das Hauptmenü"));
        player.getInventory().setItem(4, netherStar);

        // Spieler-Kopf (Position Start)
        ItemStack playerHead = getPlayerHead(player, "§bDein My-Menü", List.of("§7Zeigt Optionen basierend auf Rechten an"));
        player.getInventory().setItem(0, playerHead);
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack getPlayerHead(Player player, String name, List<String> lore) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
        ItemMeta meta = head.getItemMeta();
        if (meta instanceof org.bukkit.inventory.meta.SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(player);
            skullMeta.setDisplayName(name);
            skullMeta.setLore(lore);
            head.setItemMeta(skullMeta);
        }
        return head;
    }

@EventHandler
public void onPlayerInteract(PlayerInteractEvent event) {
    Player player = event.getPlayer();
    ItemStack item = player.getInventory().getItemInMainHand();

    if (item != null && item.hasItemMeta() && item.getItemMeta().getDisplayName().equals("§bWechselschuhe")) {
        // Bestimme aktuelles Material und wechsle zum nächsten
        Material currentMaterial = item.getType();
        double newSpeed = 0.1; // Standardgeschwindigkeit

        switch (currentMaterial) {
            case LEATHER_BOOTS:
                item.setType(Material.GOLDEN_BOOTS);
                newSpeed = 0.3; // Geschwindigkeit erhöhen
                player.sendMessage("§6Geschwindigkeit erhöht: Goldene Schuhe (+30%)");
                break;

            case GOLDEN_BOOTS:
                item.setType(Material.DIAMOND_BOOTS);
                newSpeed = 0.5; // Geschwindigkeit weiter erhöhen
                player.sendMessage("§bGeschwindigkeit erhöht: Diamantschuhe (+50%)");
                break;

            case DIAMOND_BOOTS:
                item.setType(Material.LEATHER_BOOTS);
                newSpeed = 0.1; // Zurück zur Standardgeschwindigkeit
                player.sendMessage("§7Geschwindigkeit zurückgesetzt: Lederschuhe (+10%)");
                break;

            default:
                break;
        }

        // Geschwindigkeit setzen
        player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(newSpeed);

        // Aktualisiertes Item zurück ins Inventar legen
        player.getInventory().setItemInMainHand(item);

        // Feedback-Effekte (Partikel und Sound)
        playFeedbackEffects(player);
    }
}

private void playFeedbackEffects(Player player) {
    // Längere Partikel- und Sound-Effekte
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
        // Einmaliger Partikeleffekt (z. B. Herzen für 2 Sekunden)
        for (int i = 0; i < 40; i++) { // 40 Frames für 2 Sekunden
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.getWorld().spawnParticle(
                    Particle.HEART,
                    player.getLocation().add(0, 1.5, 0), // Position über dem Spieler
                    5, // Anzahl der Partikel
                    0.5, 0.5, 0.5, // Offset für zufällige Verteilung
                    0.1 // Geschwindigkeit
                );
            }, i);
        }

        // Einmaliger Soundeffekt (längere Dauer)
        player.playSound(
            player.getLocation(),
            Sound.ENTITY_PLAYER_LEVELUP,
            1.0f, // Lautstärke
            1.0f // Tonhöhe
        );
    }, 0L); // Sofort starten
}

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta() || clickedItem.getItemMeta().getDisplayName() == null) return;

        String itemName = clickedItem.getItemMeta().getDisplayName();
        switch (itemName) {
            case "§6Lobby-Menü":
                openLobbyMenu(player);
                break;

            case "§bDein My-Menü":
                openHeadMenu(player);
                break;

            default:
                break;
        }

        if (!player.hasPermission("hmy.lobby.inventory.edit")) {
            event.setCancelled(true); // Verhindert, dass Items verschoben werden
        }
    }

    private void openLobbyMenu(Player player) {
        Inventory menu = Bukkit.createInventory(null, 9, "§6Lobby-Menü");

        // Item hinzufügen: Grasblock (Position 1)
        ItemStack grassBlock = createItem(Material.GRASS_BLOCK, "§aSurvival-Server", List.of("§7Klicke hier, um", "§7zum Survival-Server zu wechseln."));
        menu.setItem(1, grassBlock);

        player.openInventory(menu);
    }

    private void openHeadMenu(Player player) {
        Inventory menu = Bukkit.createInventory(null, 9, "§bKopf-Menü");
        // Hier weitere Items hinzufügen
        player.openInventory(menu);
    }
}
