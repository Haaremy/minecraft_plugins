package de.haaremy.hmylobby;

import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.util.Vector;

import de.haaremy.hmylobby.utils.PermissionUtils;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;

public class PlayerEventListener implements Listener {

    private final HmyLobby plugin;
    private BossBar bossBar;
    private final HmyLanguageManager language;
    private final String lang;

    public PlayerEventListener(HmyLobby plugin, HmyLanguageManager language) {
        this.plugin = plugin;
        this.language = language;
        lang = language.getMessage("language","Sprache");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        event.setJoinMessage(null); // Prevent the join message from being broadcast
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

        ItemStack i1 = createItem(Material.PINK_STAINED_GLASS_PANE, " ", List.of(" "));
        ItemStack i2 = createItem(Material.BLUE_STAINED_GLASS_PANE, " ", List.of(" "));

        for(byte i = 9; i<36; i++){
           if(i%2==0){
            player.getInventory().setItem(i, i1);
           } else player.getInventory().setItem(i, i2); 
        }

        ItemStack netherStar = createItem(Material.NETHER_STAR, "§6Lobby-Menü", List.of("§7Öffnet den Spiele-Katalog"));
        player.getInventory().setItem(4, netherStar);

        // Spieler-Kopf (Position Start)
        ItemStack playerHead = getPlayerHead(player, "§bMy-Menü", List.of("§7Öffnet Kosmetische Gegenstände."));
        player.getInventory().setItem(0, playerHead);

        // Schuhe für Geschwindigkeit (Position Ende)
        ItemStack Pfeil = createItem(Material.ARROW, "§7Speed", List.of(
                    "§7Klicke mit dem Pfeil in der Hand, um",
                    "§7die Geschwindigkeit zu ändern."
                ));
        player.getInventory().setItem(6, Pfeil);

        // Rakete für Leap (Position Ende)
        ItemStack Rocket = createItem(Material.FIREWORK_ROCKET,"§bRakete", List.of( "§7Schießt dich in die Luft." ));
        player.getInventory().setItem(7, Rocket);

        // Help Book (Position Ende)
        ItemStack Help = createItem(Material.ENCHANTED_BOOK,"§b???", List.of( "§7Nützliches." ));
        player.getInventory().setItem(8, Help);

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
    String itemName = item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : "";
    ItemStack newItem;
  
        switch (itemName) {
            case "§7Speed":
                newItem = createItem(Material.TIPPED_ARROW, "§bSpeed", List.of(
                    "§7Klicke mit dem Pfeil in der Hand, um",
                    "§7die Geschwindigkeit zu ändern."
                ));
                player.getInventory().setItem(6, newItem);
                player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.3);
                break;

            case "§bSpeed":
                newItem = createItem(Material.SPECTRAL_ARROW, "§6Speed", List.of(
                    "§7Klicke mit den Schuhen in der Hand, um",
                    "§7die Geschwindigkeit zu ändern."
                ));
                player.getInventory().setItem(6, newItem);
                player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.5);
                break;

            case "§6Speed":
                newItem = createItem(Material.ARROW, "§7Speed", List.of(
                    "§7Klicke mit den Schuhen in der Hand, um",
                    "§7die Geschwindigkeit zu ändern."
                ));
                player.getInventory().setItem(6, newItem);
                player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.1);
                break;

            case "§6Lobby-Menü":
                openLobbyMenu(player);
                break;
            
            case "§bMy-Menü":
                openHeadMenu(player);
                break;
            case "§bRakete":
                double height = 10.0d; // Standardhöhe
                double distance = 4.0d;
                if(player.getLocation().subtract(0, 2, 0).getBlock().getType() != Material.AIR){
                    player.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, player.getLocation(), 20);
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1, 0.4f);
                    player.setVelocity(player.getLocation().getDirection().multiply(distance));
                    player.setVelocity(new Vector(player.getVelocity().getX(), height, player.getVelocity().getZ()));
                }
                break;
            case "§b???":
                openBook(player);
                break;

            default:
                return; // Kein gültiges Item
        }

        

    
}

private void playFeedbackEffects(Player player) {
        // Einmaliger Partikeleffekt (z. B. Herzen für 2 Sekunden)
            if (!PermissionUtils.hasPermission(player,"hmy.lobby.particle.none")) {
                player.getWorld().spawnParticle(
                    Particle.DRAGON_BREATH,
                    player.getLocation().add(0, 0, 0), // Position unter dem Spieler
                    1000, // Anzahl der Partikel
                    1, 0, 1, // Offset für zufällige Verteilung
                    0.1 // Geschwindigkeit
                );
            }
 

if (!PermissionUtils.hasPermission(player,"hmy.lobby.sound.none")) {
        // Einmaliger Soundeffekt (längere Dauer)
        player.playSound(
            player.getLocation(),
            Sound.ENTITY_PLAYER_LEVELUP,
            1.0f, // Lautstärke
            0.4f // Tonhöhe
        );
         player.playSound(
            player.getLocation(),
            Sound.BLOCK_BELL_USE,
            2.0f, // Lautstärke
            0.1f // Tonhöhe
        );
}
if (!PermissionUtils.hasPermission(player,"hmy.lobby.message.none")) {
        player.sendTitle(
    "§6Willkommen",               // Haupttitel
    player.getName(),             // Untertitel
    10,                           // Einblenden (0,5 Sekunden)
    70,                           // Bleiben (3,5 Sekunden)
    20                            // Ausblenden (1 Sekunde)
);
}
if (!PermissionUtils.hasPermission(player,"hmy.lobby.bossbar.none")) {
        bossBar = BossBar.bossBar(Component.text("MC.HAAREMY.DE"), 0, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);
        player.showBossBar(bossBar);
}
}

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack item = event.getCurrentItem();
        String itemName = item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : "";

         switch (itemName) {
            case "§aSurvival-Server":
                player.playSound(
                    player.getLocation(),
                    Sound.BLOCK_PORTAL_TRAVEL,
                    2.0f, // Lautstärke
                    0.1f // Tonhöhe
                );
                 player.performCommand("triggervelocity hmy server survival");
                 player.hideBossBar(bossBar);
                break;
            default:
                break;
         }


        if (!PermissionUtils.hasPermission(player,"hmy.lobby.inventory.edit")) {
            event.setCancelled(true); // Verhindert, dass Items verschoben werden
        }
    }


    private void openLobbyMenu(Player player) {

        Inventory menu = Bukkit.createInventory(null, 45, "§6Lobby-Menü");

        ItemStack i1 = createItem(Material.PINK_STAINED_GLASS_PANE, "", List.of(""));
        ItemStack i2 = createItem(Material.BLUE_STAINED_GLASS_PANE, "", List.of(""));

        for(byte i = 0; i<45; i++){
           if(i%2==0){
            menu.setItem(i, i1);
           } else menu.setItem(i, i2); 
        }


        ItemStack netherBlock = createItem(Material.NETHER_STAR, "§aLobby-Server", List.of("§7Du bist hier."));
        menu.setItem(22, netherBlock);

        ItemStack grassBlock = createItem(Material.GRASS_BLOCK, "§aSurvival-Server", List.of("§7Klicke hier, um", "§7zum Survival-Server zu wechseln."));
        menu.setItem(13, grassBlock);

        player.openInventory(menu);
    }

    private void openHeadMenu(Player player) {
        Inventory menu = Bukkit.createInventory(null, 45, "§bMy-Menü");

        ItemStack i1 = createItem(Material.PINK_STAINED_GLASS_PANE, " ", List.of(" "));
        ItemStack i2 = createItem(Material.BLUE_STAINED_GLASS_PANE, " ", List.of(" "));

        for(byte i = 0; i<45; i++){
           if(i%2==0){
            menu.setItem(i, i1);
           } else menu.setItem(i, i2); 
        }

        ItemStack newItem0 = createItem(Material.DIAMOND, "§a"+lang, List.of("Wähle deine Sprache."));
        menu.setItem(0, newItem0);

        ItemStack newItem1 = createItem(Material.DIAMOND, "§aPartikel Schuhe", List.of("Die Partikel folgen deinen Füßen."));
        menu.setItem(9, newItem1);

        ItemStack newItem3 = createItem(Material.EMERALD, "§aPartikel Aura", List.of("§7Folgenden dir, wie eine Sternschnuppe."));
        menu.setItem(18, newItem3);

        ItemStack newItem2 = createItem(Material.CARROT_ON_A_STICK, "§aReittier", List.of("§7Modisch und fesh durch den Tag."));
        menu.setItem(27, newItem2);

        ItemStack newItem4 = createItem(Material.BLAZE_SPAWN_EGG, "§aHaustier", List.of("§7Mutig und Brav sowie Stubenrein."));
        menu.setItem(36, newItem4);


        
        player.openInventory(menu);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Block blockUnderPlayer = player.getLocation().subtract(0, 2, 0).getBlock();

        // Prüfen, ob der Block grüne Wolle ist
        if (blockUnderPlayer.getType() == Material.GREEN_WOOL) {
            player.performCommand("triggervelocity hmy server survival");
        }
    }

    @EventHandler
public void onMobSpawn(CreatureSpawnEvent event) {
    LivingEntity entity = event.getEntity();

    // Deaktiviere die AI des Mobs
    entity.setAI(false);
}

public ItemStack createCustomHead(UUID uuid, String texture, String displayName ) {
        // Erstelle den Spieler-Kopf
        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta skullMeta = (SkullMeta) playerHead.getItemMeta();

        // Setze die Anzeigeinformationen
        skullMeta.setDisplayName(displayName);

        // Setze die spezifische UUID und Textur
        skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));

        applyTextureToHead(skullMeta, texture);

        playerHead.setItemMeta(skullMeta);
        return playerHead;
    }

    private void applyTextureToHead(SkullMeta skullMeta, String texture) {
        try {
            // Nutze Reflections, um die Textur hinzuzufügen
            Object profile = Class.forName("com.mojang.authlib.GameProfile")
                    .getConstructor(UUID.class, String.class)
                    .newInstance(UUID.randomUUID(), null);

            Object properties = profile.getClass().getMethod("getProperties").invoke(profile);
            Class.forName("com.mojang.authlib.properties.PropertyMap")
                    .getMethod("put", Object.class, Object.class)
                    .invoke(properties, "textures", Class.forName("com.mojang.authlib.properties.Property")
                            .getConstructor(String.class, String.class)
                            .newInstance("textures", texture));

            skullMeta.getClass().getDeclaredMethod("setProfile", profile.getClass())
                    .invoke(skullMeta, profile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void openBook(Player player) {
        // Create a book item
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta bookMeta = (BookMeta) book.getItemMeta();

        // Set title, author, and pages
        if (bookMeta != null) {
            bookMeta.setTitle("Mc.Haaremy.de 101");
            bookMeta.setAuthor("Haaremy");

        // Inhaltsverzeichnis (Seite 1)
        bookMeta.addPage(
            "§lInhaltsverzeichnis\n\n" +
            "§n1§r. Willkommen\n" +
            "§n2§r. Regeln\n" +
            "§n3§r. Befehle\n" +
            "§n4§r. Kontakt\n"
        );
            // Add pages (each page can contain multiple lines)
            bookMeta.addPage(
                "§1Willkommen auf dem Server!\n" +
                "§2MC.HAAREMY.DE\n" +
                "\n" +
                "§3Du kannst überall mit /help\n"+
                "dieses Hilfe-Buch öffnen."
            );
            bookMeta.addPage(
                "§4Regeln:\n" +
                "1. Netter Umgang.\n" +
                "2. Respekt.\n" +
                "3. Kein Cheating.\n" +
                "\n" +
                "Viel Spaß!"
            );
            bookMeta.addPage(
                "§4Befehle:\n" +
                "/help - dieses Buch.\n" +
                "/lobby - Lobby Server.\n" +
                "/spawn - Spawn, falls vorhanden.\n" +
                "\n"
            );
            bookMeta.addPage(
                "§4Kontakt:\n" +
                "haaremy@gmail.com\n" +
                "Insta: @Haaremy\n" +
                "Github: @Haaremy\n" +
                "\n"
            );

            // Set the book meta to the book item
            book.setItemMeta(bookMeta);
        }

        player.openBook(book);
    }
}
