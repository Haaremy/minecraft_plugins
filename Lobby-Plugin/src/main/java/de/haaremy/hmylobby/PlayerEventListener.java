package de.haaremy.hmylobby;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import de.haaremy.hmylobby.utils.PermissionUtils;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
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
import org.bukkit.inventory.meta.SkullMeta;

import java.time.Duration;
import java.util.*;

public class PlayerEventListener implements Listener {

    private final HmyLobby plugin;
    private final String lang;
    private final Map<UUID, BossBar> activeBossBars = new HashMap<>();
    private final Map<UUID, Long> rocketCooldown = new HashMap<>();

    public PlayerEventListener(HmyLobby plugin, HmyLanguageManager language) {
        this.plugin = plugin;
        this.lang = language.getMessage("language", "Sprache");
        
        // Lava-Schadens-Task (Prüft alle 10 Ticks)
        Bukkit.getScheduler().runTaskTimer(plugin, this::handleLavaDamage, 20L, 10L);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        event.joinMessage(null);
        
        player.setGameMode(GameMode.ADVENTURE);
        giveLobbyItems(player);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) playFeedbackEffects(player);
        }, 20L);
    }

    private void handleLavaDamage() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getLocation().getBlock().getType() == Material.LAVA) {
                // Schaden, der Regeneration ignoriert (Direktes Setzen der Health oder Damage-Event)
                double current = player.getHealth();
                if (current > 2.0) {
                    player.setHealth(current - 2.0);
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT_ON_FIRE, 0.5f, 1f);
                    player.getWorld().spawnParticle(Particle.LAVA, player.getLocation(), 5);
                } else {
                    player.teleport(player.getWorld().getSpawnLocation());
                    player.setHealth(20.0);
                    player.sendMessage("§cPass auf die Lava auf!");
                }
            }
        }
    }

    // SAUBERE SERVERWECHSEL LOGIK (Ohne Command)
    private void connectToServer(Player player, String server) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(server);
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    private void giveLobbyItems(Player player) {
        player.getInventory().clear();
        player.getInventory().setItem(0, getPlayerHead(player, "§bMy-Menü"));
        player.getInventory().setItem(4, createItem(Material.NETHER_STAR, "§6Lobby-Menü", List.of("§7Serverauswahl")));
        player.getInventory().setItem(7, createItem(Material.FIREWORK_ROCKET, "§bRakete", List.of("§7Abflug!")));
        player.getInventory().setItem(8, createItem(Material.ENCHANTED_BOOK, "§bInfos", List.of("§7Hilfe & Befehle")));
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return;

        String name = LegacyComponentSerializer.legacySection().serialize(item.getItemMeta().displayName());

        if (name.equals("§6Lobby-Menü")) {
            openLobbyMenu(player);
        } else if (name.equals("§bMy-Menü")) {
            openHeadMenu(player);
        } else if (name.equals("§bRakete")) {
            handleRocketLaunch(player);
        } else if (name.equals("§bInfos")) {
            player.performCommand("help");
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        // Globaler Schutz: Niemand darf Items im Inventar verschieben (außer mit Admin-Recht)
        if (!PermissionUtils.hasPermission(player, "hmy.lobby.inventory.edit")) {
            event.setCancelled(true);
        }

        String title = LegacyComponentSerializer.legacySection().serialize(event.getView().title());

        // LOGIK FÜR DAS LOBBY-MENÜ
        if (title.equals("§6Lobby-Menü")) {
        	event.setCancelled(true);
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

            int clickedSlot = event.getRawSlot();
            for (ServerSelectorConfig.SelectorEntry entry : plugin.getServerSelectorConfig().getEntries()) {
                if (entry.slot() == clickedSlot) {
                    player.closeInventory();
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.2f);
                    connectToServer(player, entry.server());
                    break;
                }
            }
        }
        
        // LOGIK FÜR DAS MY-MENÜ
        if (title.equals("§bMy-Menü")) {
        	event.setCancelled(true);
            int slot = event.getRawSlot();
            
            switch (slot) {
            case 10 -> plugin.getCosmeticMenuListener().openParticleMenu(player);
            case 11 -> player.sendMessage("§dCosmetics kommen bald!");
            case 13 -> openLanguageMenu(player);
            case 15 -> player.sendMessage("§aKöpfe kommen bald!");
            case 16 -> plugin.getCosmeticMenuListener().openMountMenu(player);
            case 22 -> player.sendMessage("§7Einstellungen kommen bald!");
        }
            if (slot != -1) player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5f, 1f);
        }

        if (title.contains("Wähle deine Sprache")) {
            event.setCancelled(true);
            if (event.getRawSlot() == 18) { openHeadMenu(player); return; }
            
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) return;
            
            String name = LegacyComponentSerializer.legacySection().serialize(clicked.getItemMeta().displayName());
            if (name.contains("Deutsch")) {
                player.performCommand("hmy language de");
                player.closeInventory();
            } else if (name.contains("English")) {
                player.performCommand("hmy language en");
                player.closeInventory();
            }
        }
    

        // --- Logik für das neue Sprach-Menü ---
        if (title.equals("§bWähle deine Sprache")) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() != Material.PLAYER_HEAD) return;

            String name = LegacyComponentSerializer.legacySection().serialize(clicked.getItemMeta().displayName());

            if (name.contains("Deutsch")) {
                // Hier deine Logik zum Sprache setzen (z.B. über LuckPerms oder Config)
                player.performCommand("hmy language de"); 
                player.closeInventory();
                player.sendMessage("§aSprache auf Deutsch gestellt!");
            } else if (name.contains("English")) {
                player.performCommand("hmy language en");
                player.closeInventory();
                player.sendMessage("§aLanguage set to English!");
            }
            player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
        }
    }
    
    private void openLanguageMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("§8» §bWähle deine Sprache"));
        fillGlass(inv);

        // Zurück-Button
        inv.setItem(18, createItem(Material.ARROW, "§cZurück", List.of("§7Zum My-Menü")));

        // Deutschland (Base64 URL)
        inv.setItem(11, createCustomHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWU3ODk5YjQ4ZTM4ZTk1NmZlYzE5YjYyYmFjOTExYWZlY2E5NWYzM2M3Mjg4ZTUzNjgzNjQ4ZWQwODZlMmIifX19", "§eDeutsch"));
        
        // UK/English (Base64 URL)
        inv.setItem(15, createCustomHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGNhYzk3NzRkYTEyMTcyNDgxOTJiZWI5MmU3OTUxNVA3Y2FhYzc1OTkzODVlYzE1ODQwY2Y2Y2E3NjM1In19fQ==", "§bEnglish"));

        player.openInventory(inv);
    }

    private void handleRocketLaunch(Player player) {
        long now = System.currentTimeMillis();
        if (rocketCooldown.getOrDefault(player.getUniqueId(), 0L) > now) return;

        player.setVelocity(player.getLocation().getDirection().multiply(1.5).setY(0.8));
        player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, player.getLocation(), 20, 0.2, 0.2, 0.2, 0.05);
        player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 1f);
        rocketCooldown.put(player.getUniqueId(), now + 1000); 
    }

    private void openLobbyMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, Component.text("§6Lobby-Menü"));
        fillGlass(inv);

        for (ServerSelectorConfig.SelectorEntry entry : plugin.getServerSelectorConfig().getEntries()) {
            inv.setItem(entry.slot(), createItem(entry.material(), entry.name(), entry.lore()));
        }
        player.openInventory(inv);
    }

    private void openHeadMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("§b§lMY-MENÜ §8» §7Profile"));
        fillGlass(inv); // Füllt alles mit grauem Glas

        // Reihe 1: Profile & Infos
        inv.setItem(4, getPlayerHead(player, "§6§lDein Profil §8(§7" + player.getName() + "§8)"));

        // Reihe 2: Die Haupt-Features (Symmetrisch angeordnet)
        // Slot 10: Partikel
        inv.setItem(10, createItem(Material.BLAZE_POWDER, "§e§lPartikel", 
            List.of("§7Wähle einen magischen Schweif,", "§7der dir auf Schritt und Tritt folgt.", "", "§aKlicke zum Öffnen!")));

        // Slot 11: Cosmetics (Hüte/Rucksäcke)
        inv.setItem(11, createItem(Material.LEATHER_CHESTPLATE, "§d§lCosmetics", 
            List.of("§7Statte dich mit coolen", "§7Accessoires aus.", "", "§aKlicke zum Öffnen!")));

        // Slot 13: SPRACHE (Zentrum)
        inv.setItem(13, createItem(Material.DIAMOND, "§b§lSprache §8| §7Language", 
            List.of("§7Deine Sprache: §b" + lang, "", "§e» Klicke zum Ändern!")));

        // Slot 15: Köpfe (Heads)
        inv.setItem(15, createItem(Material.ZOMBIE_HEAD, "§a§lKöpfe", 
            List.of("§7Setze dir einen der", "§7vielen Köpfe auf.", "", "§aKlicke zum Öffnen!")));

        // Slot 16: Mounts (Reittiere)
        inv.setItem(16, createItem(Material.SADDLE, "§c§lMounts", 
            List.of("§7Reite auf Drachen, Pferden", "§7oder verrückten Kreaturen.", "", "§aKlicke zum Öffnen!")));

        // Reihe 3: Status / Einstellungen
        inv.setItem(22, createItem(Material.REDSTONE_TORCH, "§7Einstellungen", 
            List.of("§7Verwalte Sichtbarkeit,", "§7Chat und Anfragen.")));

        player.openInventory(inv);
        player.playSound(player, Sound.BLOCK_CHEST_OPEN, 1f, 1.2f);
    }

    private void fillGlass(Inventory inv) {
        ItemStack i1 = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) inv.setItem(i, i1);
        }
    }

    private ItemStack createItem(Material m, String name, List<String> lore) {
        ItemStack item = new ItemStack(m);
        item.editMeta(meta -> {
            meta.displayName(Component.text(name));
            meta.lore(lore.stream().map(Component::text).toList());
        });
        return item;
    }

    private ItemStack getPlayerHead(Player p, String name) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        head.editMeta(SkullMeta.class, meta -> {
            meta.setOwningPlayer(p);
            meta.displayName(Component.text(name));
        });
        return head;
    }
    
    private ItemStack createCustomHead(String url, String name) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        
        // Wir nutzen das PlayerProfile (Paper API), um die Textur zu setzen
        com.destroystokyo.paper.profile.PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
        profile.getProperties().add(new com.destroystokyo.paper.profile.ProfileProperty("textures", url));
        meta.setPlayerProfile(profile);
        
        meta.displayName(Component.text(name));
        head.setItemMeta(meta);
        return head;
    }

    private void playFeedbackEffects(Player player) {
        player.showTitle(Title.title(Component.text("§6Willkommen"), Component.text("§b" + player.getName())));
        player.getWorld().spawnParticle(Particle.DRAGON_BREATH, player.getLocation(), 20, 0.2, 0.2, 0.2, 0.05);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1f);
    }

    @EventHandler public void onQuit(PlayerQuitEvent e) { activeBossBars.remove(e.getPlayer().getUniqueId()); }
    @EventHandler public void onMobSpawn(CreatureSpawnEvent e) { e.getEntity().setAI(false); }
}