package dev.avertox.shop.repository;

import dev.avertox.shop.domain.ListingType;
import dev.avertox.shop.domain.ShopListing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryListingRepository implements ListingRepository {

    private final Map<UUID, ShopListing> listings = new ConcurrentHashMap<>();

    @Override
    public ShopListing save(ShopListing listing) {
        listings.put(listing.getId(), listing);
        return listing;
    }

    @Override
    public Optional<ShopListing> findById(UUID listingId) {
        return Optional.ofNullable(listings.get(listingId));
    }

    @Override
    public void delete(UUID listingId) {
        listings.remove(listingId);
    }

    @Override
    public List<ShopListing> findByType(ListingType type) {
        return listings.values().stream()
                .filter(l -> l.getType() == type)
                .sorted(Comparator.comparingLong(ShopListing::getCreatedAt))
                .toList();
    }

    @Override
    public List<ShopListing> findBySeller(UUID sellerId) {
        return new ArrayList<>(listings.values().stream()
                .filter(l -> l.getSellerId().equals(sellerId))
                .sorted(Comparator.comparingLong(ShopListing::getCreatedAt))
                .toList());
    }
}
