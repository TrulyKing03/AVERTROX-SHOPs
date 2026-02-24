package dev.avertox.shop.util;

import org.bukkit.Material;
import org.bukkit.inventory.CreativeCategory;
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
        CreativeCategory creativeCategory = material.getCreativeCategory();
        if (creativeCategory == null) {
            return "MISC";
        }
        return creativeCategory.name();
    }
}
