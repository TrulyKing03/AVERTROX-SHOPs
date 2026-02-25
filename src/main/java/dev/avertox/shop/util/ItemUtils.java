package dev.avertox.shop.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class ItemUtils {
    private ItemUtils() {
    }

    public static ItemStack cleanAmount(ItemStack source, int amount) {
        ItemStack clone = source.clone();
        clone.setAmount(amount);
        return clone;
    }

    public static List<ItemStack> toStacks(ItemStack base, int amount) {
        List<ItemStack> result = new ArrayList<>();
        int max = Math.max(base.getMaxStackSize(), 1);
        int remaining = amount;
        while (remaining > 0) {
            int size = Math.min(max, remaining);
            ItemStack stack = base.clone();
            stack.setAmount(size);
            result.add(stack);
            remaining -= size;
        }
        return result;
    }

    public static String creativeCategoryName(ItemStack itemStack) {
        Material material = itemStack.getType();
        String name = material.name();

        if (isBrewing(name)) return "BREWING";
        if (isFood(material, name)) return "FOOD";
        if (isCombat(name)) return "COMBAT";
        if (isTools(name)) return "TOOLS";
        if (isRedstone(name)) return "REDSTONE";
        if (isTransportation(name)) return "TRANSPORTATION";
        if (isDecoration(name)) return "DECORATIONS";
        if (isBuilding(name)) return "BUILDING_BLOCKS";
        return "MISC";
    }

    private static boolean isFood(Material material, String name) {
        return material.isEdible() || hasAny(name,
                "APPLE", "BREAD", "CARROT", "POTATO", "BEETROOT", "MELON_SLICE",
                "COOKED_", "RAW_", "BEEF", "PORKCHOP", "MUTTON", "CHICKEN", "RABBIT",
                "SALMON", "COD", "PUFFERFISH", "TROPICAL_FISH", "COOKIE", "PUMPKIN_PIE", "CAKE");
    }

    private static boolean isBrewing(String name) {
        return hasAny(name, "POTION", "BREWING", "BLAZE_POWDER", "GHAST_TEAR", "NETHER_WART",
                "GLISTERING_MELON_SLICE", "MAGMA_CREAM", "SPIDER_EYE", "FERMENTED_SPIDER_EYE",
                "GUNPOWDER", "DRAGON_BREATH", "PHANTOM_MEMBRANE", "RABBIT_FOOT", "GLASS_BOTTLE");
    }

    private static boolean isCombat(String name) {
        return hasAny(name, "SWORD", "AXE", "BOW", "CROSSBOW", "TRIDENT", "SHIELD",
                "HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS", "ARROW", "TOTEM");
    }

    private static boolean isTools(String name) {
        return hasAny(name, "PICKAXE", "SHOVEL", "HOE", "SHEARS", "FLINT_AND_STEEL",
                "FISHING_ROD", "COMPASS", "CLOCK", "SPYGLASS", "BRUSH", "BUCKET");
    }

    private static boolean isRedstone(String name) {
        return hasAny(name, "REDSTONE", "REPEATER", "COMPARATOR", "OBSERVER", "PISTON",
                "DISPENSER", "DROPPER", "HOPPER", "LEVER", "BUTTON", "PRESSURE_PLATE",
                "TRIPWIRE", "DAYLIGHT_DETECTOR", "TARGET", "SCULK_SENSOR", "NOTE_BLOCK");
    }

    private static boolean isTransportation(String name) {
        return hasAny(name, "MINECART", "RAIL", "BOAT", "CHEST_BOAT", "SADDLE",
                "CARROT_ON_A_STICK", "WARPED_FUNGUS_ON_A_STICK", "ELYTRA");
    }

    private static boolean isDecoration(String name) {
        return hasAny(name, "BANNER", "CARPET", "PAINTING", "ITEM_FRAME", "FLOWER", "SAPLING",
                "LEAVES", "POTTED", "CANDLE", "LANTERN", "SEA_LANTERN", "GLOW_LICHEN", "VINE",
                "SIGN", "HANGING_SIGN", "BED", "POT", "HEAD", "SKULL", "ARMOR_STAND");
    }

    private static boolean isBuilding(String name) {
        return hasAny(name, "PLANKS", "LOG", "WOOD", "STONE", "COBBLESTONE", "BRICKS", "TERRACOTTA",
                "CONCRETE", "WOOL", "GLASS", "STAIRS", "SLAB", "WALL", "FENCE", "FENCE_GATE",
                "DOOR", "TRAPDOOR", "ORE", "DEEPSLATE", "NETHERRACK", "BASALT", "BLACKSTONE",
                "END_STONE", "PRISMARINE", "SANDSTONE", "QUARTZ", "PURPUR");
    }

    private static boolean hasAny(String input, String... needles) {
        for (String needle : needles) {
            if (input.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
