package com.homestead.sellveg.client;

import com.homestead.sellveg.item.QuickListerItem;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

final class QuickListScreens {
    private QuickListScreens() {
    }

    static boolean isQuickListReady(Player player) {
        if (player.containerMenu == null) {
            return player.getMainHandItem().getItem() instanceof QuickListerItem;
        }
        ItemStack carried = player.containerMenu.getCarried();
        if (carried.getItem() instanceof QuickListerItem) {
            return true;
        }
        return carried.isEmpty() && player.getMainHandItem().getItem() instanceof QuickListerItem;
    }

    @Nullable
    static Slot slotAt(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        int relX = (int) mouseX - screen.getGuiLeft();
        int relY = (int) mouseY - screen.getGuiTop();
        Slot hovered = screen.getSlotUnderMouse();
        if (hovered != null && inside(hovered, relX, relY)) {
            return hovered;
        }
        for (Slot slot : screen.getMenu().slots) {
            if (inside(slot, relX, relY)) {
                return slot;
            }
        }
        return null;
    }

    static int menuIndex(AbstractContainerScreen<?> screen, Slot slot) {
        if (slot.index >= 0 && slot.index < screen.getMenu().slots.size()
                && screen.getMenu().getSlot(slot.index) == slot) {
            return slot.index;
        }
        return screen.getMenu().slots.indexOf(slot);
    }

    private static boolean inside(Slot slot, int relX, int relY) {
        return relX >= slot.x - 1 && relX < slot.x + 17 && relY >= slot.y - 1 && relY < slot.y + 17;
    }
}
