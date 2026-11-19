package com.homestead.sellveg.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Flat rounded card with an item icon and a label, in the spirit of a mobile wallet tile.
 * Rounded corners are painted row by row because {@link GuiGraphics} only fills rectangles.
 */
@OnlyIn(Dist.CLIENT)
public class RoundedCardButton extends Button {
    /** Warm parchment tiles: readable on the golden shop paper without glaring white. */
    public static final int BG = 0xFFE6CE9C;
    public static final int BG_HOVER = 0xFFF0DCB0;
    /** Deeper tile reserved for the balance board so it stays the focal card. */
    public static final int BOARD_BG = 0xFFD8B472;

    private final ItemStack icon;
    private final int accent;
    private final boolean vertical;

    public RoundedCardButton(int x, int y, int width, int height, Component message,
                             ItemStack icon, int accent, OnPress onPress) {
        this(x, y, width, height, message, icon, accent, onPress, false);
    }

    public RoundedCardButton(int x, int y, int width, int height, Component message,
                             ItemStack icon, int accent, OnPress onPress, boolean vertical) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.icon = icon == null ? ItemStack.EMPTY : icon;
        this.accent = accent;
        this.vertical = vertical;
    }

    /** Square tile with the icon above a centred label; used by the shop directory grid. */
    public static RoundedCardButton tile(int x, int y, int width, int height, Component message,
                                         ItemStack icon, OnPress onPress) {
        return new RoundedCardButton(x, y, width, height, message, icon, 0, onPress, true);
    }

    public static RoundedCardButton of(int x, int y, int width, int height, Component message,
                                       ItemStack icon, int accent, OnPress onPress) {
        return new RoundedCardButton(x, y, width, height, message, icon, accent, onPress);
    }

    /** Rounded rectangle: only the outermost {@code radius} rows are inset on each side. */
    public static void drawRounded(GuiGraphics graphics, int x, int y, int w, int h, int radius, int color) {
        int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
        for (int i = 0; i < h; i++) {
            int inset = 0;
            int dy = Math.min(i, h - 1 - i);
            if (dy < r) {
                int v = r - dy - 1;
                inset = r - (int) Math.round(Math.sqrt(Math.max(0, r * r - v * v)));
            }
            graphics.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
        }
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int bg = isHoveredOrFocused() ? BG_HOVER : BG;
        drawRounded(graphics, getX(), getY(), width, height, 4, bg);
        if (vertical) {
            graphics.renderItem(icon, getX() + (width - 16) / 2, getY() + 7);
            graphics.drawCenteredString(Minecraft.getInstance().font, getMessage(),
                    getX() + width / 2, getY() + height - 13, 0xFF3F2A14);
            return;
        }
        if (accent != 0) {
            drawRounded(graphics, getX() + 4, getY() + 6, 3, height - 12, 1, accent);
        }
        var font = Minecraft.getInstance().font;
        int textX = getX() + 10;
        if (!icon.isEmpty()) {
            graphics.renderItem(icon, getX() + 11, getY() + (height - 16) / 2);
            textX = getX() + 32;
        }
        graphics.drawString(font, getMessage(), textX, getY() + (height - 8) / 2, 0xFF3F2A14, false);
    }
}
