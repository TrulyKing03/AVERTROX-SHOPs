package dev.avertox.shop.service.permission;

import dev.avertox.shop.service.PermissionService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class BukkitPermissionService implements PermissionService {
    @Override
    public boolean has(UUID playerId, String permission) {
        Player player = Bukkit.getPlayer(playerId);
        return player != null && player.hasPermission(permission);
    }

    @Override
    public boolean isAdmin(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        return player != null && (player.isOp() || player.hasPermission("avertox.shop.admin"));
    }
}
