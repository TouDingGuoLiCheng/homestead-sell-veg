package com.homestead.sellveg.order;

import com.homestead.sellveg.block.entity.ParcelBoxBE;
import com.homestead.sellveg.config.SellVegConfig;
import com.homestead.sellveg.data.DropPointIndex;
import com.homestead.sellveg.data.DropPointRecord;
import com.homestead.sellveg.data.PlayerNetShopData;
import com.homestead.sellveg.network.ShopFeedback;
import com.homestead.sellveg.network.ShopSyncPacket;
import com.homestead.sellveg.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class DeliveryService {
    private DeliveryService() {
    }

    public static void tick(ServerPlayer player) {
        PlayerNetShopData data = PlayerNetShopData.get(player);
        if (data.shipments().isEmpty()) {
            return;
        }
        List<UUID> due = new ArrayList<>();
        boolean dirty = false;
        for (Shipment shipment : data.shipments()) {
            int before = shipment.getRemainingTicks();
            if (shipment.tickDown()) {
                due.add(shipment.getId());
            }
            if (shipment.getRemainingTicks() != before) {
                dirty = true;
            }
        }
        if (dirty && player.tickCount % 20 == 0) {
            PlayerNetShopData.save(player, data);
        }
        for (UUID id : due) {
            tryDeliver(player, id);
        }
    }

    public static void rushAll(ServerPlayer player) {
        PlayerNetShopData data = PlayerNetShopData.get(player);
        for (Shipment shipment : data.shipments()) {
            shipment.setRemainingTicks(1);
        }
        PlayerNetShopData.save(player, data);
    }

    public static boolean tryDeliver(ServerPlayer player, UUID shipmentId) {
        PlayerNetShopData data = PlayerNetShopData.get(player);
        Shipment shipment = data.shipments().stream()
                .filter(s -> s.getId().equals(shipmentId))
                .findFirst()
                .orElse(null);
        if (shipment == null) {
            return false;
        }
        DropPointRecord target = DropPointIndex.resolveDefault(player, data);
        if (target == null) {
            shipment.markBlocked();
            boolean notify = shipment.consumeBlockedNotify();
            PlayerNetShopData.save(player, data);
            if (notify) {
                ShopFeedback.toast(player, "message.homestead_sell_veg.delivery_no_point");
                ShopSyncPacket.send(player, false);
            }
            return false;
        }
        ServerLevel level = player.server.getLevel(target.dimension);
        if (level == null) {
            shipment.markBlocked();
            boolean notify = shipment.consumeBlockedNotify();
            PlayerNetShopData.save(player, data);
            if (notify) {
                ShopSyncPacket.send(player, false);
            }
            return false;
        }
        BlockPos place = findPlacePos(level, target.pos);
        if (place == null) {
            shipment.markBlocked();
            boolean notify = shipment.consumeBlockedNotify();
            PlayerNetShopData.save(player, data);
            if (notify) {
                ShopFeedback.toast(player, "message.homestead_sell_veg.delivery_blocked", target.name);
                ShopSyncPacket.send(player, false);
            }
            return false;
        }
        if (!placeBox(level, place, shipment.getContents())) {
            shipment.markBlocked();
            boolean notify = shipment.consumeBlockedNotify();
            PlayerNetShopData.save(player, data);
            if (notify) {
                ShopSyncPacket.send(player, false);
            }
            return false;
        }
        data.shipments().removeIf(s -> s.getId().equals(shipmentId));
        PlayerNetShopData.save(player, data);
        String arrivedKey = DeliveryPhrases.langKey(shipmentId);
        chat(player, Component.translatable(arrivedKey, target.name));
        ShopFeedback.toast(player, arrivedKey, target.name);
        try {
            if (SellVegConfig.DELIVERY_SOUND_ENABLED.get()) {
                player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7F, 1.2F);
            }
        } catch (Exception ignored) {
            player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7F, 1.2F);
        }
        ShopSyncPacket.send(player, false);
        return true;
    }

    private static BlockPos findPlacePos(ServerLevel level, BlockPos dropPoint) {
        int max = SellVegConfig.DELIVERY_COLUMN_MAX.get();
        BlockPos cursor = dropPoint.above();
        for (int i = 0; i < max; i++) {
            BlockState state = level.getBlockState(cursor);
            if (state.isAir()) {
                return cursor;
            }
            if (!state.is(ModBlocks.PARCEL_BOX.get())) {
                return null;
            }
            cursor = cursor.above();
        }
        return null;
    }

    private static boolean placeBox(ServerLevel level, BlockPos pos, NonNullList<ItemStack> contents) {
        if (!level.setBlock(pos, ModBlocks.PARCEL_BOX.get().defaultBlockState(), 3)) {
            return false;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ParcelBoxBE parcel)) {
            return false;
        }
        NonNullList<ItemStack> copy = NonNullList.withSize(ParcelBoxBE.SLOT_CAPACITY, ItemStack.EMPTY);
        for (int i = 0; i < Math.min(copy.size(), contents.size()); i++) {
            copy.set(i, contents.get(i).copy());
        }
        parcel.setItems(copy);
        return true;
    }

    private static void chat(ServerPlayer player, Component message) {
        try {
            if (!SellVegConfig.DELIVERY_CHAT_ENABLED.get()) {
                return;
            }
        } catch (Exception ignored) {
            //
        }
        player.sendSystemMessage(message);
    }
}
