package de.haaremy.hmycore.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class HmyGuiBuilder {

    private Component title = Component.text("Menu");
    private int rows = 3;
    private final Map<Integer, ItemStack> items = new HashMap<>();
    private final Map<Integer, Consumer<InventoryClickEvent>> handlers = new HashMap<>();
    private final Map<Integer, String> itemPermissions = new HashMap<>();

    public HmyGuiBuilder setTitle(Component title) {
        this.title = title;
        return this;
    }

    public HmyGuiBuilder setRows(int rows) {
        this.rows = Math.max(1, Math.min(6, rows));
        return this;
    }

    public HmyGuiBuilder setItem(int slot, ItemStack item, Consumer<InventoryClickEvent> handler) {
        items.put(slot, item);
        if (handler != null) {
            handlers.put(slot, handler);
        }
        return this;
    }

    public HmyGuiBuilder setItem(int slot, ItemStack item) {
        return setItem(slot, item, null);
    }

    /**
     * Setzt ein Item, das nur fuer Spieler mit der angegebenen Permission sichtbar ist.
     * Wird beim {@link #buildFor(Player)} bzw. {@link #open(Player)} gefiltert.
     * Wenn permission null/leer ist, verhaelt sich die Methode wie {@link #setItem}.
     */
    public HmyGuiBuilder setItemIfPermitted(int slot, String permission, ItemStack item, Consumer<InventoryClickEvent> handler) {
        items.put(slot, item);
        if (handler != null) {
            handlers.put(slot, handler);
        }
        if (permission != null && !permission.isEmpty()) {
            itemPermissions.put(slot, permission);
        }
        return this;
    }

    public HmyGuiBuilder setItemIfPermitted(int slot, String permission, ItemStack item) {
        return setItemIfPermitted(slot, permission, item, null);
    }

    /**
     * Liefert true, wenn der Slot fuer den Player sichtbar ist.
     * Ohne Permission-Bindung: immer true. Mit Permission: nur wenn Player vorhanden
     * und Permission gesetzt. Package-private fuer Unit-Tests.
     */
    boolean isSlotVisible(int slot, Player player) {
        String perm = itemPermissions.get(slot);
        if (perm == null) return true;
        return player != null && player.hasPermission(perm);
    }

    public HmyGui build() {
        return buildFor(null);
    }

    public HmyGui buildFor(Player player) {
        HmyGui gui = new HmyGui() {
            @Override
            public void setup() {
                // Items werden im Builder gesetzt
            }
        };
        gui.createInventory(title, rows);
        for (var entry : items.entrySet()) {
            int slot = entry.getKey();
            if (!isSlotVisible(slot, player)) {
                continue;
            }
            gui.getInventory().setItem(slot, entry.getValue());
            Consumer<InventoryClickEvent> handler = handlers.get(slot);
            if (handler != null) {
                gui.setClickHandler(slot, handler);
            }
        }
        return gui;
    }

    public void open(Player player) {
        HmyGui gui = buildFor(player);
        player.openInventory(gui.getInventory());
    }
}
