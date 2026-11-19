package com.homestead.sellveg.data;

import com.homestead.sellveg.block.entity.DropPointBE;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.UUID;

public final class DropPointIndex {
    private DropPointIndex() {
    }

    public static void register(ServerPlayer player, DropPointBE be) {
        UUID id = be.getOrCreatePointId();
        PlayerNetShopData data = PlayerNetShopData.get(player);
        data.dropPoints().removeIf(record -> record.id.equals(id) || record.pos.equals(be.getBlockPos())
                && record.dimension.equals(player.level().dimension()));
        data.dropPoints().add(new DropPointRecord(
                id,
                player.level().dimension(),
                be.getBlockPos(),
                be.getDropPointName()
        ));
        if (data.getDefaultDropPointId() == null) {
            data.setDefaultDropPointId(id);
        }
        PlayerNetShopData.save(player, data);
    }

    public static void unregister(@Nullable ServerPlayer player, UUID pointId) {
        if (player == null) {
            return;
        }
        PlayerNetShopData data = PlayerNetShopData.get(player);
        data.dropPoints().removeIf(record -> record.id.equals(pointId));
        if (pointId.equals(data.getDefaultDropPointId())) {
            data.setDefaultDropPointId(data.dropPoints().isEmpty() ? null : data.dropPoints().get(0).id);
        }
        PlayerNetShopData.save(player, data);
    }

    public static void rename(ServerPlayer player, UUID pointId, String name) {
        PlayerNetShopData data = PlayerNetShopData.get(player);
        data.findDropPoint(pointId).ifPresent(record -> record.name = name);
        PlayerNetShopData.save(player, data);
    }

    public static void pruneMissing(ServerPlayer player) {
        PlayerNetShopData data = PlayerNetShopData.get(player);
        Iterator<DropPointRecord> it = data.dropPoints().iterator();
        while (it.hasNext()) {
            DropPointRecord record = it.next();
            if (resolve(player.server, record, player.getUUID()) == null) {
                it.remove();
            }
        }
        if (data.getDefaultDropPointId() != null
                && data.findDropPoint(data.getDefaultDropPointId()).isEmpty()) {
            data.setDefaultDropPointId(data.dropPoints().isEmpty() ? null : data.dropPoints().get(0).id);
        }
        PlayerNetShopData.save(player, data);
    }

    @Nullable
    public static DropPointBE resolve(net.minecraft.server.MinecraftServer server, DropPointRecord record, UUID owner) {
        ServerLevel level = server.getLevel(record.dimension);
        if (level == null) {
            return null;
        }
        BlockEntity be = level.getBlockEntity(record.pos);
        if (!(be instanceof DropPointBE drop)) {
            return null;
        }
        if (drop.getOwnerId() != null && !drop.getOwnerId().equals(owner)) {
            return null;
        }
        if (drop.getPointId() == null) {
            drop.setPointId(record.id);
        } else if (!record.id.equals(drop.getPointId())) {
            return null;
        }
        return drop;
    }

    @Nullable
    public static DropPointRecord resolveDefault(ServerPlayer player, PlayerNetShopData data) {
        pruneInvalidOnly(player, data);
        UUID def = data.getDefaultDropPointId();
        if (def != null) {
            DropPointRecord record = data.findDropPoint(def).orElse(null);
            if (record != null && resolve(player.server, record, player.getUUID()) != null) {
                return record;
            }
        }
        for (DropPointRecord record : data.dropPoints()) {
            if (resolve(player.server, record, player.getUUID()) != null) {
                data.setDefaultDropPointId(record.id);
                return record;
            }
        }
        data.setDefaultDropPointId(null);
        return null;
    }

    private static void pruneInvalidOnly(ServerPlayer player, PlayerNetShopData data) {
        data.dropPoints().removeIf(record -> resolve(player.server, record, player.getUUID()) == null);
    }
}
