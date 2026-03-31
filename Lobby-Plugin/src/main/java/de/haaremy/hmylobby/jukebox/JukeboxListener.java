package de.haaremy.hmylobby.jukebox;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

public class JukeboxListener implements Listener {

    private final JukeboxManager manager;

    public JukeboxListener(JukeboxManager manager) {
        this.manager = manager;
    }

    /**
     * Handles golden-sword right-click for:
     * 1. Processing a pending jukebox/chest selection.
     * 2. Blocking interaction with managed jukeboxes for non-admins.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getClickedBlock() == null) return;

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        boolean holdsGoldSword = player.getInventory().getItemInMainHand().getType() == Material.GOLDEN_SWORD;

        // ── Selection mode ───────────────────────────────────────────────
        if (holdsGoldSword && manager.hasPendingSelection(player.getUniqueId())) {
            event.setCancelled(true);
            manager.handleSelection(player, block);
            return;
        }

        // ── Protect managed jukeboxes from interaction ───────────────────
        if (block.getType() == Material.JUKEBOX && manager.isManaged(block.getLocation())) {
            if (!player.hasPermission("hmy.lobby.jukebox.admin")) {
                event.setCancelled(true);
            }
        }
    }

    /** Cancel breaking of managed jukeboxes unless the player has admin permission. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.JUKEBOX) return;
        if (!manager.isManaged(block.getLocation())) return;
        if (!event.getPlayer().hasPermission("hmy.lobby.jukebox.admin")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cDiese Jukebox ist verwaltet und kann nicht abgebaut werden.");
        }
    }

    /** Clean up pending selections when a player leaves. */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        manager.clearPendingSelection(event.getPlayer().getUniqueId());
    }
}
