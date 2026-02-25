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
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
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
    private static final int SELL_INPUT_LAST_SLOT = 44;
    private final JavaPlugin plugin;
    private final ShopService shopService;
    private final PermissionService permissionService;
    private final Map<UUID, PendingCreation> pendingCreations = new ConcurrentHashMap<>();
    private final Map<UUID, PendingInput> pendingInputs = new ConcurrentHashMap<>();
    private final Map<UUID, SellInventorySession> sellSessions = new ConcurrentHashMap<>();
    private final Map<UUID, String> anvilTypedText = new ConcurrentHashMap<>();

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
        holder.onClick(12, ctx -> openSellInputMenu(ctx.player(), ListingType.AUCTION));
        holder.onClick(14, ctx -> openManageOffers(ctx.player(), 0));
        holder.onClick(16, ctx -> openCreativeCategories(ctx.player(), ListingType.ADMIN_SHOP, 0));
        holder.onClick(22, ctx -> openSellInputMenu(ctx.player(), ListingType.ADMIN_SHOP));

        player.openInventory(inventory);
    }

    public void openSellInputMenu(Player player, ListingType type) {
        if (type == ListingType.ADMIN_SHOP && !permissionService.isAdmin(player.getUniqueId())) {
            player.sendMessage(prefix("Only admins can create admin shop offers.", false));
            return;
        }
        cancelPendingCreation(player, false);

        AvertoxMenuHolder holder = new AvertoxMenuHolder("sell-input");
        Inventory inventory = Bukkit.createInventory(holder, 54, color("&6&lAvertox Sell Input"));
        holder.bind(inventory);

        for (int slot = 45; slot < 54; slot++) {
            inventory.setItem(slot, named(Material.GRAY_STAINED_GLASS_PANE, "&8", List.of()));
        }
        inventory.setItem(46, named(Material.BARRIER, "&cClear", List.of("&7Clear input slots")));
        inventory.setItem(49, named(Material.EMERALD_BLOCK, "&aProceed", List.of("&7Continue to pricing/name")));
        inventory.setItem(53, named(Material.ARROW, "&7Back", List.of("&7Return to main menu")));

        holder.onClick(46, ctx -> {
            returnSellInputItems(ctx.player(), inventory);
            ctx.player().sendMessage(prefix("Sell input cleared.", true));
        });
        holder.onClick(49, ctx -> proceedFromSellInput(ctx.player(), type, inventory));
        holder.onClick(53, ctx -> {
            returnSellInputItems(ctx.player(), inventory);
            sellSessions.remove(ctx.player().getUniqueId());
            openMainMenu(ctx.player());
        });

        sellSessions.put(player.getUniqueId(), new SellInventorySession(type));
        player.openInventory(inventory);
        player.sendMessage(prefix("Drag only one item type into the top area, then click Proceed.", true));
    }

    public void openCreativeCategories(Player player, ListingType type, int page) {
        Map<String, List<ShopListing>> grouped = shopService.groupedByCreativeCategory(type);
        List<String> categories = sortCreativeCategories(grouped.keySet().stream().toList());

        AvertoxMenuHolder holder = new AvertoxMenuHolder("categories");
        Inventory inventory = Bukkit.createInventory(holder, 54, color("&6&lAvertox " + (type == ListingType.AUCTION ? "Auction" : "AdminShop") + " Categories"));
        holder.bind(inventory);

        List<String> paged = page(categories, page);
        for (int i = 0; i < paged.size(); i++) {
            String category = paged.get(i);
            List<ShopListing> offers = grouped.getOrDefault(category, List.of());
            Material icon = defaultCategoryIcon(category);
            int slot = i;
            inventory.setItem(slot, named(icon, "&6&l" + prettyCategory(category), List.of("&7Creative Tab", "&7Offers: &f" + offers.size(), "&eClick to open")));
            holder.onClick(slot, ctx -> openListingPage(ctx.player(), type, category, 0));
        }

        for (int slot = 45; slot < 54; slot++) {
            inventory.setItem(slot, named(Material.ORANGE_STAINED_GLASS_PANE, "&8", List.of()));
        }
        inventory.setItem(50, named(Material.NETHER_STAR, "&6&lAvertox Categories", List.of("&7Pick a creative category")));
        inventory.setItem(48, named(Material.COMPASS, "&bRefresh", List.of("&7Reload live offers")));
        holder.onClick(48, ctx -> openCreativeCategories(ctx.player(), type, page));
        decoratePaging(holder, inventory, page, categories.size(), nextPage -> openCreativeCategories(player, type, nextPage), () -> openMainMenu(player));
        player.openInventory(inventory);
    }

    public void openListingPage(Player player, ListingType type, String category, int page) {
        List<ShopListing> listings = shopService.groupedByCreativeCategory(type).getOrDefault(category, List.of())
                .stream()
                .sorted(Comparator.comparingLong(ShopListing::getCreatedAt))
                .toList();

        AvertoxMenuHolder holder = new AvertoxMenuHolder("listings");
        Inventory inventory = Bukkit.createInventory(holder, 54, color("&6&lAvertox " + prettyCategory(category) + " P" + (page + 1)));
        holder.bind(inventory);

        List<ShopListing> paged = page(listings, page);
        for (int i = 0; i < paged.size(); i++) {
            ShopListing listing = paged.get(i);
            int slot = i;
            ItemStack display = listing.getItem().clone();
            ItemMeta meta = display.getItemMeta();
            double unitPrice = listing.getQuantity() > 0 ? (listing.getPrice() / listing.getQuantity()) : listing.getPrice();
            List<String> lore = new ArrayList<>();
            lore.add(color("&7Offer: &f" + listing.getOfferName()));
            lore.add(color("&7Qty: &f" + listing.getQuantity()));
            lore.add(color("&7Price: &a$" + ShopService.formatPrice(listing.getPrice())));
            lore.add(color("&7Unit: &a$" + ShopService.formatPrice(unitPrice)));
            lore.add(color("&8ID: " + listing.getId().toString().substring(0, 8)));
            lore.add(color("&eClick to choose quantity"));
            if (meta != null) {
                meta.setLore(lore);
                display.setItemMeta(meta);
            }
            inventory.setItem(slot, display);
            holder.onClick(slot, ctx -> openBuyQuantityMenu(ctx.player(), type, category, page, listing.getId()));
        }

        inventory.setItem(48, named(Material.COMPASS, "&bRefresh", List.of("&7Reload live offers")));
        holder.onClick(48, ctx -> openListingPage(ctx.player(), type, category, page));
        decoratePaging(holder, inventory, page, listings.size(), nextPage -> openListingPage(player, type, category, nextPage), () -> openCreativeCategories(player, type, 0));
        player.openInventory(inventory);
    }

    private void openBuyQuantityMenu(Player player, ListingType type, String category, int page, UUID listingId) {
        ShopListing listing = shopService.findListing(listingId).orElse(null);
        if (listing == null) {
            player.sendMessage(prefix("This offer is no longer available.", false));
            openListingPage(player, type, category, page);
            return;
        }

        AvertoxMenuHolder holder = new AvertoxMenuHolder("buy-qty");
        Inventory inventory = Bukkit.createInventory(holder, 27, color("&6&lChoose Quantity"));
        holder.bind(inventory);

        ItemStack preview = listing.getItem().clone();
        ItemMeta previewMeta = preview.getItemMeta();
        if (previewMeta != null) {
            double unitPrice = listing.getQuantity() > 0 ? (listing.getPrice() / listing.getQuantity()) : listing.getPrice();
            previewMeta.setLore(List.of(
                    color("&7Offer: &f" + listing.getOfferName()),
                    color("&7Available: &f" + listing.getQuantity()),
                    color("&7Unit: &a$" + ShopService.formatPrice(unitPrice))
            ));
            preview.setItemMeta(previewMeta);
        }
        inventory.setItem(4, preview);

        addBuyOption(holder, inventory, 10, listing, 1, type, category, page);
        addBuyOption(holder, inventory, 12, listing, 32, type, category, page);
        addBuyOption(holder, inventory, 14, listing, 64, type, category, page);
        addBuyOption(holder, inventory, 16, listing, listing.getQuantity(), type, category, page);
        inventory.setItem(22, named(Material.ARROW, "&7Back", List.of("&7Return to offers")));
        holder.onClick(22, ctx -> openListingPage(ctx.player(), type, category, page));

        player.openInventory(inventory);
    }

    private void addBuyOption(AvertoxMenuHolder holder, Inventory inventory, int slot, ShopListing listing, int quantity,
                              ListingType type, String category, int page) {
        boolean available = quantity > 0 && quantity <= listing.getQuantity();
        int finalQuantity = Math.max(1, Math.min(quantity, listing.getQuantity()));
        double cost = shopService.calculatePurchaseCost(listing, finalQuantity);

        Material icon = available ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
        String label = quantity == listing.getQuantity() ? "All" : String.valueOf(quantity);
        inventory.setItem(slot, named(icon,
                available ? "&aBuy " + label : "&7Buy " + label,
                List.of("&7Cost: &a$" + ShopService.formatPrice(cost), available ? "&eClick to confirm" : "&cNot enough stock")));

        if (!available) {
            return;
        }
        holder.onClick(slot, ctx -> {
            ServiceResult result = shopService.buyListingQuantity(ctx.player(), listing.getId(), finalQuantity);
            ctx.player().sendMessage(prefix(result.message(), result.success()));
            if (result.success()) {
                openListingPage(ctx.player(), type, category, page);
            } else {
                openBuyQuantityMenu(ctx.player(), type, category, page, listing.getId());
            }
        });
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
        inventory.setItem(12, named(Material.PAPER, "&fSet Price (Chat)", List.of("&7Type value in private chat input")));
        inventory.setItem(14, named(Material.NAME_TAG, "&bRename Offer (Chat)", List.of("&7Type value in private chat input")));
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
            ctx.player().sendMessage(prefix("Type the new offer name in chat.", true));
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
        if (type == ListingType.ADMIN_SHOP && !permissionService.isAdmin(player.getUniqueId())) {
            player.sendMessage(prefix("Only admins can create admin shop offers.", false));
            return;
        }

        PendingCreation creation = pendingCreations.get(player.getUniqueId());
        if (creation == null) {
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand.getType() == Material.AIR) {
                player.sendMessage(prefix("Hold an item or use the sell input first.", false));
                return;
            }
            creation = new PendingCreation(type, hand.clone(), hand.getAmount(), hand.getAmount(), defaultOfferName(hand), 10.0, null, false);
            pendingCreations.put(player.getUniqueId(), creation);
        }
        creation.type = type;
        PendingCreation current = creation;

        AvertoxMenuHolder holder = new AvertoxMenuHolder("create");
        Inventory inventory = Bukkit.createInventory(holder, 27, color("&6&lAvertox Create Offer"));
        holder.bind(inventory);
        inventory.setItem(4, current.item.clone());
        inventory.setItem(10, named(Material.GOLD_NUGGET, "&6Price -1", List.of("&7Current: $" + ShopService.formatPrice(current.price))));
        inventory.setItem(11, named(Material.GOLD_INGOT, "&6Price +1", List.of("&7Current: $" + ShopService.formatPrice(current.price))));
        inventory.setItem(12, named(Material.PAPER, "&fSet Price (Chat)", List.of("&7Current: $" + ShopService.formatPrice(current.price))));
        inventory.setItem(14, named(Material.NAME_TAG, "&bSet Offer Name (Chat)", List.of("&7Current: " + current.offerName)));
        inventory.setItem(15, named(Material.HOPPER, "&aQuantity -1", List.of("&7Current: " + current.quantity, "&7Max: " + current.maxQuantity)));
        inventory.setItem(16, named(Material.CHEST_MINECART, "&aQuantity +1", List.of("&7Current: " + current.quantity, "&7Max: " + current.maxQuantity)));
        inventory.setItem(21, named(Material.PAPER, "&fSet Quantity (Chat)", List.of("&7Current: " + current.quantity)));
        inventory.setItem(23, named(Material.BARRIER, "&cCancel Sell", List.of("&7Return reserved items")));
        inventory.setItem(22, named(Material.EMERALD_BLOCK, "&aConfirm Offer", List.of("&7Create listing")));

        holder.onClick(10, ctx -> {
            current.price = Math.max(0.01, current.price - 1.0);
            openCreateOfferMenu(ctx.player(), type);
        });
        holder.onClick(11, ctx -> {
            current.price += 1.0;
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
            current.quantity = Math.max(1, current.quantity - 1);
            openCreateOfferMenu(ctx.player(), type);
        });
        holder.onClick(16, ctx -> {
            current.quantity = Math.min(Math.max(1, current.maxQuantity), current.quantity + 1);
            openCreateOfferMenu(ctx.player(), type);
        });
        holder.onClick(21, ctx -> {
            pendingInputs.put(ctx.player().getUniqueId(), new PendingInput(PendingInputType.CREATE_QUANTITY, null));
            ctx.player().closeInventory();
            ctx.player().sendMessage(prefix("Type the offer quantity in chat.", true));
        });
        holder.onClick(23, ctx -> {
            cancelPendingCreation(ctx.player(), true);
            openMainMenu(ctx.player());
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
        if (creation.quantity > creation.maxQuantity) {
            player.sendMessage(prefix("Quantity cannot exceed reserved amount.", false));
            return;
        }

        ServiceResult result;
        if (creation.type == ListingType.AUCTION) {
            result = shopService.createAuctionListing(player, creation.item, creation.quantity, creation.offerName, creation.price);
        } else {
            result = shopService.createAdminListing(player, creation.item, creation.quantity, creation.offerName, creation.price);
        }
        if (result.success()) {
            if (!creation.reservedFromSellInput) {
                ItemStack hand = player.getInventory().getItemInMainHand();
                if (hand.getType() == Material.AIR || !hand.isSimilar(creation.item) || hand.getAmount() < creation.quantity) {
                    player.sendMessage(prefix("Main hand item changed before confirmation.", false));
                    return;
                }
                hand.setAmount(hand.getAmount() - creation.quantity);
                player.getInventory().setItemInMainHand(hand.getAmount() <= 0 ? null : hand);
            } else if (creation.quantity < creation.maxQuantity) {
                int toReturn = creation.maxQuantity - creation.quantity;
                ItemStack returned = creation.item.clone();
                returned.setAmount(toReturn);
                addToPlayerOrDrop(player, returned);
            }
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
                if (value > creation.maxQuantity) {
                    player.sendMessage(prefix("Quantity cannot be higher than " + creation.maxQuantity + ".", false));
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

    public void handleAnvilInputClick(InventoryClickEvent event, Player player) {
        if (event.getView().getTopInventory().getType() != InventoryType.ANVIL) {
            return;
        }
        if (!pendingInputs.containsKey(player.getUniqueId())) {
            return;
        }
        boolean isResult = event.getSlotType() == org.bukkit.event.inventory.InventoryType.SlotType.RESULT || event.getRawSlot() == 2;
        if (!isResult) {
            event.setCancelled(false);
            return;
        }
        event.setCancelled(true);

        String text = anvilTypedText.getOrDefault(player.getUniqueId(), readAnvilText(event.getView().getTopInventory()));
        if (text.isEmpty()) {
            player.sendMessage(prefix("Type a value in the anvil rename field first.", false));
            return;
        }
        handleAnvilSubmit(player, text);
    }

    public void handlePrepareAnvil(PrepareAnvilEvent event, Player player) {
        if (event.getInventory().getType() != InventoryType.ANVIL) {
            return;
        }
        if (!pendingInputs.containsKey(player.getUniqueId())) {
            return;
        }

        String typed = readRenameTextFromView(event.getView(), event.getInventory());
        if (typed == null) {
            typed = "";
        }
        typed = typed.trim();
        if (!typed.isEmpty()) {
            anvilTypedText.put(player.getUniqueId(), typed);
        }

        ItemStack result = new ItemStack(Material.PAPER);
        ItemMeta resultMeta = result.getItemMeta();
        if (resultMeta != null) {
            resultMeta.setDisplayName(typed.isEmpty() ? " " : typed);
            result.setItemMeta(resultMeta);
        }
        event.setResult(result);
    }

    public void handleSellInputClick(org.bukkit.event.inventory.InventoryClickEvent event, Player player, AvertoxMenuHolder holder) {
        Inventory top = event.getView().getTopInventory();
        int rawSlot = event.getRawSlot();
        int topSize = top.getSize();

        if (rawSlot < topSize) {
            if (rawSlot > SELL_INPUT_LAST_SLOT) {
                event.setCancelled(true);
                if (holder.actionFor(rawSlot) != null) {
                    holder.actionFor(rawSlot).accept(new MenuClickContext(player, event));
                }
                return;
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> sanitizeSellInput(player, top));
            return;
        }

        if (event.isShiftClick()) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) {
                return;
            }
            int remaining = moveToSellInput(top, clicked.clone(), player);
            if (remaining <= 0) {
                event.getClickedInventory().setItem(event.getSlot(), null);
            } else {
                clicked.setAmount(remaining);
                event.getClickedInventory().setItem(event.getSlot(), clicked);
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> sanitizeSellInput(player, top));
        }
    }

    public void handleSellInputDrag(org.bukkit.event.inventory.InventoryDragEvent event, Player player) {
        if (!(event.getView().getTopInventory().getHolder() instanceof AvertoxMenuHolder holder)) {
            return;
        }
        if (!"sell-input".equals(holder.getMenuId())) {
            return;
        }

        int topSize = event.getView().getTopInventory().getSize();
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot >= topSize) {
                continue;
            }
            if (rawSlot > SELL_INPUT_LAST_SLOT) {
                event.setCancelled(true);
                return;
            }
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> sanitizeSellInput(player, event.getView().getTopInventory()));
    }

    public void handleInventoryClose(Player player, Inventory inventory, AvertoxMenuHolder holder) {
        if (!"sell-input".equals(holder.getMenuId())) {
            if (inventory.getType() == InventoryType.ANVIL && pendingInputs.containsKey(player.getUniqueId())) {
                handleAnvilClose(player, inventory);
            }
            return;
        }
        SellInventorySession session = sellSessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        if (session.proceeding) {
            sellSessions.remove(player.getUniqueId());
            return;
        }
        returnSellInputItems(player, inventory);
        sellSessions.remove(player.getUniqueId());
    }

    public void handleAnvilInventoryClose(Player player, Inventory inventory) {
        if (inventory.getType() != InventoryType.ANVIL) {
            return;
        }
        if (!pendingInputs.containsKey(player.getUniqueId())) {
            return;
        }
        handleAnvilClose(player, inventory);
    }

    private void handleAnvilClose(Player player, Inventory inventory) {
        if (!pendingInputs.containsKey(player.getUniqueId())) {
            anvilTypedText.remove(player.getUniqueId());
            return;
        }
        String text = anvilTypedText.getOrDefault(player.getUniqueId(), readAnvilText(inventory));
        if (text.isEmpty()) {
            pendingInputs.remove(player.getUniqueId());
            anvilTypedText.remove(player.getUniqueId());
            return;
        }
        handleAnvilSubmit(player, text);
    }

    private void openAnvilInput(Player player, PendingInputType type, UUID listingId, String title, String seedText) {
        pendingInputs.put(player.getUniqueId(), new PendingInput(type, listingId));
        anvilTypedText.put(player.getUniqueId(), seedText);
        AvertoxMenuHolder holder = new AvertoxMenuHolder("anvil-input");
        Inventory inventory = Bukkit.createInventory(holder, InventoryType.ANVIL, color("&6&lAvertox " + title));
        holder.bind(inventory);

        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta meta = paper.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(seedText);
            paper.setItemMeta(meta);
        }
        inventory.setItem(0, paper);
        player.openInventory(inventory);
        player.sendMessage(prefix("Type the value in the anvil text field, then click output.", true));
    }

    private void handleAnvilSubmit(Player player, String text) {
        PendingInput pendingInput = pendingInputs.remove(player.getUniqueId());
        anvilTypedText.remove(player.getUniqueId());
        if (pendingInput == null) {
            return;
        }

        switch (pendingInput.type) {
            case UPDATE_NAME -> {
                if (pendingInput.listingId == null) {
                    player.sendMessage(prefix("Listing not found.", false));
                    return;
                }
                ServiceResult result = shopService.updateAuctionName(player, pendingInput.listingId, text);
                player.sendMessage(prefix(result.message(), result.success()));
                openOfferEditor(player, pendingInput.listingId);
            }
            case UPDATE_PRICE -> {
                if (pendingInput.listingId == null) {
                    player.sendMessage(prefix("Listing not found.", false));
                    return;
                }
                double value = parsePrice(text);
                if (value <= 0) {
                    player.sendMessage(prefix("Invalid price.", false));
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
                creation.offerName = text;
                openCreateOfferMenu(player, creation.type);
            }
            case CREATE_PRICE -> {
                PendingCreation creation = pendingCreations.get(player.getUniqueId());
                if (creation == null) {
                    player.sendMessage(prefix("No creation session available.", false));
                    return;
                }
                double value = parsePrice(text);
                if (value <= 0) {
                    player.sendMessage(prefix("Invalid price.", false));
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
                int value = parseQuantity(text);
                if (value <= 0) {
                    player.sendMessage(prefix("Quantity must be a whole number greater than 0.", false));
                    openCreateOfferMenu(player, creation.type);
                    return;
                }
                if (value > creation.maxQuantity) {
                    player.sendMessage(prefix("Quantity cannot be higher than " + creation.maxQuantity + ".", false));
                    openCreateOfferMenu(player, creation.type);
                    return;
                }
                creation.quantity = value;
                openCreateOfferMenu(player, creation.type);
            }
            default -> {
                // keep chat flow for quantity input only
                pendingInputs.put(player.getUniqueId(), pendingInput);
            }
        }
    }

    public void clearState(UUID playerId) {
        pendingInputs.remove(playerId);
        pendingCreations.remove(playerId);
        sellSessions.remove(playerId);
        anvilTypedText.remove(playerId);
    }

    public void clearState(Player player) {
        PendingCreation creation = pendingCreations.remove(player.getUniqueId());
        if (creation != null) {
            returnReservedItems(player, creation);
        }
        pendingInputs.remove(player.getUniqueId());
        sellSessions.remove(player.getUniqueId());
        anvilTypedText.remove(player.getUniqueId());
    }

    private void proceedFromSellInput(Player player, ListingType type, Inventory inventory) {
        ItemStack reference = null;
        int total = 0;
        List<ItemStack> reserved = new ArrayList<>();

        for (int slot = 0; slot <= SELL_INPUT_LAST_SLOT; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack == null || stack.getType() == Material.AIR) {
                continue;
            }
            if (reference == null) {
                reference = stack.clone();
                reference.setAmount(1);
            } else if (!stack.isSimilar(reference)) {
                player.sendMessage(prefix("Only one item type (with same meta) is allowed.", false));
                return;
            }
            total += stack.getAmount();
            reserved.add(stack.clone());
        }

        if (reference == null || total <= 0) {
            player.sendMessage(prefix("Add items first before proceeding.", false));
            return;
        }

        for (int slot = 0; slot <= SELL_INPUT_LAST_SLOT; slot++) {
            inventory.setItem(slot, null);
        }

        SellInventorySession session = sellSessions.get(player.getUniqueId());
        if (session != null) {
            session.proceeding = true;
        }

        PendingCreation old = pendingCreations.put(player.getUniqueId(),
                new PendingCreation(type, reference, total, total, defaultOfferName(reference), 10.0, reserved, true));
        if (old != null) {
            returnReservedItems(player, old);
        }
        openCreateOfferMenu(player, type);
    }

    private void sanitizeSellInput(Player player, Inventory inventory) {
        ItemStack reference = null;
        for (int slot = 0; slot <= SELL_INPUT_LAST_SLOT; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack == null || stack.getType() == Material.AIR) {
                continue;
            }
            if (reference == null) {
                reference = stack.clone();
                reference.setAmount(1);
                continue;
            }
            if (!stack.isSimilar(reference)) {
                inventory.setItem(slot, null);
                addToPlayerOrDrop(player, stack);
            }
        }
    }

    private int moveToSellInput(Inventory inventory, ItemStack moving, Player player) {
        ItemStack reference = null;
        for (int slot = 0; slot <= SELL_INPUT_LAST_SLOT; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack != null && stack.getType() != Material.AIR) {
                reference = stack.clone();
                reference.setAmount(1);
                break;
            }
        }

        if (reference != null && !moving.isSimilar(reference)) {
            player.sendMessage(prefix("Only one item type can be added.", false));
            return moving.getAmount();
        }

        int remaining = moving.getAmount();
        for (int slot = 0; slot <= SELL_INPUT_LAST_SLOT && remaining > 0; slot++) {
            ItemStack slotItem = inventory.getItem(slot);
            if (slotItem == null || slotItem.getType() == Material.AIR) {
                ItemStack placed = moving.clone();
                int put = Math.min(remaining, moving.getMaxStackSize());
                placed.setAmount(put);
                inventory.setItem(slot, placed);
                remaining -= put;
                continue;
            }
            if (!slotItem.isSimilar(moving) || slotItem.getAmount() >= slotItem.getMaxStackSize()) {
                continue;
            }
            int canPut = Math.min(remaining, slotItem.getMaxStackSize() - slotItem.getAmount());
            slotItem.setAmount(slotItem.getAmount() + canPut);
            remaining -= canPut;
        }

        moving.setAmount(remaining);
        return remaining;
    }

    private void returnSellInputItems(Player player, Inventory inventory) {
        for (int slot = 0; slot <= SELL_INPUT_LAST_SLOT; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack == null || stack.getType() == Material.AIR) {
                continue;
            }
            inventory.setItem(slot, null);
            addToPlayerOrDrop(player, stack);
        }
    }

    private void addToPlayerOrDrop(Player player, ItemStack item) {
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
        leftovers.values().forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
    }

    private void cancelPendingCreation(Player player, boolean notify) {
        PendingCreation creation = pendingCreations.remove(player.getUniqueId());
        if (creation != null) {
            returnReservedItems(player, creation);
            if (notify) {
                player.sendMessage(prefix("Sell process canceled and items returned.", true));
            }
        }
    }

    private void returnReservedItems(Player player, PendingCreation creation) {
        if (!creation.reservedFromSellInput || creation.reservedItems == null) {
            return;
        }
        for (ItemStack reserved : creation.reservedItems) {
            addToPlayerOrDrop(player, reserved);
        }
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

    private static String defaultOfferName(ItemStack item) {
        return item.getType().name().toLowerCase().replace("_", " ");
    }

    private static String prettyCategory(String category) {
        String[] parts = category.toLowerCase().split("_");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty()) {
                continue;
            }
            out.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
            if (i + 1 < parts.length) {
                out.append(" ");
            }
        }
        return out.toString().trim();
    }

    private static Material defaultCategoryIcon(String category) {
        return switch (category) {
            case "BUILDING_BLOCKS" -> Material.BRICKS;
            case "DECORATIONS" -> Material.FLOWER_POT;
            case "REDSTONE" -> Material.REDSTONE;
            case "TRANSPORTATION" -> Material.MINECART;
            case "FOOD" -> Material.APPLE;
            case "TOOLS" -> Material.IRON_PICKAXE;
            case "COMBAT" -> Material.IRON_SWORD;
            case "BREWING" -> Material.BREWING_STAND;
            default -> Material.CHEST;
        };
    }

    private static List<String> sortCreativeCategories(List<String> categories) {
        List<String> ordered = new ArrayList<>(List.of(
                "BUILDING_BLOCKS",
                "DECORATIONS",
                "REDSTONE",
                "TRANSPORTATION",
                "MISC",
                "FOOD",
                "TOOLS",
                "COMBAT",
                "BREWING"
        ));
        for (String category : categories) {
            if (!ordered.contains(category)) {
                ordered.add(category);
            }
        }
        return ordered;
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
            String normalized = message.trim().replace(",", "").replace(" ", "");
            normalized = normalized.replaceAll("[^0-9.\\-]", "");
            return Double.parseDouble(normalized);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static int parseQuantity(String message) {
        try {
            String normalized = message.trim()
                    .replace(",", "")
                    .replace(" ", "");
            return Integer.parseInt(normalized);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static String readAnvilText(Inventory top) {
        try {
            java.lang.reflect.Method method = top.getClass().getMethod("getRenameText");
            Object value = method.invoke(top);
            if (value instanceof String renameText && !renameText.trim().isEmpty()) {
                return renameText.trim();
            }
        } catch (Exception ignored) {
        }

        ItemStack result = top.getItem(2);
        String fromResult = readDisplayName(result);
        if (!fromResult.isEmpty()) {
            return fromResult;
        }

        ItemStack left = top.getItem(0);
        String fromLeft = readDisplayName(left);
        return fromLeft;
    }

    private static String readRenameTextFromView(org.bukkit.inventory.InventoryView view, Inventory top) {
        try {
            java.lang.reflect.Method method = view.getClass().getMethod("getRenameText");
            Object value = method.invoke(view);
            if (value instanceof String renameText && !renameText.trim().isEmpty()) {
                return renameText.trim();
            }
        } catch (Exception ignored) {
        }
        return readAnvilText(top);
    }

    private static String readDisplayName(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return "";
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return "";
        }
        String value = ChatColor.stripColor(meta.getDisplayName());
        return value == null ? "" : value.trim();
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
        private int maxQuantity;
        private String offerName;
        private double price;
        private List<ItemStack> reservedItems;
        private boolean reservedFromSellInput;

        private PendingCreation(ListingType type, ItemStack item, int quantity, int maxQuantity, String offerName, double price,
                                List<ItemStack> reservedItems, boolean reservedFromSellInput) {
            this.type = type;
            this.item = item;
            this.quantity = quantity;
            this.maxQuantity = maxQuantity;
            this.offerName = offerName;
            this.price = price;
            this.reservedItems = reservedItems;
            this.reservedFromSellInput = reservedFromSellInput;
        }
    }

    private static class SellInventorySession {
        private final ListingType type;
        private boolean proceeding;

        private SellInventorySession(ListingType type) {
            this.type = type;
        }
    }
}
