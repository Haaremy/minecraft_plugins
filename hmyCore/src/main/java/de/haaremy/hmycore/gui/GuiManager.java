package de.haaremy.hmycore.gui;

import de.haaremy.hmycore.HmyCore;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.function.Consumer;

public class GuiManager implements Listener {

    private final HmyCore plugin;

    public GuiManager(HmyCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof HmyGui gui) {
            event.setCancelled(true);

            int slot = event.getRawSlot();
            if (slot < 0 || slot >= event.getInventory().getSize()) {
                return;
            }

            Consumer<InventoryClickEvent> handler = gui.getClickHandler(slot);
            if (handler != null) {
                handler.accept(event);
            }
        }
    }
}
