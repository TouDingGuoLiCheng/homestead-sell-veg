package com.homestead.sellveg;

import com.homestead.sellveg.order.DeliveryService;
import com.homestead.sellveg.shop.ListingService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Single player-tick pass for listings and deliveries (avoids double NBT work). */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PlayerShopTicker {
    private PlayerShopTicker() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }
        if (event.player instanceof ServerPlayer player) {
            ListingService.tick(player);
            DeliveryService.tick(player);
        }
    }
}
