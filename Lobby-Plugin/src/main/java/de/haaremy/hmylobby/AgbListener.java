package de.haaremy.hmylobby;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Shows a Terms of Service (AGB) inventory to players who have not yet accepted them.
 * Acceptance grants the LuckPerms permission node "hmy.agb".
 */
public class AgbListener implements Listener {

    private static final String AGB_PERMISSION = "hmy.agb";
    private static final String INVENTORY_TITLE = "§8Nutzungsbedingungen (AGB)";

    // Players who currently have the AGB inventory open
    private final Set<UUID> pendingAcceptance = new HashSet<>();

    private final HmyLobby plugin;
    private final LuckPerms luckPerms;

    public AgbListener(HmyLobby plugin, LuckPerms luckPerms) {
        this.plugin     = plugin;
        this.luckPerms  = luckPerms;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission(AGB_PERMISSION)) return;

        // Delay slightly so the player is fully loaded
        Bukkit.getScheduler().runTaskLater(plugin, () -> openAgbInventory(player), 40L);
    }

    private void openAgbInventory(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, INVENTORY_TITLE);

        // Fill with gray glass
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.displayName(net.kyori.adventure.text.Component.text(" "));
        filler.setItemMeta(fillerMeta);
        for (int i = 0; i < 27; i++) inv.setItem(i, filler);

        // Info item (center-top area)
        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName("§6§lNutzungsbedingungen");
        infoMeta.setLore(List.of(
            "§7Willkommen auf §dmc.haaremy.de§7!",
            "",
            "§7Um den Server nutzen zu können,",
            "§7musst du unseren Nutzungsbedingungen",
            "§7zustimmen.",
            "",
            "§7» §ehaaremy.de/agb",
            "",
            "§7Klicke auf §a§lAKZEPTIEREN §7um fortzufahren."
        ));
        info.setItemMeta(infoMeta);
        inv.setItem(4, info);

        // Accept button (slot 11 - green wool)
        ItemStack accept = new ItemStack(Material.GREEN_WOOL);
        ItemMeta acceptMeta = accept.getItemMeta();
        acceptMeta.setDisplayName("§a§lAKZEPTIEREN");
        acceptMeta.setLore(List.of("§7Ich stimme den Nutzungsbedingungen zu."));
        accept.setItemMeta(acceptMeta);
        inv.setItem(11, accept);

        // Decline button (slot 15 - red wool) → kick
        ItemStack decline = new ItemStack(Material.RED_WOOL);
        ItemMeta declineMeta = decline.getItemMeta();
        declineMeta.setDisplayName("§c§lABLEHNEN");
        declineMeta.setLore(List.of("§7Du wirst vom Server getrennt."));
        decline.setItemMeta(declineMeta);
        inv.setItem(15, decline);

        pendingAcceptance.add(player.getUniqueId());
        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!pendingAcceptance.contains(player.getUniqueId())) return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot == 11) {
            // Accept
            pendingAcceptance.remove(player.getUniqueId());
            player.closeInventory();
            grantAgbPermission(player);
            player.sendMessage("§a§l✔ §aDu hast die Nutzungsbedingungen akzeptiert. Viel Spaß!");
        } else if (slot == 15) {
            // Decline
            pendingAcceptance.remove(player.getUniqueId());
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () ->
                player.kickPlayer("§cDu musst die Nutzungsbedingungen akzeptieren, um den Server zu nutzen.\n§7Komm gerne wieder, wenn du bereit bist."), 5L);
        }
    }

    private void grantAgbPermission(Player player) {
        luckPerms.getUserManager().modifyUser(player.getUniqueId(), user -> {
            user.data().add(Node.builder(AGB_PERMISSION).value(true).build());
        });
    }
}
