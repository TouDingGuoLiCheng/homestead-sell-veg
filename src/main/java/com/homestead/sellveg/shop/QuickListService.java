package com.homestead.sellveg.shop;

import com.homestead.sellveg.config.SellVegConfig;
import com.homestead.sellveg.data.PlayerNetShopData;
import com.homestead.sellveg.inventory.PlayerItemHelper;
import com.homestead.sellveg.market.MarketEvents;
import com.homestead.sellveg.market.PriceCatalog;
import com.homestead.sellveg.network.ShopFeedback;
import com.homestead.sellveg.network.ShopSyncPacket;
import com.homestead.sellveg.stock.StockPacker;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Server-side quick-list from a specific menu slot (scroll on cursor or in main hand).
 */
public final class QuickListService {
    private QuickListService() {
    }

    public static void listFromMenuSlot(ServerPlayer player, int containerId, int slotIndex, boolean wholeStack) {
        AbstractContainerMenu menu = player.containerMenu;
        if (menu.containerId != containerId) {
            return;
        }
        ItemStack carried = menu.getCarried();
        boolean cursorMode = carried.getItem() instanceof com.homestead.sellveg.item.QuickListerItem;
        boolean handMode = carried.isEmpty()
                && player.getMainHandItem().getItem() instanceof com.homestead.sellveg.item.QuickListerItem;
        if (!cursorMode && !handMode) {
            return;
        }
        if (slotIndex < 0 || slotIndex >= menu.slots.size()) {
            return;
        }
        Slot slot = menu.getSlot(slotIndex);
        if (!slot.mayPickup(player) || !slot.allowModification(player)) {
            feedback(player, "message.homestead_sell_veg.listing_not_tradeable");
            return;
        }
        listFromSlot(player, slot, wholeStack);
        menu.broadcastChanges();
    }

    public static void listFromSlot(ServerPlayer player, Slot slot, boolean wholeStack) {
        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) {
            return;
        }
        if (!PriceCatalog.isTradeable(stack)) {
            feedback(player, "message.homestead_sell_veg.listing_not_tradeable");
            return;
        }

        int maxList = StockPacker.LIST_MAX;
        try {
            maxList = SellVegConfig.LIST_MAX_COUNT.get();
        } catch (Exception ignored) {
        }
        int count = QuickListMath.listCount(
                stack.getCount(), wholeStack, maxList, GearOffers.isGearItem(stack.getItem()));
        if (count < 1) {
            feedback(player, "message.homestead_sell_veg.listing_not_enough");
            return;
        }

        MarketEvents.ensureMarketInitialized(player);
        PlayerNetShopData data = PlayerNetShopData.get(player);
        if (!data.shop().canAddListing()) {
            feedback(player, "message.homestead_sell_veg.listing_slots_full");
            return;
        }

        int unitPrice = Math.max(1, data.market().getSuggestMid(stack.getItem()));
        if (GearOffers.isGearItem(stack.getItem())) {
            unitPrice = Math.max(1, GearOffers.suggestUnitPrice(
                    data.market().getReferencePrice(stack.getItem()), stack));
        }
        ItemStack taken = stack.split(count);
        slot.setChanged();
        ListingService.ListResult result = ListingService.createFromTaken(player, taken, unitPrice);
        if (result != ListingService.ListResult.OK) {
            ItemStack leftover = slot.safeInsert(taken);
            if (!leftover.isEmpty()) {
                PlayerItemHelper.giveOrDrop(player, leftover);
            }
            tell(player, result);
        } else {
            feedback(player, "message.homestead_sell_veg.listing_created");
        }
    }

    private static void tell(ServerPlayer player, ListingService.ListResult result) {
        switch (result) {
            case OK -> feedback(player, "message.homestead_sell_veg.listing_created");
            case EMPTY_HAND -> feedback(player, "message.homestead_sell_veg.listing_empty_hand");
            case NOT_TRADEABLE -> feedback(player, "message.homestead_sell_veg.listing_not_tradeable");
            case NOT_ENOUGH_ITEMS -> feedback(player, "message.homestead_sell_veg.listing_not_enough");
            case SLOTS_FULL -> feedback(player, "message.homestead_sell_veg.listing_slots_full");
            case BAD_PRICE -> feedback(player, "message.homestead_sell_veg.listing_bad_price");
        }
    }

    /** Queue an in-UI toast and sync it immediately so inventory screens can draw it. */
    private static void feedback(ServerPlayer player, String key) {
        ShopFeedback.toast(player, key);
        player.displayClientMessage(Component.translatable(key), true);
        ShopSyncPacket.send(player, false);
    }
}
