package com.homestead.sellveg.item;

import com.homestead.sellveg.shop.QuickListService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Quick list tool: hold on cursor or in main hand, right-click a slot to list.
 */
public class QuickListerItem extends Item {
    public QuickListerItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack carried, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY || slot.getItem().isEmpty()) {
            return false;
        }
        // Prefer the client ScreenEvent + packet path; still handle vanilla click as fallback.
        if (player.level().isClientSide()) {
            return true;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        if (!slot.mayPickup(serverPlayer) || !slot.allowModification(serverPlayer)) {
            return true;
        }
        QuickListService.listFromSlot(serverPlayer, slot, player.isShiftKeyDown());
        serverPlayer.containerMenu.broadcastChanges();
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.homestead_sell_veg.quick_lister.lore"));
    }
}
