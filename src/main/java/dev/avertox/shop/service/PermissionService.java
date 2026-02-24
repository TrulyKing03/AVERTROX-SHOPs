package dev.avertox.shop.service;

import java.util.UUID;

public interface PermissionService {
    boolean has(UUID playerId, String permission);

    boolean isAdmin(UUID playerId);
}
