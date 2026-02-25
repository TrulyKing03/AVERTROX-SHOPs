package dev.avertox.shop.service;

import dev.avertox.shop.domain.ListingType;
import dev.avertox.shop.domain.ShopListing;
import dev.avertox.shop.repository.ListingRepository;
import dev.avertox.shop.service.money.MoneyService;
import dev.avertox.shop.util.ItemUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class ShopService {
    private final ListingRepository listingRepository;
    private final MoneyService moneyService;
    private final PermissionService permissionService;

    public ShopService(ListingRepository listingRepository, MoneyService moneyService, PermissionService permissionService) {
        this.listingRepository = listingRepository;
        this.moneyService = moneyService;
        this.permissionService = permissionService;
    }

    public ServiceResult createAuctionListing(Player seller, ItemStack source, int quantity, String offerName, double price) {
        if (source == null || source.getType() == Material.AIR) {
            return ServiceResult.fail("Hold an item first.");
        }
        if (quantity <= 0 || price <= 0.0) {
            return ServiceResult.fail("Quantity and price must be greater than 0.");
        }
        ItemStack listedItem = ItemUtils.cleanAmount(source, 1);
        ShopListing listing = new ShopListing(UUID.randomUUID(), seller.getUniqueId(), ListingType.AUCTION, listedItem, quantity, offerName, price);
        listingRepository.save(listing);
        return ServiceResult.ok("Auction listing created.");
    }

    public ServiceResult createAdminListing(Player admin, ItemStack source, int quantity, String offerName, double price) {
        if (!permissionService.isAdmin(admin.getUniqueId())) {
            return ServiceResult.fail("You are not allowed to create admin shop offers.");
        }
        if (source == null || source.getType() == Material.AIR) {
            return ServiceResult.fail("Hold an item first.");
        }
        if (quantity <= 0 || price <= 0.0) {
            return ServiceResult.fail("Quantity and price must be greater than 0.");
        }
        ItemStack listedItem = ItemUtils.cleanAmount(source, 1);
        ShopListing listing = new ShopListing(UUID.randomUUID(), admin.getUniqueId(), ListingType.ADMIN_SHOP, listedItem, quantity, offerName, price);
        listingRepository.save(listing);
        return ServiceResult.ok("Admin listing created.");
    }

    public List<ShopListing> getAuctionListings() {
        return listingRepository.findByType(ListingType.AUCTION);
    }

    public List<ShopListing> getAdminListings() {
        return listingRepository.findByType(ListingType.ADMIN_SHOP);
    }

    public List<ShopListing> getSellerListings(UUID sellerId) {
        return listingRepository.findBySeller(sellerId).stream()
                .filter(listing -> listing.getType() == ListingType.AUCTION)
                .toList();
    }

    public Optional<ShopListing> findListing(UUID listingId) {
        return listingRepository.findById(listingId);
    }

    public ServiceResult buyListing(Player buyer, UUID listingId) {
        Optional<ShopListing> optionalListing = listingRepository.findById(listingId);
        if (optionalListing.isEmpty()) {
            return ServiceResult.fail("This offer is no longer available.");
        }
        return buyListingQuantity(buyer, listingId, optionalListing.get().getQuantity());
    }

    public ServiceResult buyListingQuantity(Player buyer, UUID listingId, int quantity) {
        Optional<ShopListing> optionalListing = listingRepository.findById(listingId);
        if (optionalListing.isEmpty()) {
            return ServiceResult.fail("This offer is no longer available.");
        }
        ShopListing listing = optionalListing.get();
        if (quantity <= 0) {
            return ServiceResult.fail("Quantity must be greater than 0.");
        }
        if (quantity > listing.getQuantity()) {
            return ServiceResult.fail("Not enough quantity available.");
        }

        double cost = calculatePurchaseCost(listing, quantity);
        if (!moneyService.withdraw(buyer.getUniqueId(), cost, "Avertox purchase")) {
            return ServiceResult.fail("You do not have enough money.");
        }

        if (listing.getType() == ListingType.AUCTION) {
            moneyService.deposit(listing.getSellerId(), cost, "Avertox sale");
        }

        List<ItemStack> toGive = ItemUtils.toStacks(listing.getItem(), quantity);
        for (ItemStack stack : toGive) {
            Map<Integer, ItemStack> leftovers = buyer.getInventory().addItem(stack);
            if (!leftovers.isEmpty()) {
                leftovers.values().forEach(item -> buyer.getWorld().dropItemNaturally(buyer.getLocation(), item));
            }
        }

        int remaining = listing.getQuantity() - quantity;
        if (remaining <= 0) {
            listingRepository.delete(listingId);
        } else {
            double unitPrice = listing.getPrice() / listing.getQuantity();
            listing.setQuantity(remaining);
            listing.setPrice(unitPrice * remaining);
            listingRepository.save(listing);
        }

        return ServiceResult.ok("Purchased " + quantity + "x " + listing.getOfferName() + " for $" + formatPrice(cost) + ".");
    }

    public ServiceResult removeListing(Player actor, UUID listingId) {
        Optional<ShopListing> optionalListing = listingRepository.findById(listingId);
        if (optionalListing.isEmpty()) {
            return ServiceResult.fail("Offer not found.");
        }
        ShopListing listing = optionalListing.get();
        boolean isOwner = actor.getUniqueId().equals(listing.getSellerId());
        boolean isAdmin = permissionService.isAdmin(actor.getUniqueId());

        if (!isOwner && !isAdmin) {
            return ServiceResult.fail("You can only remove your own listings.");
        }

        listingRepository.delete(listingId);
        return ServiceResult.ok("Offer removed.");
    }

    public ServiceResult updateAuctionPrice(Player actor, UUID listingId, double price) {
        if (price <= 0.0) {
            return ServiceResult.fail("Price must be > 0.");
        }
        Optional<ShopListing> optionalListing = listingRepository.findById(listingId);
        if (optionalListing.isEmpty()) {
            return ServiceResult.fail("Offer not found.");
        }
        ShopListing listing = optionalListing.get();
        if (listing.getType() != ListingType.AUCTION || !listing.getSellerId().equals(actor.getUniqueId())) {
            return ServiceResult.fail("You can only edit your own auction offers.");
        }
        listing.setPrice(price);
        listingRepository.save(listing);
        return ServiceResult.ok("Price updated.");
    }

    public ServiceResult updateAuctionName(Player actor, UUID listingId, String name) {
        if (name == null || name.isBlank()) {
            return ServiceResult.fail("Name cannot be empty.");
        }
        Optional<ShopListing> optionalListing = listingRepository.findById(listingId);
        if (optionalListing.isEmpty()) {
            return ServiceResult.fail("Offer not found.");
        }
        ShopListing listing = optionalListing.get();
        if (listing.getType() != ListingType.AUCTION || !listing.getSellerId().equals(actor.getUniqueId())) {
            return ServiceResult.fail("You can only edit your own auction offers.");
        }
        listing.setOfferName(name);
        listingRepository.save(listing);
        return ServiceResult.ok("Offer name updated.");
    }

    public ServiceResult addToStack(Player actor, UUID listingId, ItemStack sourceStack) {
        Optional<ShopListing> optionalListing = listingRepository.findById(listingId);
        if (optionalListing.isEmpty()) {
            return ServiceResult.fail("Offer not found.");
        }
        ShopListing listing = optionalListing.get();
        if (listing.getType() != ListingType.AUCTION || !listing.getSellerId().equals(actor.getUniqueId())) {
            return ServiceResult.fail("You can only edit your own auction offers.");
        }
        if (sourceStack == null || sourceStack.getType() == Material.AIR) {
            return ServiceResult.fail("Hold a matching stack in your main hand.");
        }
        if (!sourceStack.isSimilar(listing.getItem())) {
            return ServiceResult.fail("Main hand item must match listing item (including meta).");
        }
        if (sourceStack.getMaxStackSize() <= 1) {
            return ServiceResult.fail("This item is not stackable.");
        }

        listing.setQuantity(listing.getQuantity() + sourceStack.getAmount());
        listingRepository.save(listing);
        sourceStack.setAmount(0);
        actor.getInventory().setItemInMainHand(sourceStack);
        return ServiceResult.ok("Added stack to listing.");
    }

    public Map<String, List<ShopListing>> groupedByCreativeCategory(ListingType type) {
        return listingRepository.findByType(type).stream()
                .collect(Collectors.groupingBy(listing -> ItemUtils.creativeCategoryName(listing.getItem())));
    }

    public List<String> getCreativeCategories(ListingType type) {
        return new ArrayList<>(groupedByCreativeCategory(type).keySet().stream().sorted().toList());
    }

    public double getBalance(UUID playerId) {
        return moneyService.getBalance(playerId);
    }

    public static String formatPrice(double value) {
        return String.format("%.2f", value);
    }

    public double calculatePurchaseCost(ShopListing listing, int quantity) {
        if (listing.getQuantity() <= 0 || quantity <= 0) {
            return 0.0;
        }
        double unitPrice = listing.getPrice() / listing.getQuantity();
        return unitPrice * quantity;
    }
}
