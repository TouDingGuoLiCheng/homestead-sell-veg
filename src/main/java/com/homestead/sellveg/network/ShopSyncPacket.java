package com.homestead.sellveg.network;

import com.homestead.sellveg.client.MallBrowse;
import com.homestead.sellveg.client.ShopClientState;
import com.homestead.sellveg.currency.CurrencyHelper;
import com.homestead.sellveg.currency.LedgerEntry;
import com.homestead.sellveg.data.DropPointIndex;
import com.homestead.sellveg.data.DropPointRecord;
import com.homestead.sellveg.data.PlayerNetShopData;
import com.homestead.sellveg.market.PriceCatalog;
import com.homestead.sellveg.order.CartData;
import com.homestead.sellveg.order.Shipment;
import com.homestead.sellveg.order.ShipmentSummary;
import com.homestead.sellveg.shop.Listing;
import com.homestead.sellveg.shop.PeerShopIndex;
import com.homestead.sellveg.shop.PresetShop;
import com.homestead.sellveg.shop.PresetShops;
import com.homestead.sellveg.shop.ShopOffers;
import com.homestead.sellveg.shop.ShopProgression;
import com.homestead.sellveg.shop.ShopShelfPlanner;
import com.homestead.sellveg.stock.StockIndex;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public final class ShopSyncPacket {
    public boolean open;
    public boolean includeMall;
    public long coins;
    public int level;
    public int listingUsed;
    public int listingCap;
    public long soldTotal;
    public long upgradePrev;
    public long upgradeGoal;
    public long cartTotal;
    public int shipmentCount;
    public boolean shipmentBlocked;
    public String defaultName = "-";
    public final List<ListingRow> listings = new ArrayList<>();
    public final List<CartRow> cart = new ArrayList<>();
    public final List<PointRow> points = new ArrayList<>();
    public final List<ShipmentRow> shipments = new ArrayList<>();
    public final List<MallRow> mall = new ArrayList<>();
    public int unloadedSources;
    public final List<StockRow> stock = new ArrayList<>();
    public final List<LedgerRow> ledger = new ArrayList<>();
    public String toastKey = "";
    public final List<String> toastArgs = new ArrayList<>();
    public final List<PeerShopRow> peerShops = new ArrayList<>();
    public final List<PeerListingRow> peerListings = new ArrayList<>();
    public boolean includePeerListings;

    public static void encode(ShopSyncPacket p, FriendlyByteBuf buf) {
        buf.writeBoolean(p.open);
        buf.writeBoolean(p.includeMall);
        buf.writeLong(p.coins);
        buf.writeVarInt(p.level);
        buf.writeVarInt(p.listingUsed);
        buf.writeVarInt(p.listingCap);
        buf.writeLong(p.soldTotal);
        buf.writeLong(p.upgradePrev);
        buf.writeLong(p.upgradeGoal);
        buf.writeLong(p.cartTotal);
        buf.writeVarInt(p.shipmentCount);
        buf.writeBoolean(p.shipmentBlocked);
        buf.writeUtf(p.defaultName, 64);
        buf.writeVarInt(p.listings.size());
        for (ListingRow row : p.listings) {
            buf.writeUUID(row.id);
            buf.writeItem(row.stack);
            buf.writeVarInt(row.unitPrice);
            buf.writeVarInt(row.remainingTicks);
            buf.writeBoolean(row.hot);
        }
        buf.writeVarInt(p.cart.size());
        for (CartRow row : p.cart) {
            buf.writeUtf(row.shopId, 64);
            buf.writeItem(row.stack);
            buf.writeVarInt(row.count);
            buf.writeVarInt(row.unitPrice);
        }
        buf.writeVarInt(p.points.size());
        for (PointRow row : p.points) {
            buf.writeUUID(row.id);
            buf.writeUtf(row.name, 64);
            buf.writeBoolean(row.isDefault);
        }
        buf.writeVarInt(p.shipments.size());
        for (ShipmentRow row : p.shipments) {
            buf.writeUUID(row.id);
            writeItemId(buf, row.primary.getItem());
            buf.writeVarInt(row.primary.getCount());
            buf.writeVarInt(row.kindCount);
            buf.writeVarInt(row.remainingTicks);
            buf.writeBoolean(row.blocked);
            buf.writeUtf(row.courierId == null ? "" : row.courierId, 32);
        }
        buf.writeVarInt(p.mall.size());
        if (p.includeMall) {
            for (MallRow row : p.mall) {
                buf.writeUtf(row.shopId, 64);
                buf.writeItem(row.stack);
                buf.writeVarInt(row.refPrice);
                buf.writeVarInt(row.buyPrice);
                buf.writeVarInt(row.baseBuyPrice);
                buf.writeBoolean(row.hot);
                buf.writeVarInt(row.salePercent);
            }
        }
        buf.writeVarInt(p.unloadedSources);
        buf.writeVarInt(p.stock.size());
        for (StockRow row : p.stock) {
            buf.writeItem(row.template);
            buf.writeVarInt(row.count);
            buf.writeVarInt(row.refPrice);
            buf.writeVarInt(row.bandLo);
            buf.writeVarInt(row.bandHi);
        }
        buf.writeVarInt(p.ledger.size());
        for (LedgerRow row : p.ledger) {
            buf.writeLong(row.timeMillis());
            buf.writeBoolean(row.income());
            buf.writeLong(row.amount());
            buf.writeUtf(row.summary(), 64);
        }
        buf.writeBoolean(!p.toastKey.isEmpty());
        if (!p.toastKey.isEmpty()) {
            buf.writeUtf(p.toastKey, 128);
            buf.writeVarInt(p.toastArgs.size());
            for (String arg : p.toastArgs) {
                buf.writeUtf(arg, 256);
            }
        }
        buf.writeVarInt(p.peerShops.size());
        for (PeerShopRow row : p.peerShops) {
            buf.writeUUID(row.ownerId);
            buf.writeUtf(row.name, 32);
            buf.writeVarInt(row.listingCount);
        }
        buf.writeBoolean(p.includePeerListings);
        buf.writeVarInt(p.peerListings.size());
        for (PeerListingRow row : p.peerListings) {
            buf.writeUUID(row.ownerId);
            buf.writeUtf(row.ownerName, 32);
            buf.writeUUID(row.listingId);
            buf.writeItem(row.stack);
            buf.writeVarInt(row.unitPrice);
        }
    }

    public static ShopSyncPacket decode(FriendlyByteBuf buf) {
        ShopSyncPacket p = new ShopSyncPacket();
        p.open = buf.readBoolean();
        p.includeMall = buf.readBoolean();
        p.coins = buf.readLong();
        p.level = buf.readVarInt();
        p.listingUsed = buf.readVarInt();
        p.listingCap = buf.readVarInt();
        p.soldTotal = buf.readLong();
        p.upgradePrev = buf.readLong();
        p.upgradeGoal = buf.readLong();
        p.cartTotal = buf.readLong();
        p.shipmentCount = buf.readVarInt();
        p.shipmentBlocked = buf.readBoolean();
        p.defaultName = buf.readUtf(64);
        int n = buf.readVarInt();
        for (int i = 0; i < n; i++) {
            UUID id = buf.readUUID();
            ItemStack stack = buf.readItem();
            p.listings.add(new ListingRow(id, stack, buf.readVarInt(), buf.readVarInt(), buf.readBoolean()));
        }
        n = buf.readVarInt();
        for (int i = 0; i < n; i++) {
            p.cart.add(new CartRow(buf.readUtf(64), buf.readItem(), buf.readVarInt(), buf.readVarInt()));
        }
        n = buf.readVarInt();
        for (int i = 0; i < n; i++) {
            p.points.add(new PointRow(buf.readUUID(), buf.readUtf(64), buf.readBoolean()));
        }
        n = buf.readVarInt();
        for (int i = 0; i < n; i++) {
            UUID id = buf.readUUID();
            Item item = readItemId(buf);
            int count = buf.readVarInt();
            ItemStack primary = (item == null || count < 1) ? ItemStack.EMPTY : new ItemStack(item, count);
            p.shipments.add(new ShipmentRow(
                    id,
                    primary,
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readBoolean(),
                    buf.readUtf(32)));
        }
        n = buf.readVarInt();
        if (p.includeMall) {
            for (int i = 0; i < n; i++) {
                p.mall.add(new MallRow(
                        buf.readUtf(64),
                        buf.readItem(),
                        buf.readVarInt(),
                        buf.readVarInt(),
                        buf.readVarInt(),
                        buf.readBoolean(),
                        buf.readVarInt()));
            }
        }
        p.unloadedSources = buf.readVarInt();
        n = buf.readVarInt();
        for (int i = 0; i < n; i++) {
            p.stock.add(new StockRow(
                    buf.readItem(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt()));
        }
        n = buf.readVarInt();
        for (int i = 0; i < n; i++) {
            p.ledger.add(new LedgerRow(buf.readLong(), buf.readBoolean(), buf.readLong(), buf.readUtf(64)));
        }
        if (buf.readBoolean()) {
            p.toastKey = buf.readUtf(128);
            n = buf.readVarInt();
            for (int i = 0; i < n; i++) {
                p.toastArgs.add(buf.readUtf(256));
            }
        }
        n = buf.readVarInt();
        for (int i = 0; i < n; i++) {
            p.peerShops.add(new PeerShopRow(buf.readUUID(), buf.readUtf(32), buf.readVarInt()));
        }
        p.includePeerListings = buf.readBoolean();
        n = buf.readVarInt();
        for (int i = 0; i < n; i++) {
            p.peerListings.add(new PeerListingRow(
                    buf.readUUID(),
                    buf.readUtf(32),
                    buf.readUUID(),
                    buf.readItem(),
                    buf.readVarInt()));
        }
        return p;
    }

    public static void handle(ShopSyncPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> handleClient(packet));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(ShopSyncPacket packet) {
        ShopClientState.apply(packet);
    }

    public static void send(ServerPlayer player, boolean open) {
        send(player, open, null);
    }

    /** Sends the packet with one peer's shelf included, without re-sending the whole mall. */
    public static void send(ServerPlayer player, boolean open, UUID peerTarget) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), build(player, open, peerTarget));
    }

    public static ShopSyncPacket build(ServerPlayer player, boolean open) {
        return build(player, open, null);
    }

    public static ShopSyncPacket build(ServerPlayer player, boolean open, UUID peerTarget) {
        PlayerNetShopData data = PlayerNetShopData.get(player);
        DropPointIndex.resolveDefault(player, data);
        PlayerNetShopData.save(player, data);
        data = PlayerNetShopData.get(player);

        ShopSyncPacket p = new ShopSyncPacket();
        p.open = open;
        p.includeMall = open;
        p.coins = CurrencyHelper.countCoins(player);
        p.level = data.shop().getLevel();
        p.listingUsed = (int) data.shop().listings().stream().filter(l -> !l.getStack().isEmpty()).count();
        p.listingCap = data.shop().getListingCapacity();
        p.soldTotal = data.shop().getSoldTotal();
        long[] thresholds = ShopProgression.thresholds();
        p.upgradePrev = ShopProgression.previousThreshold(p.level, thresholds);
        p.upgradeGoal = ShopProgression.nextThreshold(p.level, thresholds);
        p.cartTotal = 0;
        for (CartData.Line line : data.cart().lines()) {
            p.cartTotal += (long) line.unitPrice * line.count;
            p.cart.add(new CartRow(line.shopId, line.stack.copy(), line.count, line.unitPrice));
        }
        p.shipmentCount = data.shipments().size();
        p.shipmentBlocked = data.shipments().stream().anyMatch(Shipment::isBlocked);
        UUID def = data.getDefaultDropPointId();
        for (DropPointRecord record : data.dropPoints()) {
            boolean isDef = record.id.equals(def);
            if (isDef) {
                p.defaultName = record.name;
            }
            p.points.add(new PointRow(record.id, record.name, isDef));
        }
        for (Shipment shipment : data.shipments()) {
            ItemStack primary = ShipmentSummary.primaryStack(shipment.getContents());
            p.shipments.add(new ShipmentRow(
                    shipment.getId(),
                    primary.isEmpty() ? ItemStack.EMPTY : primary,
                    ShipmentSummary.kindCount(shipment.getContents()),
                    shipment.getRemainingTicks(),
                    shipment.isBlocked(),
                    shipment.getCourier().id()
            ));
        }
        for (Listing listing : data.shop().listings()) {
            if (listing.getStack().isEmpty()) {
                continue;
            }
            p.listings.add(new ListingRow(
                    listing.getId(),
                    listing.getStack().copy(),
                    listing.getUnitPrice(),
                    listing.getRemainingTicks(),
                    data.market().isHot(listing.getStack().getItem())
            ));
        }

        if (open) {
            List<Item> items = new ArrayList<>(PriceCatalog.collectTradeableItems());
            ShopShelfPlanner.Assignment shelves = ShopOffers.current();
            for (Item item : items) {
                String key = ShopOffers.itemKey(item);
                int ref = data.market().getReferencePrice(item);
                boolean hot = data.market().isHot(item);
                for (PresetShop shop : PresetShops.ALL) {
                    if (!shelves.sells(shop.id(), key)) {
                        continue;
                    }
                    int base = data.market().getShopBaseBuyPrice(shop.id(), item);
                    int percent = data.market().getSalePercent(shop.id(), item);
                    p.mall.add(new MallRow(
                            shop.id(),
                            new ItemStack(item),
                            ref,
                            data.market().getShopBuyPrice(shop.id(), item),
                            base,
                            hot,
                            percent
                    ));
                }
            }
            for (com.homestead.sellveg.shop.GearOffers.Offer offer : data.market().gearEnchantOffers()) {
                Item item = offer.stack().getItem();
                int blank = data.market().getShopBaseBuyPrice(offer.shopId(), item);
                p.mall.add(new MallRow(
                        offer.shopId(),
                        offer.stack().copy(),
                        blank,
                        offer.unitPrice(),
                        blank,
                        false,
                        0
                ));
            }
            Collections.shuffle(p.mall, new java.util.Random(
                    data.market().getDayIndex()
                            ^ player.getUUID().getLeastSignificantBits()
                            ^ player.getUUID().getMostSignificantBits()));
            MallBrowse.featureFirst(p.mall, MallRow::sale, MallRow::hot);
        }
        StockIndex.Snapshot stock = StockIndex.scan(player);
        p.unloadedSources = stock.unloadedSources();
        for (StockIndex.StockSlot slot : stock.slots()) {
            ItemStack template = slot.template().copy();
            template.setCount(1);
            Item item = template.getItem();
            int[] band = data.market().getLiveBand(item);
            p.stock.add(new StockRow(
                    template,
                    slot.count(),
                    data.market().getReferencePrice(item),
                    band == null ? 0 : band[0],
                    band == null ? 0 : band[1]));
        }
        for (LedgerEntry entry : data.wallet().ledger()) {
            p.ledger.add(new LedgerRow(entry.timeMillis(), entry.income(), entry.amount(), entry.summary()));
        }
        ShopFeedback.Toast toast = ShopFeedback.take(player);
        if (toast != null) {
            p.toastKey = toast.key();
            p.toastArgs.addAll(toast.args());
        }
        for (PeerShopIndex.Entry entry : PeerShopIndex.visibleTo(player)) {
            p.peerShops.add(new PeerShopRow(entry.ownerId(), entry.name(), entry.listingCount()));
        }
        if (peerTarget != null && !peerTarget.equals(player.getUUID()) && player.getServer() != null) {
            ServerPlayer owner = player.getServer().getPlayerList().getPlayer(peerTarget);
            if (owner != null) {
                PlayerNetShopData ownerData = PlayerNetShopData.get(owner);
                String ownerName = owner.getGameProfile().getName();
                p.includePeerListings = true;
                for (Listing listing : ownerData.shop().listings()) {
                    if (listing.getStack().isEmpty()) {
                        continue;
                    }
                    p.peerListings.add(new PeerListingRow(
                            peerTarget,
                            ownerName,
                            listing.getId(),
                            listing.getStack().copy(),
                            listing.getUnitPrice()));
                }
            }
        }
        return p;
    }

    private static void writeItemId(FriendlyByteBuf buf, Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        buf.writeUtf(id == null ? "minecraft:air" : id.toString(), 128);
    }

    private static Item readItemId(FriendlyByteBuf buf) {
        return BuiltInRegistries.ITEM.get(new ResourceLocation(buf.readUtf(128)));
    }

    public record ListingRow(UUID id, ItemStack stack, int unitPrice, int remainingTicks, boolean hot) {
    }

    public record CartRow(String shopId, ItemStack stack, int count, int unitPrice) {
        public Item item() {
            return stack.getItem();
        }
    }

    public record PointRow(UUID id, String name, boolean isDefault) {
    }

    public record ShipmentRow(UUID id, ItemStack primary, int kindCount, int remainingTicks, boolean blocked, String courierId) {
    }

    public record MallRow(String shopId, ItemStack stack, int refPrice, int buyPrice, int baseBuyPrice, boolean hot, int salePercent) {
        public Item item() {
            return stack.getItem();
        }

        public boolean enchanted() {
            return stack != null && stack.isEnchanted();
        }

        public boolean sale() {
            return salePercent > 0 && !enchanted();
        }
    }

    public record StockRow(ItemStack template, int count, int refPrice, int bandLo, int bandHi) {
        public Item item() {
            return template.getItem();
        }
    }

    public record LedgerRow(long timeMillis, boolean income, long amount, String summary) {
    }

    public record PeerShopRow(UUID ownerId, String name, int listingCount) {
    }

    public record PeerListingRow(UUID ownerId, String ownerName, UUID listingId, ItemStack stack, int unitPrice) {
    }
}
