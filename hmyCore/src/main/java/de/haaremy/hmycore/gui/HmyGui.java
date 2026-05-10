package de.haaremy.hmycore.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.bukkit.event.inventory.InventoryClickEvent;

public abstract class HmyGui implements InventoryHolder {

    private Inventory inventory;
    private final Map<Integer, Consumer<InventoryClickEvent>> clickHandlers = new HashMap<>();

    protected void createInventory(Component title, int rows) {
        this.inventory = Bukkit.createInventory(this, rows * 9, title);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public void setClickHandler(int slot, Consumer<InventoryClickEvent> handler) {
        clickHandlers.put(slot, handler);
    }

    public Consumer<InventoryClickEvent> getClickHandler(int slot) {
        return clickHandlers.get(slot);
    }

    public boolean hasClickHandler(int slot) {
        return clickHandlers.containsKey(slot);
    }

    public abstract void setup();
}
