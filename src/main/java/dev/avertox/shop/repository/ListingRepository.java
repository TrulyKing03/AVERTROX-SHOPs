package dev.avertox.shop.repository;

import dev.avertox.shop.domain.ListingType;
import dev.avertox.shop.domain.ShopListing;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ListingRepository {
    ShopListing save(ShopListing listing);

    Optional<ShopListing> findById(UUID listingId);

    void delete(UUID listingId);

    List<ShopListing> findByType(ListingType type);

    List<ShopListing> findBySeller(UUID sellerId);
}
