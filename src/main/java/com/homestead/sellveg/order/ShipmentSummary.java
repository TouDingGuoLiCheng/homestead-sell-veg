package com.homestead.sellveg.order;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.Set;

/** Helpers for logistics UI rows built from a shipment box. */
public final class ShipmentSummary {
    private ShipmentSummary() {
    }

    /**
     * First non-empty stack, with {@link ItemStack#getCount()} set to the total of all
     * matching stacks in the box (so 2×64 iron shows as 128, not 64).
     */
    public static ItemStack primaryStack(NonNullList<ItemStack> contents) {
        if (contents == null) {
            return ItemStack.EMPTY;
        }
        ItemStack first = ItemStack.EMPTY;
        for (ItemStack stack : contents) {
            if (stack != null && !stack.isEmpty()) {
                first = stack;
                break;
            }
        }
        if (first.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int n = contents.size();
        int[] counts = new int[n];
        int[] kinds = new int[n];
        for (int i = 0; i < n; i++) {
            ItemStack stack = contents.get(i);
            if (stack == null || stack.isEmpty() || !ItemStack.isSameItemSameTags(stack, first)) {
                continue;
            }
            counts[i] = stack.getCount();
            kinds[i] = 1;
        }
        int total = ShipmentCountMath.primaryKindTotal(counts, kinds);
        ItemStack out = first.copy();
        out.setCount(Math.max(1, total));
        return out;
    }

    /** Distinct non-empty item types in the box. */
    public static int kindCount(NonNullList<ItemStack> contents) {
        if (contents == null) {
            return 0;
        }
        Set<Item> kinds = new HashSet<>();
        for (ItemStack stack : contents) {
            if (stack != null && !stack.isEmpty()) {
                kinds.add(stack.getItem());
            }
        }
        return kinds.size();
    }

    /**
     * Whole minutes remaining (ceil). {@code 0} ticks → {@code 0};
     * any positive remainder less than a minute still shows {@code 1}.
     */
    public static int etaMinutes(int remainingTicks) {
        if (remainingTicks <= 0) {
            return 0;
        }
        return Math.max(1, (remainingTicks + 1199) / 1200);
    }
}
