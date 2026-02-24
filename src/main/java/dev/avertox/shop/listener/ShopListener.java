package dev.avertox.shop.listener;

import dev.avertox.shop.gui.AvertoxMenuHolder;
import dev.avertox.shop.gui.MenuClickContext;
import dev.avertox.shop.gui.MenuController;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
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

    @EventHandler
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
        menuController.clearState(event.getPlayer().getUniqueId());
    }
}
