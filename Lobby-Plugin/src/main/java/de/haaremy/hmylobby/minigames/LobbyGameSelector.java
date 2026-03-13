package de.haaremy.hmylobby.minigames;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Verwaltet die Punkt-Auswahl mit dem goldenen Schwert für Game-Creator.
 * Linksklick = Punkt 1, Rechtsklick = Punkt 2.
 */
public class LobbyGameSelector implements Listener {

    private final Map<UUID, Location[]> selections = new HashMap<>();

    public Location[] getSelection(Player player) {
        return selections.getOrDefault(player.getUniqueId(), new Location[2]);
    }

    public void clearSelection(Player player) {
        selections.remove(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("hmy.lobby.gamecreator")) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.GOLDEN_SWORD) return;
        if (event.getClickedBlock() == null) return;
        if (event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        event.setCancelled(true);

        Location[] sel = selections.computeIfAbsent(player.getUniqueId(), k -> new Location[2]);
        Location loc = event.getClickedBlock().getLocation();

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            sel[0] = loc;
            player.sendMessage(Component.text("§aPunkt §e1 §agesetzt: §7"
                    + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()));
        } else {
            sel[1] = loc;
            player.sendMessage(Component.text("§aPunkt §e2 §agesetzt: §7"
                    + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()));
            if (sel[0] != null) {
                player.sendMessage(Component.text("§7Beide Punkte gesetzt. Führe §e/lobbygame create tiktaktoe <name> <feld-id> §7aus."));
            }
        }
    }
}
