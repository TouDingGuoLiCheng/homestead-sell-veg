package com.homestead.sellveg.data;

import com.homestead.sellveg.NetShopMod;
import com.homestead.sellveg.currency.WalletAccount;
import com.homestead.sellveg.currency.WalletNbt;
import com.homestead.sellveg.market.MarketData;
import com.homestead.sellveg.order.CartData;
import com.homestead.sellveg.order.Shipment;
import com.homestead.sellveg.shop.ShopData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerNetShopData {
    public static final String ROOT_KEY = NetShopMod.MOD_ID;
    private static final String ROOT = ROOT_KEY;

    /** Server-side live view; flushed to player NBT on save / logout / stop. */
    private static final Map<UUID, PlayerNetShopData> CACHE = new ConcurrentHashMap<>();

    private final MarketData market = new MarketData();
    private final ShopData shop = new ShopData();
    private final CartData cart = new CartData();
    private final WalletAccount wallet = new WalletAccount();
    private final List<Shipment> shipments = new ArrayList<>();
    private final List<DropPointRecord> dropPoints = new ArrayList<>();
    @Nullable
    private UUID defaultDropPointId;

    public MarketData market() {
        return market;
    }

    public ShopData shop() {
        return shop;
    }

    public CartData cart() {
        return cart;
    }

    public WalletAccount wallet() {
        return wallet;
    }

    public List<Shipment> shipments() {
        return shipments;
    }

    public List<DropPointRecord> dropPoints() {
        return dropPoints;
    }

    @Nullable
    public UUID getDefaultDropPointId() {
        return defaultDropPointId;
    }

    public void setDefaultDropPointId(@Nullable UUID defaultDropPointId) {
        this.defaultDropPointId = defaultDropPointId;
    }

    public Optional<DropPointRecord> findDropPoint(UUID id) {
        return dropPoints.stream().filter(p -> p.id.equals(id)).findFirst();
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.put("Market", market.save());
        tag.put("Shop", shop.save());
        tag.put("Cart", cart.save());
        tag.put("Wallet", WalletNbt.save(wallet));
        ListTag ships = new ListTag();
        for (Shipment shipment : shipments) {
            ships.add(shipment.save());
        }
        tag.put("Shipments", ships);
        ListTag points = new ListTag();
        for (DropPointRecord record : dropPoints) {
            points.add(record.save());
        }
        tag.put("DropPoints", points);
        if (defaultDropPointId != null) {
            tag.putUUID("DefaultDropPoint", defaultDropPointId);
        }
        return tag;
    }

    public void load(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return;
        }
        if (tag.contains("Market")) {
            market.load(tag.getCompound("Market"));
        }
        if (tag.contains("Shop")) {
            shop.load(tag.getCompound("Shop"));
        }
        if (tag.contains("Cart")) {
            cart.load(tag.getCompound("Cart"));
        }
        if (tag.contains("Wallet")) {
            WalletNbt.load(wallet, tag.getCompound("Wallet"));
        }
        shipments.clear();
        ListTag ships = tag.getList("Shipments", Tag.TAG_COMPOUND);
        for (int i = 0; i < ships.size(); i++) {
            shipments.add(Shipment.load(ships.getCompound(i)));
        }
        dropPoints.clear();
        ListTag points = tag.getList("DropPoints", Tag.TAG_COMPOUND);
        for (int i = 0; i < points.size(); i++) {
            dropPoints.add(DropPointRecord.load(points.getCompound(i)));
        }
        defaultDropPointId = tag.hasUUID("DefaultDropPoint") ? tag.getUUID("DefaultDropPoint") : null;
    }

    public static PlayerNetShopData get(Player player) {
        if (!player.level().isClientSide()) {
            PlayerNetShopData cached = CACHE.get(player.getUUID());
            if (cached != null) {
                return cached;
            }
        }
        PlayerNetShopData data = new PlayerNetShopData();
        data.load(player.getPersistentData().getCompound(ROOT));
        if (!player.level().isClientSide()) {
            CACHE.put(player.getUUID(), data);
        }
        return data;
    }

    public static void save(Player player, PlayerNetShopData data) {
        if (!player.level().isClientSide()) {
            CACHE.put(player.getUUID(), data);
        }
        player.getPersistentData().put(ROOT, data.save());
    }

    /** Write cached data into the player's persistent NBT without dropping the cache entry. */
    public static void flush(Player player) {
        if (player.level().isClientSide()) {
            return;
        }
        PlayerNetShopData data = CACHE.get(player.getUUID());
        if (data != null) {
            player.getPersistentData().put(ROOT, data.save());
        }
    }

    public static void invalidate(UUID playerId) {
        CACHE.remove(playerId);
    }

    public static void flushAndInvalidate(Player player) {
        flush(player);
        invalidate(player.getUUID());
    }

    public static void clearCache() {
        CACHE.clear();
    }

    public static PlayerNetShopData get(ServerPlayer player) {
        return get((Player) player);
    }
}
