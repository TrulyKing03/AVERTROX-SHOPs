package dev.avertox.shop;

import dev.avertox.shop.command.AvertoxShopCommand;
import dev.avertox.shop.gui.MenuController;
import dev.avertox.shop.listener.ShopListener;
import dev.avertox.shop.repository.InMemoryListingRepository;
import dev.avertox.shop.repository.ListingRepository;
import dev.avertox.shop.service.PermissionService;
import dev.avertox.shop.service.ShopService;
import dev.avertox.shop.service.money.MoneyService;
import dev.avertox.shop.service.permission.BukkitPermissionService;
import dev.avertox.shop.service.money.SimpleMoneyService;
import org.bukkit.command.PluginCommand;
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

        getServer().getPluginManager().registerEvents(new ShopListener(menuController), this);
        getLogger().info("AvertoxShop enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("AvertoxShop disabled.");
    }
}
