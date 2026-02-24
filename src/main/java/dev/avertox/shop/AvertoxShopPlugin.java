package dev.avertox.shop;

import dev.avertox.shop.command.AvertoxShopCommand;
import dev.avertox.shop.gui.MenuController;
import dev.avertox.shop.listener.ShopListener;
import dev.avertox.shop.repository.InMemoryListingRepository;
import dev.avertox.shop.repository.ListingRepository;
import dev.avertox.shop.service.PermissionService;
import dev.avertox.shop.service.ShopService;
import dev.avertox.shop.service.money.MoneyService;
import dev.avertox.shop.service.money.SimpleMoneyService;
import dev.avertox.shop.service.permission.BukkitPermissionService;
import org.bukkit.entity.Player;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.java.JavaPlugin;

public class AvertoxShopPlugin extends JavaPlugin {

    private ShopService shopService;
    private MenuController menuController;

    @Override
    public void onEnable() {
        ListingRepository listingRepository = new InMemoryListingRepository();
        MoneyService moneyService = new SimpleMoneyService();
        PermissionService permissionService = new BukkitPermissionService();

        this.shopService = new ShopService(listingRepository, moneyService, permissionService);
        this.menuController = new MenuController(this, shopService, permissionService);

        PluginCommand command = getCommand("avertoxshop");
        if (command != null) {
            command.setExecutor(new AvertoxShopCommand(menuController));
        }

        ShopListener shopListener = new ShopListener(menuController);
        getServer().getPluginManager().registerEvents(shopListener, this);
        registerPaperAsyncChatBridge(shopListener);
        getLogger().info("AvertoxShop enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("AvertoxShop disabled.");
    }

    @SuppressWarnings("unchecked")
    private void registerPaperAsyncChatBridge(ShopListener shopListener) {
        try {
            Class<?> asyncChatRaw = Class.forName("io.papermc.paper.event.player.AsyncChatEvent");
            if (!Event.class.isAssignableFrom(asyncChatRaw)) {
                return;
            }
            Class<? extends Event> asyncChatEvent = (Class<? extends Event>) asyncChatRaw;

            getServer().getPluginManager().registerEvent(asyncChatEvent, shopListener, EventPriority.HIGHEST, (listener, event) -> {
                try {
                    Object playerObj = asyncChatEvent.getMethod("getPlayer").invoke(event);
                    if (!(playerObj instanceof Player player)) {
                        return;
                    }
                    if (!menuController.hasPendingInput(player.getUniqueId())) {
                        return;
                    }

                    if (event instanceof Cancellable cancellable) {
                        cancellable.setCancelled(true);
                    }

                    String message = readPaperChatMessage(asyncChatEvent, event);
                    getServer().getScheduler().runTask(this, () -> menuController.handleChatInput(player, message));
                } catch (Exception ignored) {
                }
            }, this, false);

            getLogger().info("Registered Paper AsyncChatEvent bridge for private shop input.");
        } catch (ClassNotFoundException ignored) {
        }
    }

    private String readPaperChatMessage(Class<? extends Event> asyncChatEvent, Event event) {
        try {
            Object messageComponent = asyncChatEvent.getMethod("message").invoke(event);
            Class<?> serializerClass = Class.forName("net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer");
            Object serializer = serializerClass.getMethod("plainText").invoke(null);
            Object result = serializerClass.getMethod("serialize", Class.forName("net.kyori.adventure.text.Component"))
                    .invoke(serializer, messageComponent);
            if (result != null) {
                return result.toString();
            }
        } catch (Exception ignored) {
        }

        try {
            Object legacy = asyncChatEvent.getMethod("getMessage").invoke(event);
            return legacy != null ? legacy.toString() : "";
        } catch (Exception ignored) {
        }
        return "";
    }
}
