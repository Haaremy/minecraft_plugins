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
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PlayerEventListener implements Listener {

    private final HmyLobby plugin;

    public PlayerEventListener(HmyLobby plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getLogger().info(player.getName() + " hat die Lobby betreten.");


        // Inventar vorbereiten
        giveLobbyItems(player);
        // Gegenstand ins Inventar legen
        giveBoots(player);


        Bukkit.getScheduler().runTaskLater(plugin, () -> {
             // Willkommensnachricht senden (sofern Berechtigung nicht vorhanden ist)
            if (true){  //!player.hasPermission("hmy.lobby.message.none")) {
                player.sendTitle(
                    "Willkommen, " + player.getName() + "!",
                    "Viel Spaß auf dem Server!",
                    10, 70, 20
                );
            }
                // Sound abspielen
            if (true){ //!player.hasPermission("hmy.lobby.sound.none")) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);
            }

                // Partikel anzeigen
            if (true){ //!player.hasPermission("hmy.lobby.particle.none")) {
                for (int i = 0; i < 5; i++) {
                    player.getWorld().spawnParticle(
                        Particle.HEART,                     // Partikeltyp
                        player.getLocation().add(0, 1.5, 0), // Position über dem Spieler
                        10,                                  // Anzahl der Partikel
                        0.5, 0.5, 0.5,                       // Offset (x, y, z)
                        0.1                                  // Geschwindigkeit
                    );
                }
            }
        }, 20L); // 20 Ticks Verzögerung = 1 Sekunden
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getLogger().info(player.getName() + " hat die Lobby verlassen.");
    }

   private void giveBoots(Player player) {
    // Schuhe erstellen
    ItemStack boots = createItem(Material.DIAMOND_BOOTS, "§bLobby-Schuhe", List.of(
        "§7Gibt dir einen Geschwindigkeitsschub",
        "§7und setzt die Geschwindigkeit zurück"
    ));

    // Schuhe an die letzte Inventarposition legen
    player.getInventory().setItem(8, boots);

    // Standardgeschwindigkeit setzen
    applyEffects(player, 0, 0); // Standardgeschwindigkeit, keine Sprungkraft
}

private void applyEffects(Player player, double speedMultiplier, int jumpBoostLevel) {
    // Geschwindigkeit setzen
    player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.1 + speedMultiplier);

    // Sprungkraft hinzufügen
    if (jumpBoostLevel > 0) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 20 * 60, jumpBoostLevel - 1)); // Level - 1 wegen Minecraft-Logik
    }
}


 private void giveLobbyItems(Player player) {
        // Nether Stern (Position Mitte)
        ItemStack netherStar = createItem(Material.NETHER_STAR, "§6Lobby-Menü", List.of("§7Öffnet das Hauptmenü"));
        player.getInventory().setItem(4, netherStar);

        // Kopf (Position Start)
       // Spieler-Kopf (Position Start)
    ItemStack playerHead = getPlayerHead(player, "§bDein My-Menü", List.of("§7Zeigt Optionen basierend auf Rechten an"));
    player.getInventory().setItem(0, playerHead);
    }

    private ItemStack getPlayerHead(Player player, String name, List<String> lore) {
    // Spieler-Kopf erstellen
    ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
    ItemMeta meta = head.getItemMeta();
    if (meta instanceof org.bukkit.inventory.meta.SkullMeta skullMeta) {
        skullMeta.setOwningPlayer(player); // Setzt den Kopf auf den Skin des Spielers
        skullMeta.setDisplayName(name);
        skullMeta.setLore(lore);
        head.setItemMeta(skullMeta);
    }
    return head;
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

        case "§bLobby-Schuhe":
            applyEffects(player, 1, 2); // Geschwindigkeit +100%, Sprungkraft Level 2
            break;
        case "§aSurvival":
            // Befehl ausführen
            player.performCommand("hmy server survival");
            player.closeInventory(); // Schließt das Menü
            break;

        default:
            break;
    }
    if (!player.hasPermission("hmy.lobby.inventory.edit")){ 
        event.setCancelled(true); // Verhindert, dass Items verschoben werden
    }
    }

    private void openLobbyMenu(Player player) {
        Inventory menu = Bukkit.createInventory(null, 9, "§6Lobby-Menü");
        // Hier Items für das Menü hinzufügen
        // Item hinzufügen (Minecraft:Gras)
        ItemStack grassBlock = createItem(Material.GRASS_BLOCK, "§aSurvival", List.of("§7Klicke hier, um", "§7zum Survival-Server zu wechseln."));
        menu.setItem(1, grassBlock); // Position 1 (Slot 1 im Inventar)
        player.openInventory(menu);
    }

    private void openHeadMenu(Player player) {
        Inventory menu = Bukkit.createInventory(null, 9, "§bKopf-Menü");
        // Hier Items basierend auf Berechtigungen hinzufügen
        player.openInventory(menu);
    }

}
