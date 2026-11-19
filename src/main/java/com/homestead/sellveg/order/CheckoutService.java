package com.homestead.sellveg.order;

import com.homestead.sellveg.config.SellVegConfig;
import com.homestead.sellveg.currency.CurrencyHelper;
import com.homestead.sellveg.data.DropPointIndex;
import com.homestead.sellveg.data.PlayerNetShopData;
import com.homestead.sellveg.market.MarketEvents;
import com.homestead.sellveg.market.PriceCatalog;
import com.homestead.sellveg.market.SaleDailyLimit;
import com.homestead.sellveg.shop.GearOffers;
import com.homestead.sellveg.shop.GearTradeRules;
import com.homestead.sellveg.stock.StockPacker;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

public final class CheckoutService {
    private CheckoutService() {
    }

    public enum Result {
        OK,
        EMPTY_CART,
        NOT_TRADEABLE,
        NO_DROP_POINT,
        NOT_ENOUGH_COINS,
        SALE_DAILY_LIMIT,
        SHIPMENTS_FULL
    }

    public static long cartTotal(ServerPlayer player, PlayerNetShopData data) {
        MarketEvents.ensureMarketInitialized(player);
        data = PlayerNetShopData.get(player);
        long total = 0;
        for (CartData.Line line : data.cart().lines()) {
            total += (long) line.unitPrice * line.count;
        }
        return total;
    }

    public static int countSaleUnitsInCart(PlayerNetShopData data) {
        int total = 0;
        for (CartData.Line line : data.cart().lines()) {
            if (!line.stack.isEnchanted() && data.market().isSale(line.shopId, line.item())) {
                total += line.count;
            }
        }
        return total;
    }

    private static int cartLineCount(PlayerNetShopData data, String shopId, ItemStack template) {
        for (CartData.Line line : data.cart().lines()) {
            if (line.shopId.equals(shopId == null ? "" : shopId) && GearOffers.sameOffer(line.stack, template)) {
                return line.count;
            }
        }
        return 0;
    }

    private static int saleUnitsDelta(PlayerNetShopData data, String shopId, ItemStack template, int newTotal) {
        if (template.isEnchanted() || !data.market().isSale(shopId, template.getItem())) {
            return 0;
        }
        return Math.max(0, newTotal - cartLineCount(data, shopId, template));
    }

    public static boolean wouldExceedSaleLimit(PlayerNetShopData data, String shopId, ItemStack template, int newTotalCount) {
        int delta = saleUnitsDelta(data, shopId, template, newTotalCount);
        int inCart = countSaleUnitsInCart(data);
        int oldLine = cartLineCount(data, shopId, template);
        if (!template.isEnchanted() && data.market().isSale(shopId, template.getItem())) {
            inCart -= oldLine;
        }
        return !SaleDailyLimit.canAdd(data.market().getSaleBoughtToday(), inCart, delta);
    }

    public static Result addToCart(ServerPlayer player, Item item, int count) {
        PlayerNetShopData data = PlayerNetShopData.get(player);
        MarketEvents.ensureMarketInitialized(player);
        data = PlayerNetShopData.get(player);
        String shopId = data.market().cheapestShop(item);
        return addToCart(player, shopId, new ItemStack(item), count);
    }

    public static Result addToCart(ServerPlayer player, String shopId, Item item, int count) {
        return addToCart(player, shopId, new ItemStack(item), count);
    }

    public static Result addToCart(ServerPlayer player, String shopId, ItemStack template, int count) {
        count = GearTradeRules.clampTradeCount(
                GearOffers.isGearItem(template == null ? null : template.getItem()), count, StockPacker.LIST_MAX);
        if (count < 1 || template == null || template.isEmpty()
                || !PriceCatalog.isTradeable(new ItemStack(template.getItem()))) {
            return Result.NOT_TRADEABLE;
        }
        PlayerNetShopData data = PlayerNetShopData.get(player);
        MarketEvents.ensureMarketInitialized(player);
        data = PlayerNetShopData.get(player);
        if (shopId == null || shopId.isEmpty() || !data.market().shopSells(shopId, template.getItem())) {
            return Result.NOT_TRADEABLE;
        }
        int unit;
        ItemStack cartStack = template.copy();
        cartStack.setCount(1);
        if (cartStack.isEnchanted()) {
            GearOffers.Offer offer = data.market().findGearEnchantOffer(shopId, cartStack);
            if (offer == null) {
                return Result.NOT_TRADEABLE;
            }
            unit = offer.unitPrice();
            cartStack = offer.stack().copy();
        } else {
            unit = data.market().getShopBuyPrice(shopId, cartStack.getItem());
        }
        int existing = cartLineCount(data, shopId, cartStack);
        if (GearOffers.isGearItem(cartStack.getItem()) && existing >= 1) {
            data.cart().setCount(shopId, cartStack, 1);
            PlayerNetShopData.save(player, data);
            return Result.OK;
        }
        if (wouldExceedSaleLimit(data, shopId, cartStack, existing + count)) {
            return Result.SALE_DAILY_LIMIT;
        }
        data.cart().add(shopId, cartStack, count, unit);
        PlayerNetShopData.save(player, data);
        return Result.OK;
    }

    public static Result setCartCount(ServerPlayer player, String shopId, Item item, int count) {
        return setCartCount(player, shopId, new ItemStack(item), count);
    }

    public static Result setCartCount(ServerPlayer player, String shopId, ItemStack template, int count) {
        if (template == null || template.isEmpty() || !PriceCatalog.isTradeable(new ItemStack(template.getItem()))) {
            return Result.NOT_TRADEABLE;
        }
        PlayerNetShopData data = PlayerNetShopData.get(player);
        MarketEvents.ensureMarketInitialized(player);
        data = PlayerNetShopData.get(player);
        count = GearTradeRules.clampTradeCount(GearOffers.isGearItem(template.getItem()), count, StockPacker.LIST_MAX);
        if (count >= 1 && wouldExceedSaleLimit(data, shopId, template, count)) {
            return Result.SALE_DAILY_LIMIT;
        }
        data.cart().setCount(shopId, template, count);
        PlayerNetShopData.save(player, data);
        return Result.OK;
    }

    public static Result checkout(ServerPlayer player) {
        PlayerNetShopData data = PlayerNetShopData.get(player);
        if (data.cart().isEmpty()) {
            return Result.EMPTY_CART;
        }
        MarketEvents.ensureMarketInitialized(player);
        data = PlayerNetShopData.get(player);
        if (DropPointIndex.resolveDefault(player, data) == null) {
            PlayerNetShopData.save(player, data);
            return Result.NO_DROP_POINT;
        }
        if (!SaleDailyLimit.canAdd(data.market().getSaleBoughtToday(), 0, countSaleUnitsInCart(data))) {
            return Result.SALE_DAILY_LIMIT;
        }
        int slots = SellVegConfig.BOX_SLOT_CAPACITY.get();
        List<NonNullList<ItemStack>> boxes = ParcelFiller.pack(data.cart().asStacks(), slots);
        int pendingCap = SellVegConfig.DELIVERY_PENDING_CAP.get();
        if (!ShipmentCapacity.canAdd(data.shipments().size(), boxes.size(), pendingCap)) {
            return Result.SHIPMENTS_FULL;
        }
        long total = 0;
        for (CartData.Line line : data.cart().lines()) {
            if (!PriceCatalog.isTradeable(new ItemStack(line.item()))) {
                return Result.NOT_TRADEABLE;
            }
            total += (long) line.unitPrice * line.count;
        }
        if (!CurrencyHelper.takeCoins(data, total)) {
            return Result.NOT_ENOUGH_COINS;
        }
        data.wallet().record(false, total, cartSummary(data), System.currentTimeMillis());

        int min = SellVegConfig.DELIVERY_MIN_TICKS.get();
        int max = Math.max(min, SellVegConfig.DELIVERY_MAX_TICKS.get());
        for (NonNullList<ItemStack> box : boxes) {
            data.shipments().add(rollShipment(player, box, min, max));
        }
        int saleUnits = 0;
        for (CartData.Line line : data.cart().lines()) {
            data.market().recordBought(line.item(), line.count);
            if (!line.stack.isEnchanted() && data.market().isSale(line.shopId, line.item())) {
                saleUnits += line.count;
            }
        }
        if (saleUnits > 0) {
            data.market().recordSaleBought(saleUnits);
        }
        String items = orderItemsSummary(data.cart().lines());
        data.cart().clear();
        PlayerNetShopData.save(player, data);
        com.homestead.sellveg.network.ShopFeedback.toast(player,
                "message.homestead_sell_veg.checkout_ok", items, total);
        return Result.OK;
    }

    public static Result buyNow(ServerPlayer player, Item item, int count) {
        PlayerNetShopData data = PlayerNetShopData.get(player);
        String shopId = data.market().cheapestShop(item);
        return buyNow(player, shopId, new ItemStack(item), count);
    }

    public static Result buyNow(ServerPlayer player, String shopId, Item item, int count) {
        return buyNow(player, shopId, new ItemStack(item), count);
    }

    public static Result buyNow(ServerPlayer player, String shopId, ItemStack template, int count) {
        count = GearTradeRules.clampTradeCount(
                GearOffers.isGearItem(template == null ? null : template.getItem()), count, StockPacker.LIST_MAX);
        if (count < 1 || template == null || template.isEmpty()
                || !PriceCatalog.isTradeable(new ItemStack(template.getItem()))) {
            return Result.NOT_TRADEABLE;
        }
        PlayerNetShopData data = PlayerNetShopData.get(player);
        MarketEvents.ensureMarketInitialized(player);
        data = PlayerNetShopData.get(player);
        if (shopId == null || shopId.isEmpty() || !data.market().shopSells(shopId, template.getItem())) {
            return Result.NOT_TRADEABLE;
        }
        ItemStack delivery = template.copy();
        delivery.setCount(1);
        int unit;
        if (delivery.isEnchanted()) {
            GearOffers.Offer offer = data.market().findGearEnchantOffer(shopId, delivery);
            if (offer == null) {
                return Result.NOT_TRADEABLE;
            }
            unit = offer.unitPrice();
            delivery = offer.stack().copy();
        } else {
            unit = data.market().getShopBuyPrice(shopId, delivery.getItem());
        }
        if (!delivery.isEnchanted() && data.market().isSale(shopId, delivery.getItem())
                && !SaleDailyLimit.canAdd(data.market().getSaleBoughtToday(), countSaleUnitsInCart(data), count)) {
            return Result.SALE_DAILY_LIMIT;
        }
        if (DropPointIndex.resolveDefault(player, data) == null) {
            PlayerNetShopData.save(player, data);
            return Result.NO_DROP_POINT;
        }
        long total = (long) unit * count;
        CartData temp = new CartData();
        temp.add(shopId, delivery, count, unit);
        int slots = SellVegConfig.BOX_SLOT_CAPACITY.get();
        List<NonNullList<ItemStack>> boxes = ParcelFiller.pack(temp.asStacks(), slots);
        int pendingCap = SellVegConfig.DELIVERY_PENDING_CAP.get();
        if (!ShipmentCapacity.canAdd(data.shipments().size(), boxes.size(), pendingCap)) {
            return Result.SHIPMENTS_FULL;
        }
        if (!CurrencyHelper.takeCoins(data, total)) {
            return Result.NOT_ENOUGH_COINS;
        }
        data.wallet().record(false, total, buyNowSummary(delivery, count), System.currentTimeMillis());
        int min = SellVegConfig.DELIVERY_MIN_TICKS.get();
        int max = Math.max(min, SellVegConfig.DELIVERY_MAX_TICKS.get());
        for (NonNullList<ItemStack> box : boxes) {
            data.shipments().add(rollShipment(player, box, min, max));
        }
        data.market().recordBought(delivery.getItem(), count);
        if (!delivery.isEnchanted() && data.market().isSale(shopId, delivery.getItem())) {
            data.market().recordSaleBought(count);
        }
        PlayerNetShopData.save(player, data);
        com.homestead.sellveg.network.ShopFeedback.toast(player,
                "message.homestead_sell_veg.checkout_ok", buyNowSummary(delivery, count), total);
        return Result.OK;
    }

    private static Shipment rollShipment(ServerPlayer player, NonNullList<ItemStack> box, int min, int max) {
        Courier courier = Courier.pick(player.getRandom().nextInt(Courier.values().length));
        int delay = min + player.getRandom().nextInt(max - min + 1);
        return new Shipment(UUID.randomUUID(), box, courier.scaleDelay(delay), courier);
    }

    private static String cartSummary(PlayerNetShopData data) {
        if (data.cart().isEmpty()) {
            return "checkout";
        }
        return orderItemsSummary(data.cart().lines());
    }

    private static String orderItemsSummary(List<CartData.Line> lines) {
        StringBuilder sb = new StringBuilder();
        for (CartData.Line line : lines) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(line.stack.getHoverName().getString()).append(" x").append(line.count);
        }
        return sb.isEmpty() ? "checkout" : sb.toString();
    }

    private static String buyNowSummary(ItemStack stack, int count) {
        return stack.getHoverName().getString() + " x" + count;
    }
}
