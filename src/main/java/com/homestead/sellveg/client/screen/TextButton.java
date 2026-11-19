package com.homestead.sellveg.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Gray-fill button with a 1px border (no vanilla widgets.png).
 */
@OnlyIn(Dist.CLIENT)
public class TextButton extends Button {
    private final int idleColor;
    private final int hoverColor;
    private final boolean fill;
    private final boolean vertical;

    public TextButton(int x, int y, int width, int height, Component message, OnPress onPress,
                      int idleColor, int hoverColor) {
        this(x, y, width, height, message, onPress, idleColor, hoverColor, false);
    }

    public TextButton(int x, int y, int width, int height, Component message, OnPress onPress,
                      int idleColor, int hoverColor, boolean fill) {
        this(x, y, width, height, message, onPress, idleColor, hoverColor, fill, false);
    }

    public TextButton(int x, int y, int width, int height, Component message, OnPress onPress,
                      int idleColor, int hoverColor, boolean fill, boolean vertical) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.idleColor = idleColor;
        this.hoverColor = hoverColor;
        this.fill = fill;
        this.vertical = vertical;
    }

    public static TextButton woodVertical(int x, int y, int width, int height, Component message, OnPress onPress) {
        return new TextButton(x, y, width, height, message, onPress, 0xFFF5F0E6, 0xFFFFFFFF, true, true);
    }

    public static TextButton wood(int x, int y, int width, int height, Component message, OnPress onPress) {
        return new TextButton(x, y, width, height, message, onPress, 0xFFF5F0E6, 0xFFFFFFFF, true);
    }

    public static TextButton ink(int x, int y, int width, int height, Component message, OnPress onPress) {
        return new TextButton(x, y, width, height, message, onPress, 0xFF3F2A14, 0xFF9A3412, false);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int color = isHoveredOrFocused() ? hoverColor : idleColor;
        int x1 = getX();
        int y1 = getY();
        int x2 = x1 + width;
        int y2 = y1 + height;
        if (fill) {
            int bg = isHoveredOrFocused() ? 0xFF7A7A7A : 0xFF5A5A5A;
            graphics.fill(x1, y1, x2, y2, bg);
        }
        graphics.fill(x1, y1, x2, y1 + 1, color);
        graphics.fill(x1, y2 - 1, x2, y2, color);
        graphics.fill(x1, y1, x1 + 1, y2, color);
        graphics.fill(x2 - 1, y1, x2, y2, color);
        var font = Minecraft.getInstance().font;
        if (vertical) {
            String text = getMessage().getString();
            int total = text.length() * 10;
            int ty = y1 + Math.max(2, (height - total) / 2);
            for (int i = 0; i < text.length(); i++) {
                String ch = text.substring(i, i + 1);
                graphics.drawString(font, ch, x1 + (width - font.width(ch)) / 2, ty + i * 10, color, false);
            }
        } else {
            graphics.drawCenteredString(font, getMessage(), x1 + width / 2, y1 + (height - 8) / 2, color);
        }
    }
}
