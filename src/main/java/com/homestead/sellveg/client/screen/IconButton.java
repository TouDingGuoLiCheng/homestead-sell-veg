package com.homestead.sellveg.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class IconButton extends Button {
    private final ResourceLocation texture;
    private final int texSize;

    public IconButton(int x, int y, int size, ResourceLocation texture, int texSize,
                      Component tooltip, OnPress onPress) {
        super(x, y, size, size, tooltip, onPress, DEFAULT_NARRATION);
        this.texture = texture;
        this.texSize = texSize;
        setTooltip(Tooltip.create(tooltip));
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x1 = getX();
        int y1 = getY();
        int x2 = x1 + width;
        int y2 = y1 + height;
        int border = isHoveredOrFocused() ? 0xFFFFFFFF : 0xFFF5F0E6;
        int bg = isHoveredOrFocused() ? 0xFF7A7A7A : 0xFF5A5A5A;
        graphics.fill(x1, y1, x2, y2, bg);
        graphics.fill(x1, y1, x2, y1 + 1, border);
        graphics.fill(x1, y2 - 1, x2, y2, border);
        graphics.fill(x1, y1, x1 + 1, y2, border);
        graphics.fill(x2 - 1, y1, x2, y2, border);
        int pad = Math.max(1, (width - texSize) / 2);
        graphics.blit(texture, x1 + pad, y1 + pad, 0, 0, texSize, texSize, texSize, texSize);
    }
}
