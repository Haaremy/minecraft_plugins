package de.haaremy.hmylobby;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import de.haaremy.hmylobby.utils.PermissionUtils;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;

public class PlayerEventListener implements Listener {

    private final HmyLobby plugin;
    private final String lang;
    
    private final Map<UUID, BossBar> activeBossBars = new HashMap<>();
    private final Map<UUID, Long> rocketCooldown = new HashMap<>();

    public PlayerEventListener(HmyLobby plugin, HmyLanguageManager language) {
        this.plugin = plugin;
        // Die Variable 'language' wurde entfernt, da nur der String 'lang' benötigt wird.
        this.lang = language.getMessage("language", "Sprache");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        event.joinMessage(null); 
        
        giveLobbyItems(player);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) playFeedbackEffects(player);
        }, 20L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        BossBar bar = activeBossBars.remove(uuid);
        if (bar != null) event.getPlayer().hideBossBar(bar);
        rocketCooldown.remove(uuid);
    }

    private void playFeedbackEffects(Player player) {
        // Particles (1.21 Fix mit Float-Daten am Ende)
        if (!PermissionUtils.hasPermission(player, "hmy.lobby.particle.none")) {
            player.getWorld().spawnParticle(Particle.DRAGON_BREATH, player.getLocation(), 100, 1.0, 0.2, 1.0, 0.1, 0.5f);
        }

        // Title (Adventure API)
        if (!PermissionUtils.hasPermission(player, "hmy.lobby.message.none")) {
            player.showTitle(Title.title(
                Component.text("§6Willkommen"),
                Component.text("§b" + player.getName()),
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(1000))
            ));
        }

        // BossBar (Adventure API)
        if (!PermissionUtils.hasPermission(player, "hmy.lobby.bossbar.none")) {
            BossBar bar = BossBar.bossBar(Component.text("§b§lMC.HAAREMY.DE"), 1.0f, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);
            player.showBossBar(bar);
            activeBossBars.put(player.getUniqueId(), bar);
        }
    }

    private void giveLobbyItems(Player player) {
        player.getInventory().clear();
        
        // Hintergrund-Glas (9-35)
        ItemStack i1 = createItem(Material.PINK_STAINED_GLASS_PANE, " ", List.of());
        ItemStack i2 = createItem(Material.BLUE_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 9; i < 36; i++) {
            player.getInventory().setItem(i, (i % 2 == 0) ? i1 : i2);
        }

        player.getInventory().setItem(0, getPlayerHead(player, "§bMy-Menü"));
        player.getInventory().setItem(4, createItem(Material.NETHER_STAR, "§6Lobby-Menü", List.of("§7Serverauswahl")));
        player.getInventory().setItem(6, createItem(Material.ARROW, "§7Speed", List.of("§7Klicke zum Ändern")));
        player.getInventory().setItem(7, createItem(Material.FIREWORK_ROCKET, "§bRakete", List.of("§7Abflug!")));
        player.getInventory().setItem(8, createItem(Material.ENCHANTED_BOOK, "§b???", List.of("§7Infos")));
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR || !item.hasItemMeta()) return;

        String name = LegacyComponentSerializer.legacySection().serialize(item.getItemMeta().displayName());

        if (name.contains("Speed")) {
            handleSpeedChange(player, item);
        } else if (name.equals("§6Lobby-Menü")) {
            openLobbyMenu(player);
        } else if (name.equals("§bMy-Menü")) {
            openHeadMenu(player);
        } else if (name.equals("§bRakete")) {
            handleRocketLaunch(player);
        } else if (name.equals("§b???")) {
            openBook(player);
        }
    }

    private void handleSpeedChange(Player player, ItemStack item) {
        if (item.getType() == Material.ARROW) {
            player.getInventory().setItem(6, createItem(Material.TIPPED_ARROW, "§bSpeed", List.of("§7Status: §bSchnell")));
            player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.3);
        } else if (item.getType() == Material.TIPPED_ARROW) {
            player.getInventory().setItem(6, createItem(Material.SPECTRAL_ARROW, "§6Speed", List.of("§7Status: §6Ultra")));
            player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.5);
        } else {
            player.getInventory().setItem(6, createItem(Material.ARROW, "§7Speed", List.of("§7Status: §7Normal")));
            player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.1);
        }
    }

    private void handleRocketLaunch(Player player) {
        long now = System.currentTimeMillis();
        if (rocketCooldown.getOrDefault(player.getUniqueId(), 0L) > now) return;

        if (!player.getLocation().subtract(0, 1, 0).getBlock().getType().isAir()) {
            player.setVelocity(player.getLocation().getDirection().multiply(2.0).setY(1.0));
            player.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, player.getLocation(), 10);
            player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 1f);
            rocketCooldown.put(player.getUniqueId(), now + 1500); 
        }
    }

 // 1. Verhindert das Aufheben von Items vom Boden
    @EventHandler
    public void onPickup(org.bukkit.event.entity.EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (!PermissionUtils.hasPermission(player, "hmy.lobby.inventory.edit")) {
                event.setCancelled(true);
            }
        }
    }

    // 2. Verhindert das Droppen (Wegwerfen) von Items
    @EventHandler
    public void onDrop(org.bukkit.event.player.PlayerDropItemEvent event) {
        if (!PermissionUtils.hasPermission(event.getPlayer(), "hmy.lobby.inventory.edit")) {
            event.setCancelled(true);
        }
    }

    // 3. Verhindert das Erhalten von Items durch den Kreativ-Katalog
    // (Diese Ergänzung kommt in dein bestehendes InventoryClickEvent)
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        // Verhindert das Verschieben/Hinzufügen im Kreativ-Inventar oder normalen Inventar
        if (!PermissionUtils.hasPermission(player, "hmy.lobby.inventory.edit")) {
            event.setCancelled(true);
            
            // Falls das Inventar das Lobby-Menü ist, Logik ausführen
            String title = LegacyComponentSerializer.legacySection().serialize(event.getView().title());
            if (title.contains("Menü") && event.getCurrentItem() != null) {
                if (event.getCurrentItem().getType() == Material.GRASS_BLOCK) {
                    player.performCommand("triggervelocity hmy server survival");
                    player.closeInventory();
                }
            }
        }
    }

    private void openLobbyMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, Component.text("§6Lobby-Menü"));
        fillGlass(inv);
        inv.setItem(13, createItem(Material.GRASS_BLOCK, "§aSurvival-Server", List.of("§7Wechseln")));
        player.openInventory(inv);
    }

    private void openHeadMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, Component.text("§bMy-Menü"));
        fillGlass(inv);
        inv.setItem(0, createItem(Material.DIAMOND, "§a" + lang, List.of("§7Sprache wählen")));
        player.openInventory(inv);
    }

    private void fillGlass(Inventory inv) {
        ItemStack i1 = createItem(Material.PINK_STAINED_GLASS_PANE, " ", List.of());
        ItemStack i2 = createItem(Material.BLUE_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) inv.setItem(i, (i % 2 == 0) ? i1 : i2);
        }
    }

    private ItemStack createItem(Material m, String name, List<String> lore) {
        ItemStack item = new ItemStack(m);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name));
            meta.lore(lore.stream().map(Component::text).toList());
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack getPlayerHead(Player p, String name) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(p);
        meta.displayName(Component.text(name));
        head.setItemMeta(meta);
        return head;
    }

    public void openBook(Player player) {
    	player.performCommand("triggervelocity help");
    }

    @EventHandler
    public void onMobSpawn(CreatureSpawnEvent event) {
        event.getEntity().setAI(false);
    }
}