package dev.avertox.shop.service;

public record ServiceResult(boolean success, String message) {
    public static ServiceResult ok(String message) {
        return new ServiceResult(true, message);
    }

    public static ServiceResult fail(String message) {
        return new ServiceResult(false, message);
    }
}
