package dev.avertox.shop.service.money;

import java.util.UUID;

public interface MoneyService {
    boolean has(UUID playerId, double amount);

    boolean withdraw(UUID playerId, double amount, String reason);

    void deposit(UUID playerId, double amount, String reason);

    double getBalance(UUID playerId);
}
