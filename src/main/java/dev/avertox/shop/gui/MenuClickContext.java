package dev.avertox.shop.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public record MenuClickContext(Player player, InventoryClickEvent event) {
}
