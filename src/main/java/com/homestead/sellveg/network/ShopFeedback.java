package com.homestead.sellveg.network;

import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Queues one in-UI toast per player, consumed on the next {@link ShopSyncPacket}. */
public final class ShopFeedback {
    public record Toast(String key, List<String> args) {
    }

    private static final Map<UUID, Toast> PENDING = new ConcurrentHashMap<>();

    private ShopFeedback() {
    }

    public static void toast(ServerPlayer player, String key, Object... args) {
        List<String> encoded = Arrays.stream(args).map(String::valueOf).toList();
        PENDING.put(player.getUUID(), new Toast(key, encoded));
    }

    @Nullable
    public static Toast take(ServerPlayer player) {
        return PENDING.remove(player.getUUID());
    }

    public static void clear(UUID playerId) {
        PENDING.remove(playerId);
    }

    public static void clearAll() {
        PENDING.clear();
    }
}
