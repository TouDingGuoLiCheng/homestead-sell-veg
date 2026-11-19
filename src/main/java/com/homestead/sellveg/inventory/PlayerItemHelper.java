package com.homestead.sellveg.inventory;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class PlayerItemHelper {
    private PlayerItemHelper() {
    }

    public static int countMatching(Player player, ItemStack template) {
        int total = 0;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.items.size(); i++) {
            ItemStack stack = inv.items.get(i);
            if (ItemStack.isSameItemSameTags(stack, template)) {
                total += stack.getCount();
            }
        }
        ItemStack off = inv.offhand.get(0);
        if (ItemStack.isSameItemSameTags(off, template)) {
            total += off.getCount();
        }
        return total;
    }

    public static boolean takeMatching(Player player, ItemStack template, int count) {
        if (countMatching(player, template) < count) {
            return false;
        }
        int remaining = count;
        Inventory inv = player.getInventory();
        remaining = takeFromList(inv.items, template, remaining);
        if (remaining > 0) {
            remaining = takeFromList(inv.offhand, template, remaining);
        }
        return remaining == 0;
    }

    private static int takeFromList(net.minecraft.core.NonNullList<ItemStack> list, ItemStack template, int remaining) {
        for (int i = 0; i < list.size() && remaining > 0; i++) {
            ItemStack stack = list.get(i);
            if (!ItemStack.isSameItemSameTags(stack, template)) {
                continue;
            }
            int take = Math.min(remaining, stack.getCount());
            stack.shrink(take);
            remaining -= take;
        }
        return remaining;
    }

    public static void giveOrDrop(Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
