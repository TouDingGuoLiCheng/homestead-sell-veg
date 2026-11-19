package com.homestead.sellveg.network;

import com.homestead.sellveg.currency.TransferService;
import com.homestead.sellveg.order.CheckoutService;
import com.homestead.sellveg.order.PeerPurchaseService;
import com.homestead.sellveg.shop.ListingService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public final class ShopActionPacket {
    public enum Type {
        LIST, CANCEL, CART_ADD, CART_DEL, CART_SET, CHECKOUT, BUY_NOW, SET_DEFAULT, BUY_PEER, TRANSFER
    }

    public Type type = Type.CHECKOUT;
    public ItemStack stack = ItemStack.EMPTY;
    public int count = 1;
    public int price = 1;
    public UUID id = new UUID(0, 0);
    public UUID ownerId = new UUID(0, 0);
    public String shopId = "";
    public long amount;

    public static ShopActionPacket list(ItemStack stack, int count, int price) {
        ShopActionPacket p = new ShopActionPacket();
        p.type = Type.LIST;
        p.stack = stack.copy();
        p.count = count;
        p.price = price;
        return p;
    }

    public static ShopActionPacket cancel(UUID id) {
        ShopActionPacket p = new ShopActionPacket();
        p.type = Type.CANCEL;
        p.id = id;
        return p;
    }

    public static ShopActionPacket cartAdd(String shopId, Item item, int count) {
        return cartAdd(shopId, new ItemStack(item), count);
    }

    public static ShopActionPacket cartAdd(String shopId, ItemStack stack, int count) {
        ShopActionPacket p = new ShopActionPacket();
        p.type = Type.CART_ADD;
        p.shopId = shopId == null ? "" : shopId;
        p.stack = stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        if (!p.stack.isEmpty()) {
            p.stack.setCount(1);
        }
        p.count = count;
        return p;
    }

    public static ShopActionPacket cartDel(String shopId, Item item) {
        return cartDel(shopId, new ItemStack(item));
    }

    public static ShopActionPacket cartDel(String shopId, ItemStack stack) {
        ShopActionPacket p = new ShopActionPacket();
        p.type = Type.CART_DEL;
        p.shopId = shopId == null ? "" : shopId;
        p.stack = stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        if (!p.stack.isEmpty()) {
            p.stack.setCount(1);
        }
        return p;
    }

    public static ShopActionPacket cartSet(String shopId, Item item, int count) {
        return cartSet(shopId, new ItemStack(item), count);
    }

    public static ShopActionPacket cartSet(String shopId, ItemStack stack, int count) {
        ShopActionPacket p = new ShopActionPacket();
        p.type = Type.CART_SET;
        p.shopId = shopId == null ? "" : shopId;
        p.stack = stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        if (!p.stack.isEmpty()) {
            p.stack.setCount(1);
        }
        p.count = count;
        return p;
    }

    public static ShopActionPacket checkout() {
        ShopActionPacket p = new ShopActionPacket();
        p.type = Type.CHECKOUT;
        return p;
    }

    public static ShopActionPacket buyNow(String shopId, Item item, int count) {
        return buyNow(shopId, new ItemStack(item), count);
    }

    public static ShopActionPacket buyNow(String shopId, ItemStack stack, int count) {
        ShopActionPacket p = new ShopActionPacket();
        p.type = Type.BUY_NOW;
        p.shopId = shopId == null ? "" : shopId;
        p.stack = stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        if (!p.stack.isEmpty()) {
            p.stack.setCount(1);
        }
        p.count = count;
        return p;
    }

    public static ShopActionPacket setDefault(UUID id) {
        ShopActionPacket p = new ShopActionPacket();
        p.type = Type.SET_DEFAULT;
        p.id = id;
        return p;
    }

    /** Buys one whole listing belonging to another online player. */
    public static ShopActionPacket buyPeer(UUID ownerId, UUID listingId) {
        ShopActionPacket p = new ShopActionPacket();
        p.type = Type.BUY_PEER;
        p.ownerId = ownerId == null ? new UUID(0, 0) : ownerId;
        p.id = listingId == null ? new UUID(0, 0) : listingId;
        return p;
    }

    public static ShopActionPacket transfer(UUID targetId, long amount) {
        ShopActionPacket p = new ShopActionPacket();
        p.type = Type.TRANSFER;
        p.id = targetId == null ? new UUID(0, 0) : targetId;
        p.amount = amount;
        return p;
    }

    public static void encode(ShopActionPacket p, FriendlyByteBuf buf) {
        buf.writeEnum(p.type);
        buf.writeItem(p.stack == null ? ItemStack.EMPTY : p.stack);
        buf.writeVarInt(p.count);
        buf.writeVarInt(p.price);
        buf.writeUUID(p.id);
        buf.writeUtf(p.shopId == null ? "" : p.shopId, 64);
        buf.writeUUID(p.ownerId == null ? new UUID(0, 0) : p.ownerId);
        buf.writeLong(p.amount);
    }

    public static ShopActionPacket decode(FriendlyByteBuf buf) {
        ShopActionPacket p = new ShopActionPacket();
        p.type = buf.readEnum(Type.class);
        p.stack = buf.readItem();
        p.count = buf.readVarInt();
        p.price = buf.readVarInt();
        p.id = buf.readUUID();
        p.shopId = buf.readUtf(64);
        p.ownerId = buf.readUUID();
        p.amount = buf.readLong();
        return p;
    }

    public static void handle(ShopActionPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            switch (packet.type) {
                case LIST -> tellList(player, ListingService.create(player, packet.stack, packet.count, packet.price));
                case CANCEL -> {
                    if (!ListingService.cancel(player, packet.id)) {
                        ShopFeedback.toast(player, "message.homestead_sell_veg.listing_unknown");
                    }
                }
                case CART_ADD -> {
                    if (packet.stack.isEmpty()) {
                        tellCheckout(player, CheckoutService.Result.NOT_TRADEABLE);
                    } else {
                        tellCartAdd(player, CheckoutService.addToCart(
                                player, packet.shopId, packet.stack, packet.count));
                    }
                }
                case CART_DEL -> {
                    var data = com.homestead.sellveg.data.PlayerNetShopData.get(player);
                    data.cart().remove(packet.shopId, packet.stack);
                    com.homestead.sellveg.data.PlayerNetShopData.save(player, data);
                }
                case CART_SET -> {
                    if (!packet.stack.isEmpty()) {
                        tellCheckout(player, CheckoutService.setCartCount(
                                player, packet.shopId, packet.stack, packet.count));
                    }
                }
                case CHECKOUT -> tellCheckout(player, CheckoutService.checkout(player));
                case BUY_NOW -> {
                    if (packet.stack.isEmpty()) {
                        tellCheckout(player, CheckoutService.Result.NOT_TRADEABLE);
                    } else {
                        tellCheckout(player, CheckoutService.buyNow(
                                player, packet.shopId, packet.stack, packet.count));
                    }
                }
                case SET_DEFAULT -> {
                    var data = com.homestead.sellveg.data.PlayerNetShopData.get(player);
                    data.setDefaultDropPointId(packet.id);
                    com.homestead.sellveg.data.PlayerNetShopData.save(player, data);
                    ShopFeedback.toast(player, "message.homestead_sell_veg.default_set");
                }
                case BUY_PEER -> tellPeerPurchase(player,
                        PeerPurchaseService.purchase(player, packet.ownerId, packet.id));
                case TRANSFER -> tellTransfer(player,
                        TransferService.transfer(player, packet.id, packet.amount));
            }
            ShopSyncPacket.send(player, false);
        });
        ctx.get().setPacketHandled(true);
    }

    private static void tellList(ServerPlayer player, ListingService.ListResult result) {
        switch (result) {
            case OK -> ShopFeedback.toast(player, "message.homestead_sell_veg.listing_created");
            case EMPTY_HAND -> ShopFeedback.toast(player, "message.homestead_sell_veg.listing_empty_hand");
            case NOT_TRADEABLE -> ShopFeedback.toast(player, "message.homestead_sell_veg.listing_not_tradeable");
            case NOT_ENOUGH_ITEMS -> ShopFeedback.toast(player, "message.homestead_sell_veg.listing_not_enough");
            case SLOTS_FULL -> ShopFeedback.toast(player, "message.homestead_sell_veg.listing_slots_full");
            case BAD_PRICE -> ShopFeedback.toast(player, "message.homestead_sell_veg.listing_bad_price");
        }
    }

    private static void tellCheckout(ServerPlayer player, CheckoutService.Result result) {
        switch (result) {
            case OK -> {
            }
            case EMPTY_CART -> ShopFeedback.toast(player, "message.homestead_sell_veg.cart_empty");
            case NOT_TRADEABLE -> ShopFeedback.toast(player, "message.homestead_sell_veg.listing_not_tradeable");
            case NO_DROP_POINT -> ShopFeedback.toast(player, "message.homestead_sell_veg.checkout_no_point");
            case NOT_ENOUGH_COINS -> ShopFeedback.toast(player, "message.homestead_sell_veg.checkout_no_coins");
            case SALE_DAILY_LIMIT -> ShopFeedback.toast(player, "message.homestead_sell_veg.sale_daily_limit");
            case SHIPMENTS_FULL -> ShopFeedback.toast(player, "message.homestead_sell_veg.checkout_shipments_full");
        }
    }

    private static void tellPeerPurchase(ServerPlayer player, PeerPurchaseService.Result result) {
        switch (result) {
            case OK -> {
            }
            case SELF -> ShopFeedback.toast(player, "message.homestead_sell_veg.peer_buy_self");
            case OWNER_OFFLINE -> ShopFeedback.toast(player, "message.homestead_sell_veg.peer_buy_offline");
            case LISTING_GONE -> ShopFeedback.toast(player, "message.homestead_sell_veg.peer_buy_gone");
            case NOT_ENOUGH_MONEY -> ShopFeedback.toast(player, "message.homestead_sell_veg.checkout_no_coins");
        }
    }

    private static void tellTransfer(ServerPlayer player, TransferService.Result result) {
        switch (result) {
            case OK -> {
            }
            case BAD_AMOUNT -> ShopFeedback.toast(player, "message.homestead_sell_veg.transfer_bad_amount");
            case SELF -> ShopFeedback.toast(player, "message.homestead_sell_veg.transfer_self");
            case TARGET_OFFLINE -> ShopFeedback.toast(player, "message.homestead_sell_veg.transfer_offline");
            case NOT_ENOUGH -> ShopFeedback.toast(player, "message.homestead_sell_veg.checkout_no_coins");
        }
    }

    private static void tellCartAdd(ServerPlayer player, CheckoutService.Result result) {
        if (result == CheckoutService.Result.OK) {
            ShopFeedback.toast(player, "message.homestead_sell_veg.cart_added");
            return;
        }
        tellCheckout(player, result);
    }
}
