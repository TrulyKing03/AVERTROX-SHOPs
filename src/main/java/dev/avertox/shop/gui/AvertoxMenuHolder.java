package dev.avertox.shop.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class AvertoxMenuHolder implements InventoryHolder {
    private final Map<Integer, Consumer<MenuClickContext>> actions = new HashMap<>();
    private final String menuId;
    private Inventory inventory;

    public AvertoxMenuHolder(String menuId) {
        this.menuId = menuId;
    }

    public String getMenuId() {
        return menuId;
    }

    public void onClick(int slot, Consumer<MenuClickContext> action) {
        actions.put(slot, action);
    }

    public Consumer<MenuClickContext> actionFor(int slot) {
        return actions.get(slot);
    }

    public void bind(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
