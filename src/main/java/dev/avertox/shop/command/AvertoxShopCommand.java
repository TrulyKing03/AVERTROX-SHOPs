package dev.avertox.shop.command;

import dev.avertox.shop.gui.MenuController;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AvertoxShopCommand implements CommandExecutor {
    private final MenuController menuController;

    public AvertoxShopCommand(MenuController menuController) {
        this.menuController = menuController;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        menuController.openMainMenu(player);
        return true;
    }
}
