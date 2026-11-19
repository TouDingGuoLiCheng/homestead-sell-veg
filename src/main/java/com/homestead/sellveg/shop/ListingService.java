package com.homestead.sellveg.shop;

import com.homestead.sellveg.config.SellVegConfig;
import com.homestead.sellveg.currency.CurrencyHelper;
import com.homestead.sellveg.data.PlayerNetShopData;
import com.homestead.sellveg.market.MarketEvents;
import com.homestead.sellveg.market.PriceCatalog;
import com.homestead.sellveg.market.PriceMath;
import com.homestead.sellveg.network.ShopFeedback;
import com.homestead.sellveg.stock.StockDraw;
import com.homestead.sellveg.stock.StockIndex;
import com.homestead.sellveg.stock.StockPacker;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Random;
import java.util.UUID;

public final class ListingService {
    private ListingService() {
    }

    public enum ListResult {
        OK,
        EMPTY_HAND,
        NOT_TRADEABLE,
        NOT_ENOUGH_ITEMS,
        SLOTS_FULL,
        BAD_PRICE
    }

    public static ListResult createFromHeld(ServerPlayer player, int count, int unitPrice) {
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            return ListResult.EMPTY_HAND;
        }
        ItemStack template = held.copy();
        template.setCount(1);
        return create(player, template, count, unitPrice);
    }

    public static ListResult create(ServerPlayer player, ItemStack template, int count, int unitPrice) {
        if (template.isEmpty()) {
            return ListResult.EMPTY_HAND;
        }
        if (unitPrice < 1) {
            return ListResult.BAD_PRICE;
        }
        int maxList = StockPacker.LIST_MAX;
        try {
            maxList = SellVegConfig.LIST_MAX_COUNT.get();
        } catch (Exception ignored) {
        }
        template = template.copy();
        template.setCount(1);
        count = GearTradeRules.clampTradeCount(GearOffers.isGearItem(template.getItem()), count, maxList);
        if (count < 1) {
            return ListResult.NOT_ENOUGH_ITEMS;
        }
        if (!PriceCatalog.isTradeable(template)) {
            return ListResult.NOT_TRADEABLE;
        }
        if (StockIndex.available(player, template) < count) {
            return ListResult.NOT_ENOUGH_ITEMS;
        }

        PlayerNetShopData data = PlayerNetShopData.get(player);
        MarketEvents.ensureMarketInitialized(player);
        data = PlayerNetShopData.get(player);
        if (!data.shop().canAddListing()) {
            return ListResult.SLOTS_FULL;
        }

        ItemStack taken = template.copy();
        taken.setCount(count);
        StockIndex.TakeOutcome outcome = StockIndex.takeTracking(player, template, count);
        if (!outcome.ok()) {
            return ListResult.NOT_ENOUGH_ITEMS;
        }

        return finishListing(player, data, taken, unitPrice, outcome.draws());
    }

    /**
     * List items already removed from a specific inventory slot.
     * Sources are marked as player so cancel always restores to the backpack.
     */
    public static ListResult createFromTaken(ServerPlayer player, ItemStack taken, int unitPrice) {
        if (taken == null || taken.isEmpty()) {
            return ListResult.EMPTY_HAND;
        }
        if (unitPrice < 1) {
            return ListResult.BAD_PRICE;
        }
        ItemStack template = taken.copy();
        template.setCount(1);
        if (!PriceCatalog.isTradeable(template)) {
            return ListResult.NOT_TRADEABLE;
        }

        PlayerNetShopData data = PlayerNetShopData.get(player);
        MarketEvents.ensureMarketInitialized(player);
        data = PlayerNetShopData.get(player);
        if (!data.shop().canAddListing()) {
            return ListResult.SLOTS_FULL;
        }

        ItemStack listed = taken.copy();
        return finishListing(player, data, listed, unitPrice, List.of(StockDraw.player(listed.getCount())));
    }

    private static ListResult finishListing(
            ServerPlayer player,
            PlayerNetShopData data,
            ItemStack taken,
            int unitPrice,
            List<StockDraw> sources) {
        int marketRef = Math.max(1, data.market().getReferencePrice(taken.getItem()));
        if (GearOffers.isGearItem(taken.getItem())) {
            marketRef = Math.max(1, GearOffers.suggestUnitPrice(marketRef, taken));
        }
        int[] live = data.market().getLiveBand(taken.getItem());
        int liveHi = live == null ? 0 : live[1];
        if (GearOffers.isGearItem(taken.getItem())) {
            liveHi = Math.max(liveHi, marketRef);
        }
        double ratio = unitPrice / (double) marketRef;
        int delay = ListingDelay.isUnsaleable(unitPrice, liveHi, SellVegConfig.LISTING_UNSALEABLE_RATIO.get())
                ? ListingDelay.NEVER_SELLS
                : ListingDelay.computeDelayTicks(
                ratio,
                SellVegConfig.LISTING_BASE_DELAY_TICKS.get(),
                SellVegConfig.LISTING_MIN_DELAY_TICKS.get(),
                SellVegConfig.INSTANT_SELL_RATIO.get(),
                SellVegConfig.INSTANT_SELL_CHANCE.get(),
                new Random(player.getRandom().nextLong())
        );
        if (data.market().isHot(taken.getItem()) && delay > 0) {
            delay = PriceMath.applyHotDelay(
                    delay,
                    SellVegConfig.HOT_SALE_DELAY_MULTIPLIER.get(),
                    SellVegConfig.LISTING_MIN_DELAY_TICKS.get()
            );
        }

        Listing listing = new Listing(UUID.randomUUID(), taken, unitPrice, delay, sources);
        data.shop().addListing(listing);
        PlayerNetShopData.save(player, data);

        if (delay == 0) {
            completeListing(player, listing.getId());
        }
        return ListResult.OK;
    }

    public static boolean cancel(ServerPlayer player, UUID id) {
        PlayerNetShopData data = PlayerNetShopData.get(player);
        return data.shop().removeListing(id).map(listing -> {
            StockIndex.restore(player, listing.getStack(), listing.getSources());
            PlayerNetShopData.save(player, data);
            return true;
        }).orElse(false);
    }

    public static boolean completeListing(ServerPlayer player, UUID id) {
        PlayerNetShopData data = PlayerNetShopData.get(player);
        return data.shop().removeListing(id).map(listing -> {
            String itemName = listing.getStack().getHoverName().getString();
            int count = listing.getStack().getCount();
            settleToSeller(
                    player,
                    data,
                    listing,
                    itemName + " x" + count,
                    "message.homestead_sell_veg.listing_sold",
                    new Object[]{itemName, count, listing.getPayout()},
                    true);
            return true;
        }).orElse(false);
    }

    /**
     * Seller-side settlement shared by the system NPC purchase path and player-to-player purchases.
     * Market volume is recorded only when the goods really leave the market (the NPC path).
     */
    public static void settleToSeller(
            ServerPlayer owner,
            PlayerNetShopData data,
            Listing listing,
            String ledgerSummary,
            String messageKey,
            Object[] messageArgs,
            boolean countMarket) {
        long payout = listing.getPayout();
        CurrencyHelper.giveCoins(data, payout);
        data.wallet().record(true, payout, ledgerSummary, System.currentTimeMillis());
        data.shop().addSoldTotal(payout);
        if (countMarket) {
            data.market().recordSold(listing.getStack().getItem(), listing.getStack().getCount());
        }
        PlayerNetShopData.save(owner, data);
        ShopFeedback.toast(owner, messageKey, messageArgs);
        owner.sendSystemMessage(Component.translatable(messageKey, messageArgs));
        com.homestead.sellveg.network.ShopSyncPacket.send(owner, false);
    }

    public static void tick(ServerPlayer player) {
        PlayerNetShopData data = PlayerNetShopData.get(player);
        if (data.shop().listings().isEmpty()) {
            return;
        }
        java.util.List<UUID> due = new java.util.ArrayList<>();
        boolean dirty = false;
        for (Listing listing : data.shop().listings()) {
            int before = listing.getRemainingTicks();
            if (listing.tickDown()) {
                due.add(listing.getId());
            }
            if (listing.getRemainingTicks() != before) {
                dirty = true;
            }
        }
        // Countdown lives in memory; flush NBT at most once per second while changing.
        if (dirty && player.tickCount % 20 == 0) {
            PlayerNetShopData.save(player, data);
        }
        for (UUID id : due) {
            completeListing(player, id);
        }
    }
}
