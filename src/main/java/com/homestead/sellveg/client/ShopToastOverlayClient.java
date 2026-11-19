package com.homestead.sellveg.client;

import com.homestead.sellveg.NetShopMod;
import com.homestead.sellveg.client.screen.NetShopScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = NetShopMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ShopToastOverlayClient {
    private ShopToastOverlayClient() {
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        if (screen instanceof NetShopScreen) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ShopToasts.renderWindow(event.getGuiGraphics(), mc.font, screen.width, screen.height);
    }
}
