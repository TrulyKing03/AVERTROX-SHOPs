package dev.avertox.shop.domain;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class ShopListing {
    private final UUID id;
    private final UUID sellerId;
    private final ListingType type;
    private ItemStack item;
    private int quantity;
    private String offerName;
    private double price;
    private final long createdAt;

    public ShopListing(UUID id, UUID sellerId, ListingType type, ItemStack item, int quantity, String offerName, double price) {
        this.id = id;
        this.sellerId = sellerId;
        this.type = type;
        this.item = item;
        this.quantity = quantity;
        this.offerName = offerName;
        this.price = price;
        this.createdAt = System.currentTimeMillis();
    }

    public UUID getId() {
        return id;
    }

    public UUID getSellerId() {
        return sellerId;
    }

    public ListingType getType() {
        return type;
    }

    public ItemStack getItem() {
        return item;
    }

    public void setItem(ItemStack item) {
        this.item = item;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getOfferName() {
        return offerName;
    }

    public void setOfferName(String offerName) {
        this.offerName = offerName;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
