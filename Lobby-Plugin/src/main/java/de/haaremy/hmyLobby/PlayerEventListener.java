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
        giveLobbyItems(player);

        // Partikel und Sounds mit Verzögerung und Wiederholung abspielen
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            playFeedbackEffects(player);
        }, 20L); // Startet nach 1 Sekunden und wiederholt sich jede Sekunde
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getLogger().info(player.getName() + " hat die Lobby verlassen.");
    }



    private void giveLobbyItems(Player player) {
        // Nether Stern (Position Mitte)
        ItemStack netherStar = createItem(Material.NETHER_STAR, "§6Lobby-Menü", List.of("§7Öffnet das Hauptmenü"));
        player.getInventory().setItem(4, netherStar);

        // Spieler-Kopf (Position Start)
        ItemStack playerHead = getPlayerHead(player, "§bDein My-Menü", List.of("§7Zeigt Optionen basierend auf Rechten an"));
        player.getInventory().setItem(0, playerHead);

        // Schuhe für Geschwindigkeit (Position Ende)
        ItemStack Pfeil = createItem(Material.ARROW, "§bPfeil", List.of(
                    "§7Klicke mit den Schuhen in der Hand, um",
                    "§7die Geschwindigkeit zu ändern."
                ));
                player.getInventory().setItem(8, Pfeil);
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
    ItemStack newItem;

  
        switch (item.getType()) {
            case ARROW:
                newItem = createItem(Material.TIPPED_ARROW, "§bPfeil", List.of(
                    "§7Klicke mit dem Pfeil in der Hand, um",
                    "§7die Geschwindigkeit zu ändern."
                ));
                player.getInventory().setItem(8, newItem);
                player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.3);
                break;

            case TIPPED_ARROW:
                newItem = createItem(Material.SPECTRAL_ARROW, "§bPfeil", List.of(
                    "§7Klicke mit den Schuhen in der Hand, um",
                    "§7die Geschwindigkeit zu ändern."
                ));
                player.getInventory().setItem(8, newItem);
                player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.5);
                break;

            case SPECTRAL_ARROW:
                newItem = createItem(Material.ARROW, "§bPfeil", List.of(
                    "§7Klicke mit den Schuhen in der Hand, um",
                    "§7die Geschwindigkeit zu ändern."
                ));
                player.getInventory().setItem(8, newItem);
                player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.1);
                break;

            case NETHER_STAR:
                openLobbyMenu(player);
                break;
            
            case PLAYER_HEAD:
                openHeadMenu(player);
                break;


            default:
                return; // Kein gültiges Item
        }

        

    
}

private void playFeedbackEffects(Player player) {
        // Einmaliger Partikeleffekt (z. B. Herzen für 2 Sekunden)

                player.getWorld().spawnParticle(
                    Particle.DRAGON_BREATH,
                    player.getLocation().add(0, 0, 0), // Position unter dem Spieler
                    1000, // Anzahl der Partikel
                    1, 0, 1, // Offset für zufällige Verteilung
                    0.1 // Geschwindigkeit
                );
 


        // Einmaliger Soundeffekt (längere Dauer)
        player.playSound(
            player.getLocation(),
            Sound.ENTITY_PLAYER_LEVELUP,
            1.0f, // Lautstärke
            0.4f // Tonhöhe
        );

        //player.sendTitle();
}

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack item = player.getInventory().getItemInMainHand();

         switch (event.getCurrentItem().getType()) {
            case GRASS_BLOCK:
                 player.performCommand("triggervelocity hmy server survival");
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

        ItemStack netherBlock = createItem(Material.NETHER_STAR, "§aLobby-Server", List.of("§7Du bist hier."));
        menu.setItem(0, netherBlock);

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
