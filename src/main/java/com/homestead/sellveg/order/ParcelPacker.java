package com.homestead.sellveg.order;

/**
 * Pure packing math: how many 27-slot boxes a purchase needs.
 */
public final class ParcelPacker {
    private ParcelPacker() {
    }

    public static int stacksForCount(int count, int maxStack) {
        if (count <= 0 || maxStack <= 0) {
            return 0;
        }
        return (count + maxStack - 1) / maxStack;
    }

    public static int boxesForStacks(int stacks, int slotsPerBox) {
        if (stacks <= 0 || slotsPerBox <= 0) {
            return 0;
        }
        return (stacks + slotsPerBox - 1) / slotsPerBox;
    }
}
