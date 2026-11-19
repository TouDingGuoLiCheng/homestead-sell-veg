package com.homestead.sellveg.client;

import com.homestead.sellveg.NetShopMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class StarIcons {
    private static final ResourceLocation EMPTY = new ResourceLocation(NetShopMod.MOD_ID, "textures/gui/star_empty.png");
    private static final ResourceLocation HALF = new ResourceLocation(NetShopMod.MOD_ID, "textures/gui/star_half.png");
    private static final ResourceLocation FULL = new ResourceLocation(NetShopMod.MOD_ID, "textures/gui/star_full.png");
    private static final int SIZE = 16;
    private static final int STEP = 14;

    private StarIcons() {
    }

    public static void blit(GuiGraphics graphics, int x, int y, float rating) {
        for (int i = 0; i < 5; i++) {
            ResourceLocation tex = switch (StarRating.fill(i, rating)) {
                case FULL -> FULL;
                case HALF -> HALF;
                case EMPTY -> EMPTY;
            };
            graphics.blit(tex, x + i * STEP, y, 0, 0, SIZE, SIZE, SIZE, SIZE);
        }
    }

    public static int width() {
        return STEP * 4 + SIZE;
    }

    public static int height() {
        return SIZE;
    }
}
