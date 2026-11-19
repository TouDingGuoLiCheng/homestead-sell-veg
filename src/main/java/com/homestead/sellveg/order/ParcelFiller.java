package com.homestead.sellveg.order;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class ParcelFiller {
    private ParcelFiller() {
    }

    public static List<NonNullList<ItemStack>> pack(List<ItemStack> lines, int slotsPerBox) {
        List<NonNullList<ItemStack>> boxes = new ArrayList<>();
        NonNullList<ItemStack> current = NonNullList.withSize(slotsPerBox, ItemStack.EMPTY);
        for (ItemStack line : lines) {
            ItemStack remaining = line.copy();
            while (!remaining.isEmpty()) {
                mergeInto(current, remaining);
                if (remaining.isEmpty()) {
                    break;
                }
                int empty = firstEmpty(current);
                if (empty >= 0) {
                    int take = Math.min(remaining.getMaxStackSize(), remaining.getCount());
                    current.set(empty, remaining.split(take));
                } else {
                    boxes.add(current);
                    current = NonNullList.withSize(slotsPerBox, ItemStack.EMPTY);
                }
            }
        }
        if (!isEmpty(current)) {
            boxes.add(current);
        }
        return boxes;
    }

    public static CompoundTag saveBox(NonNullList<ItemStack> items) {
        CompoundTag tag = new CompoundTag();
        ContainerHelper.saveAllItems(tag, items);
        return tag;
    }

    public static NonNullList<ItemStack> loadBox(CompoundTag tag, int slots) {
        NonNullList<ItemStack> items = NonNullList.withSize(slots, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);
        return items;
    }

    private static void mergeInto(NonNullList<ItemStack> box, ItemStack remaining) {
        for (ItemStack slot : box) {
            if (remaining.isEmpty()) {
                return;
            }
            if (slot.isEmpty() || !ItemStack.isSameItemSameTags(slot, remaining)) {
                continue;
            }
            int space = slot.getMaxStackSize() - slot.getCount();
            if (space <= 0) {
                continue;
            }
            int take = Math.min(space, remaining.getCount());
            slot.grow(take);
            remaining.shrink(take);
        }
    }

    private static int firstEmpty(NonNullList<ItemStack> box) {
        for (int i = 0; i < box.size(); i++) {
            if (box.get(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isEmpty(NonNullList<ItemStack> box) {
        for (ItemStack stack : box) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
