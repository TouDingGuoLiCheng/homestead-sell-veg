package com.homestead.sellveg.data;

import com.homestead.sellveg.market.FoodRecipeIndex;
import com.homestead.sellveg.network.ShopFeedback;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Keeps {@link PlayerNetShopData} cache coherent across login / death / logout / stop.
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PlayerShopLifecycle {
    private PlayerShopLifecycle() {
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (event.getOriginal() instanceof ServerPlayer original) {
            PlayerNetShopData.flush(original);
        }
        CompoundTag tag = event.getOriginal().getPersistentData().getCompound(PlayerNetShopData.ROOT_KEY);
        if (!tag.isEmpty()) {
            player.getPersistentData().put(PlayerNetShopData.ROOT_KEY, tag.copy());
        }
        PlayerNetShopData.invalidate(player.getUUID());
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        PlayerNetShopData.flushAndInvalidate(player);
        ShopFeedback.clear(player.getUUID());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            PlayerNetShopData.flush(player);
        }
        PlayerNetShopData.clearCache();
        ShopFeedback.clearAll();
        FoodRecipeIndex.clear();
    }
}
