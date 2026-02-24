package dev.avertox.shop.gui;

import dev.avertox.shop.domain.ListingType;
import dev.avertox.shop.domain.ShopListing;
import dev.avertox.shop.service.PermissionService;
import dev.avertox.shop.service.ServiceResult;
import dev.avertox.shop.service.ShopService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class MenuController {
    private static final int PAGE_SIZE = 45;
    private final JavaPlugin plugin;
    private final ShopService shopService;
    private final PermissionService permissionService;
    private final Map<UUID, PendingCreation> pendingCreations = new ConcurrentHashMap<>();
    private final Map<UUID, PendingInput> pendingInputs = new ConcurrentHashMap<>();

    public MenuController(JavaPlugin plugin, ShopService shopService, PermissionService permissionService) {
        this.plugin = plugin;
        this.shopService = shopService;
        this.permissionService = permissionService;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public void openMainMenu(Player player) {
        AvertoxMenuHolder holder = new AvertoxMenuHolder("main");
        Inventory inventory = Bukkit.createInventory(holder, 27, color("&6&lAvertox Shop"));
        holder.bind(inventory);

        inventory.setItem(4, named(Material.SUNFLOWER, "&e&lBalance", List.of("&7$" + ShopService.formatPrice(shopService.getBalance(player.getUniqueId())))));
        inventory.setItem(10, named(Material.CHEST, "&aAuction Buy", List.of("&7Browse player offers")));
        inventory.setItem(12, named(Material.EMERALD, "&2Sell Item", List.of("&7Create your own auction offer")));
        inventory.setItem(14, named(Material.ANVIL, "&6Manage Offers", List.of("&7Edit your price/name/stack")));
        inventory.setItem(16, named(Material.DIAMOND, "&bAdmin Shop", List.of("&7Buy from server offers")));
        if (permissionService.isAdmin(player.getUniqueId())) {
            inventory.setItem(22, named(Material.NETHER_STAR, "&cAdmin Offer Item", List.of("&7Create admin shop offer")));
        }

        holder.onClick(10, ctx -> openCreativeCategories(ctx.player(), ListingType.AUCTION, 0));
        holder.onClick(12, ctx -> openCreateOfferMenu(ctx.player(), ListingType.AUCTION));
        holder.onClick(14, ctx -> openManageOffers(ctx.player(), 0));
        holder.onClick(16, ctx -> openCreativeCategories(ctx.player(), ListingType.ADMIN_SHOP, 0));
        holder.onClick(22, ctx -> openCreateOfferMenu(ctx.player(), ListingType.ADMIN_SHOP));

        player.openInventory(inventory);
    }

    public void openCreativeCategories(Player player, ListingType type, int page) {
        Map<String, List<ShopListing>> grouped = shopService.groupedByCreativeCategory(type);
        List<String> categories = grouped.keySet().stream().sorted().toList();

        AvertoxMenuHolder holder = new AvertoxMenuHolder("categories");
        Inventory inventory = Bukkit.createInventory(holder, 54, color("&6&lAvertox " + (type == ListingType.AUCTION ? "Auction" : "AdminShop") + " Categories"));
        holder.bind(inventory);

        List<String> paged = page(categories, page);
        for (int i = 0; i < paged.size(); i++) {
            String category = paged.get(i);
            List<ShopListing> offers = grouped.getOrDefault(category, List.of());
            Material icon = offers.isEmpty() ? Material.CHEST : offers.get(0).getItem().getType();
            int slot = i;
            inventory.setItem(slot, named(icon, "&e" + category, List.of("&7Offers: &f" + offers.size(), "&8Click to open")));
            holder.onClick(slot, ctx -> openListingPage(ctx.player(), type, category, 0));
        }

        decoratePaging(holder, inventory, page, categories.size(), nextPage -> openCreativeCategories(player, type, nextPage), () -> openMainMenu(player));
        player.openInventory(inventory);
    }

    public void openListingPage(Player player, ListingType type, String category, int page) {
        List<ShopListing> listings = shopService.groupedByCreativeCategory(type).getOrDefault(category, List.of())
                .stream()
                .sorted(Comparator.comparingLong(ShopListing::getCreatedAt))
                .toList();

        AvertoxMenuHolder holder = new AvertoxMenuHolder("listings");
        Inventory inventory = Bukkit.createInventory(holder, 54, color("&6&lAvertox " + category + " P" + (page + 1)));
        holder.bind(inventory);

        List<ShopListing> paged = page(listings, page);
        for (int i = 0; i < paged.size(); i++) {
            ShopListing listing = paged.get(i);
            int slot = i;
            ItemStack display = listing.getItem().clone();
            ItemMeta meta = display.getItemMeta();
            List<String> lore = new ArrayList<>();
            lore.add(color("&7Offer: &f" + listing.getOfferName()));
            lore.add(color("&7Qty: &f" + listing.getQuantity()));
            lore.add(color("&7Price: &a$" + ShopService.formatPrice(listing.getPrice())));
            lore.add(color("&8ID: " + listing.getId().toString().substring(0, 8)));
            lore.add(color("&eClick to buy"));
            if (meta != null) {
                meta.setLore(lore);
                display.setItemMeta(meta);
            }
            inventory.setItem(slot, display);
            holder.onClick(slot, ctx -> {
                ServiceResult result = shopService.buyListing(ctx.player(), listing.getId());
                ctx.player().sendMessage(prefix(result.message(), result.success()));
                if (result.success()) {
                    openListingPage(ctx.player(), type, category, page);
                }
            });
        }

        decoratePaging(holder, inventory, page, listings.size(), nextPage -> openListingPage(player, type, category, nextPage), () -> openCreativeCategories(player, type, 0));
        player.openInventory(inventory);
    }

    public void openManageOffers(Player player, int page) {
        List<ShopListing> listings = shopService.getSellerListings(player.getUniqueId());
        AvertoxMenuHolder holder = new AvertoxMenuHolder("manage");
        Inventory inventory = Bukkit.createInventory(holder, 54, color("&6&lAvertox Manage Offers"));
        holder.bind(inventory);

        List<ShopListing> paged = page(listings, page);
        for (int i = 0; i < paged.size(); i++) {
            ShopListing listing = paged.get(i);
            int slot = i;
            ItemStack display = listing.getItem().clone();
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                meta.setLore(List.of(
                        color("&7Name: &f" + listing.getOfferName()),
                        color("&7Qty: &f" + listing.getQuantity()),
                        color("&7Price: &a$" + ShopService.formatPrice(listing.getPrice())),
                        color("&eClick to edit")
                ));
                display.setItemMeta(meta);
            }
            inventory.setItem(slot, display);
            holder.onClick(slot, ctx -> openOfferEditor(ctx.player(), listing.getId()));
        }

        decoratePaging(holder, inventory, page, listings.size(), nextPage -> openManageOffers(player, nextPage), () -> openMainMenu(player));
        player.openInventory(inventory);
    }

    public void openOfferEditor(Player player, UUID listingId) {
        ShopListing listing = shopService.findListing(listingId).orElse(null);
        if (listing == null) {
            player.sendMessage(prefix("Offer not found.", false));
            openManageOffers(player, 0);
            return;
        }

        AvertoxMenuHolder holder = new AvertoxMenuHolder("offer-edit");
        Inventory inventory = Bukkit.createInventory(holder, 27, color("&6&lEdit " + listing.getOfferName()));
        holder.bind(inventory);
        inventory.setItem(4, listing.getItem().clone());
        inventory.setItem(10, named(Material.GOLD_NUGGET, "&6Price -1", List.of("&7Current: $" + ShopService.formatPrice(listing.getPrice()))));
        inventory.setItem(11, named(Material.GOLD_INGOT, "&6Price +1", List.of("&7Current: $" + ShopService.formatPrice(listing.getPrice()))));
        inventory.setItem(12, named(Material.PAPER, "&fSet Price in Chat", List.of("&7Type a number in chat")));
        inventory.setItem(14, named(Material.NAME_TAG, "&bRename Offer", List.of("&7Type new name in chat")));
        inventory.setItem(15, named(Material.HOPPER, "&aAdd Stack From Hand", List.of("&7Main hand must match")));
        inventory.setItem(16, named(Material.BARRIER, "&cRemove Offer", List.of("&7Delete this listing")));
        inventory.setItem(22, named(Material.ARROW, "&7Back", List.of("&7Return to your offers")));

        holder.onClick(10, ctx -> {
            ServiceResult result = shopService.updateAuctionPrice(ctx.player(), listingId, Math.max(0.01, listing.getPrice() - 1.0));
            ctx.player().sendMessage(prefix(result.message(), result.success()));
            openOfferEditor(ctx.player(), listingId);
        });
        holder.onClick(11, ctx -> {
            ServiceResult result = shopService.updateAuctionPrice(ctx.player(), listingId, listing.getPrice() + 1.0);
            ctx.player().sendMessage(prefix(result.message(), result.success()));
            openOfferEditor(ctx.player(), listingId);
        });
        holder.onClick(12, ctx -> {
            pendingInputs.put(ctx.player().getUniqueId(), new PendingInput(PendingInputType.UPDATE_PRICE, listingId));
            ctx.player().closeInventory();
            ctx.player().sendMessage(prefix("Type the new price in chat.", true));
        });
        holder.onClick(14, ctx -> {
            pendingInputs.put(ctx.player().getUniqueId(), new PendingInput(PendingInputType.UPDATE_NAME, listingId));
            ctx.player().closeInventory();
            ctx.player().sendMessage(prefix("Type the new listing name in chat.", true));
        });
        holder.onClick(15, ctx -> {
            ServiceResult result = shopService.addToStack(ctx.player(), listingId, ctx.player().getInventory().getItemInMainHand());
            ctx.player().sendMessage(prefix(result.message(), result.success()));
            openOfferEditor(ctx.player(), listingId);
        });
        holder.onClick(16, ctx -> {
            ServiceResult result = shopService.removeListing(ctx.player(), listingId);
            ctx.player().sendMessage(prefix(result.message(), result.success()));
            openManageOffers(ctx.player(), 0);
        });
        holder.onClick(22, ctx -> openManageOffers(ctx.player(), 0));
        player.openInventory(inventory);
    }

    public void openCreateOfferMenu(Player player, ListingType type) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() == Material.AIR) {
            player.sendMessage(prefix("Hold the item in your main hand first.", false));
            return;
        }
        if (type == ListingType.ADMIN_SHOP && !permissionService.isAdmin(player.getUniqueId())) {
            player.sendMessage(prefix("Only admins can create admin shop offers.", false));
            return;
        }

        PendingCreation creation = pendingCreations.computeIfAbsent(player.getUniqueId(), id -> {
            String defaultName = hand.getType().name().toLowerCase().replace("_", " ");
            return new PendingCreation(type, hand.clone(), hand.getAmount(), defaultName, 10.0);
        });
        creation.type = type;
        if (!hand.isSimilar(creation.item)) {
            creation.item = hand.clone();
            creation.quantity = hand.getAmount();
        }

        AvertoxMenuHolder holder = new AvertoxMenuHolder("create");
        Inventory inventory = Bukkit.createInventory(holder, 27, color("&6&lAvertox Create Offer"));
        holder.bind(inventory);
        inventory.setItem(4, hand.clone());
        inventory.setItem(10, named(Material.GOLD_NUGGET, "&6Price -1", List.of("&7Current: $" + ShopService.formatPrice(creation.price))));
        inventory.setItem(11, named(Material.GOLD_INGOT, "&6Price +1", List.of("&7Current: $" + ShopService.formatPrice(creation.price))));
        inventory.setItem(12, named(Material.PAPER, "&fSet Price in Chat", List.of("&7Current: $" + ShopService.formatPrice(creation.price))));
        inventory.setItem(14, named(Material.NAME_TAG, "&bSet Offer Name", List.of("&7Current: " + creation.offerName)));
        inventory.setItem(15, named(Material.HOPPER, "&aQuantity -1", List.of("&7Current: " + creation.quantity, "&7Held: " + hand.getAmount())));
        inventory.setItem(16, named(Material.CHEST_MINECART, "&aQuantity +1", List.of("&7Current: " + creation.quantity, "&7Held: " + hand.getAmount())));
        inventory.setItem(21, named(Material.PAPER, "&fSet Quantity in Chat", List.of("&7Current: " + creation.quantity)));
        inventory.setItem(22, named(Material.EMERALD_BLOCK, "&aConfirm Offer", List.of("&7Create listing")));

        holder.onClick(10, ctx -> {
            creation.price = Math.max(0.01, creation.price - 1.0);
            openCreateOfferMenu(ctx.player(), type);
        });
        holder.onClick(11, ctx -> {
            creation.price += 1.0;
            openCreateOfferMenu(ctx.player(), type);
        });
        holder.onClick(12, ctx -> {
            pendingInputs.put(ctx.player().getUniqueId(), new PendingInput(PendingInputType.CREATE_PRICE, null));
            ctx.player().closeInventory();
            ctx.player().sendMessage(prefix("Type the offer price in chat.", true));
        });
        holder.onClick(14, ctx -> {
            pendingInputs.put(ctx.player().getUniqueId(), new PendingInput(PendingInputType.CREATE_NAME, null));
            ctx.player().closeInventory();
            ctx.player().sendMessage(prefix("Type the offer name in chat.", true));
        });
        holder.onClick(15, ctx -> {
            creation.quantity = Math.max(1, creation.quantity - 1);
            openCreateOfferMenu(ctx.player(), type);
        });
        holder.onClick(16, ctx -> {
            ItemStack currentHand = ctx.player().getInventory().getItemInMainHand();
            int max = currentHand.getType() == Material.AIR ? 1 : currentHand.getAmount();
            creation.quantity = Math.min(Math.max(1, max), creation.quantity + 1);
            openCreateOfferMenu(ctx.player(), type);
        });
        holder.onClick(21, ctx -> {
            pendingInputs.put(ctx.player().getUniqueId(), new PendingInput(PendingInputType.CREATE_QUANTITY, null));
            ctx.player().closeInventory();
            ctx.player().sendMessage(prefix("Type the offer quantity in chat.", true));
        });
        holder.onClick(22, ctx -> confirmCreation(ctx.player()));
        player.openInventory(inventory);
    }

    private void confirmCreation(Player player) {
        PendingCreation creation = pendingCreations.get(player.getUniqueId());
        if (creation == null) {
            player.sendMessage(prefix("No creation session available.", false));
            return;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() == Material.AIR || !hand.isSimilar(creation.item) || hand.getAmount() < creation.quantity) {
            player.sendMessage(prefix("Main hand item changed or quantity is too low.", false));
            return;
        }

        ServiceResult result;
        if (creation.type == ListingType.AUCTION) {
            result = shopService.createAuctionListing(player, creation.item, creation.quantity, creation.offerName, creation.price);
        } else {
            result = shopService.createAdminListing(player, creation.item, creation.quantity, creation.offerName, creation.price);
        }
        if (result.success()) {
            hand.setAmount(hand.getAmount() - creation.quantity);
            player.getInventory().setItemInMainHand(hand.getAmount() <= 0 ? null : hand);
            pendingCreations.remove(player.getUniqueId());
            player.sendMessage(prefix(result.message(), true));
            openMainMenu(player);
            return;
        }
        player.sendMessage(prefix(result.message(), false));
    }

    public boolean hasPendingInput(UUID playerId) {
        return pendingInputs.containsKey(playerId);
    }

    public void handleChatInput(Player player, String message) {
        PendingInput pendingInput = pendingInputs.remove(player.getUniqueId());
        if (pendingInput == null) {
            return;
        }

        switch (pendingInput.type) {
            case UPDATE_NAME -> {
                if (pendingInput.listingId == null) {
                    player.sendMessage(prefix("Listing not found.", false));
                    return;
                }
                ServiceResult result = shopService.updateAuctionName(player, pendingInput.listingId, message);
                player.sendMessage(prefix(result.message(), result.success()));
                openOfferEditor(player, pendingInput.listingId);
            }
            case UPDATE_PRICE -> {
                if (pendingInput.listingId == null) {
                    player.sendMessage(prefix("Listing not found.", false));
                    return;
                }
                double value = parsePrice(message);
                if (value <= 0) {
                    player.sendMessage(prefix("Invalid number.", false));
                    openOfferEditor(player, pendingInput.listingId);
                    return;
                }
                ServiceResult result = shopService.updateAuctionPrice(player, pendingInput.listingId, value);
                player.sendMessage(prefix(result.message(), result.success()));
                openOfferEditor(player, pendingInput.listingId);
            }
            case CREATE_NAME -> {
                PendingCreation creation = pendingCreations.get(player.getUniqueId());
                if (creation == null) {
                    player.sendMessage(prefix("No creation session available.", false));
                    return;
                }
                creation.offerName = message;
                openCreateOfferMenu(player, creation.type);
            }
            case CREATE_PRICE -> {
                PendingCreation creation = pendingCreations.get(player.getUniqueId());
                if (creation == null) {
                    player.sendMessage(prefix("No creation session available.", false));
                    return;
                }
                double value = parsePrice(message);
                if (value <= 0) {
                    player.sendMessage(prefix("Invalid number.", false));
                    openCreateOfferMenu(player, creation.type);
                    return;
                }
                creation.price = value;
                openCreateOfferMenu(player, creation.type);
            }
            case CREATE_QUANTITY -> {
                PendingCreation creation = pendingCreations.get(player.getUniqueId());
                if (creation == null) {
                    player.sendMessage(prefix("No creation session available.", false));
                    return;
                }
                int value = parseQuantity(message);
                if (value <= 0) {
                    player.sendMessage(prefix("Quantity must be a whole number greater than 0.", false));
                    openCreateOfferMenu(player, creation.type);
                    return;
                }
                creation.quantity = value;
                player.sendMessage(prefix("Quantity updated to " + value + ".", true));
                openCreateOfferMenu(player, creation.type);
            }
        }
    }

    public void handleInventoryClick(Player player, int slot) {
        if (!(player.getOpenInventory().getTopInventory().getHolder() instanceof AvertoxMenuHolder holder)) {
            return;
        }
        if (holder.actionFor(slot) != null) {
            holder.actionFor(slot).accept(new MenuClickContext(player, null));
        }
    }

    public void clearState(UUID playerId) {
        pendingInputs.remove(playerId);
        pendingCreations.remove(playerId);
    }

    private static ItemStack named(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));
            meta.setLore(lore.stream().map(MenuController::color).collect(Collectors.toList()));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private static String prefix(String msg, boolean ok) {
        return color((ok ? "&a[Avertox] " : "&c[Avertox] ") + msg);
    }

    private static <T> List<T> page(List<T> list, int page) {
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, list.size());
        if (start >= list.size()) {
            return List.of();
        }
        return list.subList(start, end);
    }

    private static void decoratePaging(AvertoxMenuHolder holder, Inventory inv, int page, int totalItems,
                                       java.util.function.IntConsumer nextHandler, Runnable backHandler) {
        int pageCount = Math.max(1, (int) Math.ceil((double) totalItems / PAGE_SIZE));
        inv.setItem(49, named(Material.BARRIER, "&cBack", List.of("&7Return")));
        holder.onClick(49, ctx -> backHandler.run());

        if (page > 0) {
            inv.setItem(45, named(Material.ARROW, "&ePrevious", List.of("&7Page " + page + "/" + pageCount)));
            holder.onClick(45, ctx -> nextHandler.accept(page - 1));
        }
        if (page + 1 < pageCount) {
            inv.setItem(53, named(Material.ARROW, "&eNext", List.of("&7Page " + (page + 2) + "/" + pageCount)));
            holder.onClick(53, ctx -> nextHandler.accept(page + 1));
        }
    }

    private static double parsePrice(String message) {
        try {
            return Double.parseDouble(message.trim());
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static int parseQuantity(String message) {
        try {
            return Integer.parseInt(message.trim());
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private enum PendingInputType {
        UPDATE_NAME,
        UPDATE_PRICE,
        CREATE_NAME,
        CREATE_PRICE,
        CREATE_QUANTITY
    }

    private static class PendingInput {
        private final PendingInputType type;
        private final UUID listingId;

        private PendingInput(PendingInputType type, UUID listingId) {
            this.type = type;
            this.listingId = listingId;
        }
    }

    private static class PendingCreation {
        private ListingType type;
        private ItemStack item;
        private int quantity;
        private String offerName;
        private double price;

        private PendingCreation(ListingType type, ItemStack item, int quantity, String offerName, double price) {
            this.type = type;
            this.item = item;
            this.quantity = quantity;
            this.offerName = offerName;
            this.price = price;
        }
    }
}
