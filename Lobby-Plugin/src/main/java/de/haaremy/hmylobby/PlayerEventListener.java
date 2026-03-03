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
        player.getInventory().setItem(1, createItem(Material.CHEST, "§eExtras", List.of("§7Partikel & Effekte")));
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
        } else if (name.equals("§eExtras")) {
            player.sendMessage("§eDieses Feature kommt bald!");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
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
            if (event.getRawSlot() == 0) { // Sprach-Icon
                player.sendMessage("§7Deine aktuelle Sprache ist: §e" + lang);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                player.closeInventory();
            }
        }
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
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("§bMy-Menü"));
        fillGlass(inv);
        inv.setItem(13, createItem(Material.DIAMOND, "§a" + lang, List.of("§7Klicke, um die Sprache zu ändern")));
        player.openInventory(inv);
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

    private void playFeedbackEffects(Player player) {
        player.showTitle(Title.title(Component.text("§6Willkommen"), Component.text("§b" + player.getName())));
        player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_TELEPORT, 0.5f, 1f);
    }

    @EventHandler public void onQuit(PlayerQuitEvent e) { activeBossBars.remove(e.getPlayer().getUniqueId()); }
    @EventHandler public void onMobSpawn(CreatureSpawnEvent e) { e.getEntity().setAI(false); }
}