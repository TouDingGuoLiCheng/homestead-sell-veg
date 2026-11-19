package com.homestead.sellveg.client;

import com.homestead.sellveg.NetShopMod;
import com.homestead.sellveg.item.QuickListerItem;
import com.homestead.sellveg.network.ModNetwork;
import com.homestead.sellveg.network.QuickListFromSlotPacket;
import com.homestead.sellveg.shop.QuickListHints;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Intercept inventory right-clicks while the Quick Listing Scroll is on the cursor
 * or selected in the hotbar (main hand), matching "拿着卷轴点格子" usage.
 */
@Mod.EventBusSubscriber(modid = NetShopMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class QuickListClickClient {
    private QuickListClickClient() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() != 1) {
            return;
        }
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || player.containerMenu == null) {
            return;
        }
        if (!QuickListScreens.isQuickListReady(player)) {
            return;
        }
        Slot slot = QuickListScreens.slotAt(screen, event.getMouseX(), event.getMouseY());
        if (slot == null) {
            return;
        }
        boolean hoveringScroll = slot.getItem().getItem() instanceof QuickListerItem;
        if (!QuickListHints.shouldInterceptClick(
                true,
                hoveringScroll,
                slot.getItem().isEmpty()
        )) {
            return;
        }
        int slotIndex = QuickListScreens.menuIndex(screen, slot);
        if (slotIndex < 0) {
            return;
        }
        event.setCanceled(true);
        ModNetwork.CHANNEL.sendToServer(new QuickListFromSlotPacket(
                player.containerMenu.containerId,
                slotIndex,
                Screen.hasShiftDown()
        ));
    }

    /** Cursor holds the scroll, or cursor empty and main hand holds it. */
    public static boolean isQuickListReady(Player player) {
        return QuickListScreens.isQuickListReady(player);
    }
}
