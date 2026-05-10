package de.haaremy.hmynavigator;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class NavigatorListener implements Listener {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private final HmyNavigator plugin;
    private final NavigatorGui gui;
    private final ServerConnector connector;
    private final NavigatorConfig config;
    private final NamespacedKey navigatorKey;

    public NavigatorListener(HmyNavigator plugin, NavigatorGui gui, ServerConnector connector, NavigatorConfig config) {
        this.plugin = plugin;
        this.gui = gui;
        this.connector = connector;
        this.config = config;
        this.navigatorKey = new NamespacedKey(plugin, "navigator");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        giveCompass(event.getPlayer());
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        // Verzögert geben, damit das Inventar bereit ist
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            giveCompass(event.getPlayer());
        }, 1L);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || !isNavigatorCompass(item)) {
            return;
        }

        event.setCancelled(true);
        gui.open(event.getPlayer());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!gui.isNavigatorInventory(event.getView())) {
            return;
        }

        event.setCancelled(true);

        String gameMode = gui.getGameMode(event.getSlot());
        if (gameMode != null) {
            player.closeInventory();
            connector.sendToServer(player, gameMode);
        }
    }

    @EventHandler
    public void onPlayerDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (isNavigatorCompass(item)) {
            event.setCancelled(true);
        }
    }

    private void giveCompass(Player player) {
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();

        meta.displayName(HmyNavigator.COMPASS_DISPLAY_NAME);
        meta.lore(List.of(MINI.deserialize("<gray>Rechtsklick zum Öffnen</gray>")));
        meta.setCustomModelData(1001);

        // PersistentDataContainer Tag setzen
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(navigatorKey, PersistentDataType.BYTE, (byte) 1);

        compass.setItemMeta(meta);

        player.getInventory().setItem(config.getCompassSlot(), compass);
    }

    private boolean isNavigatorCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(navigatorKey, PersistentDataType.BYTE);
    }
}
