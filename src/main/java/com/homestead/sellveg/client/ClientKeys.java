package com.homestead.sellveg.client;

import com.homestead.sellveg.NetShopMod;
import com.homestead.sellveg.network.ModNetwork;
import com.homestead.sellveg.network.RequestOpenShopPacket;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = NetShopMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientKeys {
    public static final KeyMapping OPEN_SHOP = new KeyMapping(
            "key.homestead_sell_veg.open_shop",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            "key.categories.homestead_sell_veg"
    );

    private ClientKeys() {
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_SHOP);
    }

    @Mod.EventBusSubscriber(modid = NetShopMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ForgeEvents {
        private static boolean appliedUnboundDefault;

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            if (!appliedUnboundDefault) {
                appliedUnboundDefault = true;
                if (OPEN_SHOP.isUnbound()) {
                    OPEN_SHOP.setToDefault();
                    KeyMapping.resetMapping();
                }
            }
            if (mc.player == null || mc.screen != null) {
                return;
            }
            while (OPEN_SHOP.consumeClick()) {
                ModNetwork.CHANNEL.sendToServer(new RequestOpenShopPacket());
            }
        }
    }
}
