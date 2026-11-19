package com.homestead.sellveg.client;

import com.homestead.sellveg.NetShopMod;
import com.homestead.sellveg.item.QuickListerItem;
import com.homestead.sellveg.market.PriceCatalog;
import com.homestead.sellveg.shop.GearOffers;
import com.homestead.sellveg.shop.QuickListHints;
import com.homestead.sellveg.shop.QuickListMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

@Mod.EventBusSubscriber(modid = NetShopMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class QuickListTooltipClient {
    private QuickListTooltipClient() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        Player player = event.getEntity();
        if (player == null) {
            player = Minecraft.getInstance().player;
        }
        QuickListHints.Hover hover = hintFor(player, event.getItemStack());
        Component line = lineFor(hover);
        if (line != null) {
            event.getToolTip().add(line);
        }
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) {
            return;
        }
        Slot slot = QuickListScreens.slotAt(screen, event.getMouseX(), event.getMouseY());
        if (slot == null) {
            return;
        }
        QuickListHints.Hover hover = hintFor(player, slot.getItem());
        Component line = lineFor(hover);
        if (line == null) {
            return;
        }
        int x = screen.getGuiLeft() + slot.x + 18;
        int y = Math.max(4, screen.getGuiTop() + slot.y - 8);
        ShopToasts.drawImmediate(event.getGuiGraphics(), mc.font, line, x, y);
    }

    private static QuickListHints.Hover hintFor(@Nullable Player player, ItemStack hovered) {
        if (player == null || hovered == null) {
            return QuickListHints.Hover.none();
        }
        boolean hoveringScroll = hovered.getItem() instanceof QuickListerItem;
        boolean tradeable = !hovered.isEmpty() && PriceCatalog.isTradeable(hovered);
        int mid = hovered.isEmpty() ? 0 : QuickListClientPrices.suggestMid(hovered.getItem());
        if (mid < 1 && tradeable) {
            int[] band = PriceCatalog.marketBand(hovered.getItem());
            if (band != null) {
                mid = QuickListMath.suggestMid(band[0], band[1]);
            }
        }
        // Gear is worth more when enchanted or damaged; match the server-side listing price.
        if (mid >= 1 && GearOffers.isGearItem(hovered.getItem())) {
            mid = Math.max(1, GearOffers.suggestUnitPrice(mid, hovered));
        }
        return QuickListHints.hover(
                QuickListScreens.isQuickListReady(player),
                hoveringScroll,
                hovered.isEmpty(),
                tradeable,
                mid
        );
    }

    @Nullable
    private static Component lineFor(QuickListHints.Hover hover) {
        return switch (hover.kind()) {
            case PRICE -> Component.translatable("item.homestead_sell_veg.quick_lister.suggest_price", hover.price());
            case PENDING -> Component.translatable("item.homestead_sell_veg.quick_lister.suggest_price_pending");
            case NONE -> null;
        };
    }
}
