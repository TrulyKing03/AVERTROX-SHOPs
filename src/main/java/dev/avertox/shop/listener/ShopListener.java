package dev.avertox.shop.listener;

import dev.avertox.shop.gui.AvertoxMenuHolder;
import dev.avertox.shop.gui.MenuClickContext;
import dev.avertox.shop.gui.MenuController;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ShopListener implements Listener {

    private final MenuController menuController;

    public ShopListener(MenuController menuController) {
        this.menuController = menuController;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!(event.getView().getTopInventory().getHolder() instanceof AvertoxMenuHolder holder)) {
            if (event.getView().getTopInventory().getType() == InventoryType.ANVIL && menuController.hasPendingInput(player.getUniqueId())) {
                menuController.handleAnvilInputClick(event, player);
            }
            return;
        }
        if ("anvil-input".equals(holder.getMenuId()) || (event.getView().getTopInventory().getType() == InventoryType.ANVIL && menuController.hasPendingInput(player.getUniqueId()))) {
            menuController.handleAnvilInputClick(event, player);
            return;
        }
        if ("sell-input".equals(holder.getMenuId())) {
            menuController.handleSellInputClick(event, player, holder);
            return;
        }
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            event.setCancelled(true);
            return;
        }
        event.setCancelled(true);
        if (holder.actionFor(event.getSlot()) != null) {
            holder.actionFor(event.getSlot()).accept(new MenuClickContext(player, event));
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        menuController.handleSellInputDrag(event, player);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (!(event.getInventory().getHolder() instanceof AvertoxMenuHolder holder)) {
            return;
        }
        menuController.handleInventoryClose(player, event.getInventory(), holder);
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (!(event.getView().getPlayer() instanceof Player player)) {
            return;
        }
        if (!menuController.hasPendingInput(player.getUniqueId())) {
            return;
        }
        menuController.handlePrepareAnvil(event, player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!menuController.hasPendingInput(player.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        String message = event.getMessage();
        menuController.getPlugin().getServer().getScheduler().runTask(menuController.getPlugin(), () ->
                menuController.handleChatInput(player, message));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    @SuppressWarnings("deprecation")
    public void onLegacyChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!menuController.hasPendingInput(player.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        String message = event.getMessage();
        menuController.getPlugin().getServer().getScheduler().runTask(menuController.getPlugin(), () ->
                menuController.handleChatInput(player, message));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        menuController.clearState(event.getPlayer());
    }
}
