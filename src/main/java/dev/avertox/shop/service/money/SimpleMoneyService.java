package dev.avertox.shop.service.money;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SimpleMoneyService implements MoneyService {
    private static final double START_BALANCE = 10000.0;
    private final Map<UUID, Double> balances = new ConcurrentHashMap<>();

    @Override
    public boolean has(UUID playerId, double amount) {
        return getBalance(playerId) >= amount;
    }

    @Override
    public boolean withdraw(UUID playerId, double amount, String reason) {
        double balance = getBalance(playerId);
        if (balance < amount) {
            return false;
        }
        balances.put(playerId, balance - amount);
        return true;
    }

    @Override
    public void deposit(UUID playerId, double amount, String reason) {
        balances.put(playerId, getBalance(playerId) + amount);
    }

    @Override
    public double getBalance(UUID playerId) {
        return balances.computeIfAbsent(playerId, id -> START_BALANCE);
    }
}
