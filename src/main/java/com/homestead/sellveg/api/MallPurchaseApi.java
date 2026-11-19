package com.homestead.sellveg.api;

import com.homestead.sellveg.config.SellVegConfig;
import com.homestead.sellveg.currency.CurrencyHelper;
import com.homestead.sellveg.data.DropPointIndex;
import com.homestead.sellveg.data.PlayerNetShopData;
import com.homestead.sellveg.market.MarketEvents;
import com.homestead.sellveg.market.PriceCatalog;
import com.homestead.sellveg.market.SaleDailyLimit;
import com.homestead.sellveg.order.CheckoutService;
import com.homestead.sellveg.order.Courier;
import com.homestead.sellveg.order.ParcelFiller;
import com.homestead.sellveg.order.Shipment;
import com.homestead.sellveg.order.ShipmentCapacity;
import com.homestead.sellveg.shop.GearOffers;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Stable purchase hooks for companion mods (e.g. Construction DuoDuo Supply).
 * Charges the player wallet, records market buys (price float), and delivers via a custom {@link ItemSink}
 * instead of requiring the normal express drop-point path.
 */
public final class MallPurchaseApi {
    private MallPurchaseApi() {
    }

    public enum Status {
        OK,
        NOT_TRADEABLE,
        NOT_ENOUGH_COINS,
        SALE_DAILY_LIMIT,
        NO_DROP_POINT,
        SHIPMENTS_FULL,
        EMPTY
    }

    /** How many units {@code balance} can pay for at {@code unitPrice}, capped by {@code wanted}. */
    public static int affordableCount(long balance, int unitPrice, int wanted) {
        return MallPurchaseMath.affordableCount(balance, unitPrice, wanted);
    }

    public record Quote(boolean available, String shopId, int unitPrice) {
        public static Quote unavailable() {
            return new Quote(false, "", 0);
        }
    }

    @FunctionalInterface
    public interface ItemSink {
        /** Accept a stack; return how many items from this stack were not accepted. */
        int accept(ItemStack stack);
    }

    public record PurchaseResult(Status status, int boughtCount, long spent, int sinkLeftover) {
        public static PurchaseResult fail(Status status) {
            return new PurchaseResult(status, 0, 0L, 0);
        }
    }

    public static Quote quote(ServerPlayer player, Item item) {
        if (player == null || item == null || !PriceCatalog.isTradeable(new ItemStack(item))) {
            return Quote.unavailable();
        }
        MarketEvents.ensureMarketInitialized(player);
        PlayerNetShopData data = PlayerNetShopData.get(player);
        String shopId = data.market().cheapestShop(item);
        if (shopId == null || shopId.isEmpty() || !data.market().shopSells(shopId, item)) {
            return Quote.unavailable();
        }
        int unit = data.market().getShopBuyPrice(shopId, item);
        if (unit < 1) {
            return Quote.unavailable();
        }
        return new Quote(true, shopId, unit);
    }

    /**
     * Buy up to {@code count} of {@code item} from the system mall.
     * Non-gear counts are not clipped to the shop listing cap (64); gear stays at 1.
     * If the wallet cannot cover the full amount, buys as many as affordable (partial OK).
     * Discounted goods stop at the remaining daily sale allowance instead of failing the whole order.
     * Does not require a drop point; stacks go to {@code sink}. Does not show shop toasts.
     */
    public static PurchaseResult purchase(ServerPlayer player, Item item, int count, ItemSink sink) {
        if (player == null || item == null || sink == null) {
            return PurchaseResult.fail(Status.EMPTY);
        }
        count = MallPurchaseMath.requestedUnits(GearOffers.isGearItem(item), count);
        if (count < 1 || !PriceCatalog.isTradeable(new ItemStack(item))) {
            return PurchaseResult.fail(Status.NOT_TRADEABLE);
        }
        MarketEvents.ensureMarketInitialized(player);
        PlayerNetShopData data = PlayerNetShopData.get(player);
        String shopId = data.market().cheapestShop(item);
        if (shopId == null || shopId.isEmpty() || !data.market().shopSells(shopId, item)) {
            return PurchaseResult.fail(Status.NOT_TRADEABLE);
        }
        ItemStack delivery = new ItemStack(item);
        delivery.setCount(1);
        int unit = data.market().getShopBuyPrice(shopId, item);
        if (unit < 1) {
            return PurchaseResult.fail(Status.NOT_TRADEABLE);
        }

        long balance = data.wallet().balance();
        int buy = affordableCount(balance, unit, count);
        if (buy < 1) {
            return PurchaseResult.fail(Status.NOT_ENOUGH_COINS);
        }
        if (!delivery.isEnchanted() && data.market().isSale(shopId, item)) {
            int remain = SaleDailyLimit.remaining(
                    data.market().getSaleBoughtToday(), CheckoutService.countSaleUnitsInCart(data));
            if (remain < 1) {
                return PurchaseResult.fail(Status.SALE_DAILY_LIMIT);
            }
            buy = Math.min(buy, remain);
        }

        long total = (long) unit * buy;
        if (!CurrencyHelper.takeCoins(data, total)) {
            return PurchaseResult.fail(Status.NOT_ENOUGH_COINS);
        }
        String summary = delivery.getHoverName().getString() + " x" + buy;
        data.wallet().record(false, total, summary, System.currentTimeMillis());

        int leftover = deliverToSink(delivery, buy, sink);
        data.market().recordBought(item, buy);
        if (!delivery.isEnchanted() && data.market().isSale(shopId, item)) {
            data.market().recordSaleBought(buy);
        }
        PlayerNetShopData.save(player, data);
        return new PurchaseResult(Status.OK, buy, total, leftover);
    }

    /**
     * Queue leftover stacks as express shipments (same as mall checkout).
     * Requires a default drop point; does not charge coins.
     */
    public static Status enqueueExpress(ServerPlayer player, List<ItemStack> stacks) {
        if (player == null || stacks == null || stacks.isEmpty()) {
            return Status.EMPTY;
        }
        List<ItemStack> nonEmpty = new ArrayList<>();
        for (ItemStack stack : stacks) {
            if (stack != null && !stack.isEmpty()) {
                nonEmpty.add(stack.copy());
            }
        }
        if (nonEmpty.isEmpty()) {
            return Status.EMPTY;
        }
        PlayerNetShopData data = PlayerNetShopData.get(player);
        MarketEvents.ensureMarketInitialized(player);
        data = PlayerNetShopData.get(player);
        if (DropPointIndex.resolveDefault(player, data) == null) {
            PlayerNetShopData.save(player, data);
            return Status.NO_DROP_POINT;
        }
        int slots = SellVegConfig.BOX_SLOT_CAPACITY.get();
        List<NonNullList<ItemStack>> boxes = ParcelFiller.pack(nonEmpty, slots);
        int pendingCap = SellVegConfig.DELIVERY_PENDING_CAP.get();
        if (!ShipmentCapacity.canAdd(data.shipments().size(), boxes.size(), pendingCap)) {
            return Status.SHIPMENTS_FULL;
        }
        int min = SellVegConfig.DELIVERY_MIN_TICKS.get();
        int max = Math.max(min, SellVegConfig.DELIVERY_MAX_TICKS.get());
        for (NonNullList<ItemStack> box : boxes) {
            Courier courier = Courier.pick(player.getRandom().nextInt(Courier.values().length));
            int delay = min + player.getRandom().nextInt(max - min + 1);
            data.shipments().add(new Shipment(UUID.randomUUID(), box, courier.scaleDelay(delay), courier));
        }
        PlayerNetShopData.save(player, data);
        return Status.OK;
    }

    private static int deliverToSink(ItemStack unitTemplate, int count, ItemSink sink) {
        int leftover = 0;
        int remaining = count;
        int maxStack = Math.max(1, unitTemplate.getMaxStackSize());
        while (remaining > 0) {
            int n = Math.min(remaining, maxStack);
            ItemStack stack = unitTemplate.copy();
            stack.setCount(n);
            leftover += Math.max(0, sink.accept(stack));
            remaining -= n;
        }
        return leftover;
    }
}
