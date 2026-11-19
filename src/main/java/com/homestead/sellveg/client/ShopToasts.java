package com.homestead.sellveg.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

@OnlyIn(Dist.CLIENT)
public final class ShopToasts {
    private static final int FADE_IN_MS = 320;
    private static final int HOLD_MS = 2800;
    private static final int FADE_OUT_MS = 480;
    private static final int PAD_X = 10;
    private static final int PAD_Y = 6;
    private static final int MAX_W = 248;

    private static final int BG = 0xFFFDF8F0;
    private static final int EDGE = 0xFF8B5A2B;
    private static final int TEXT = 0xFF3F2A14;

    @Nullable
    private static Active active;

    private ShopToasts() {
    }

    public static void show(Component text) {
        active = new Active(text, System.currentTimeMillis());
    }

    public static void render(GuiGraphics graphics, Font font, int panelLeft, int panelTop, int panelW, int panelH) {
        int bh = font.lineHeight + PAD_Y * 2;
        int cx = panelLeft + panelW / 2;
        // Keep toasts in the upper parchment so they are not hidden under the footer coins/cart row.
        int by = panelTop + 40;
        if (by + bh > panelTop + panelH - 24) {
            by = panelTop + Math.max(8, panelH - bh - 24);
        }
        renderActive(graphics, font, cx, by);
    }

    /** Visible over inventory / chests — ActionBar is covered by those screens. */
    public static void renderWindow(GuiGraphics graphics, Font font, int screenW, int screenH) {
        int by = Math.max(12, screenH / 8);
        renderActive(graphics, font, screenW / 2, by);
    }

    public static void drawImmediate(GuiGraphics graphics, Font font, Component text, int x, int y) {
        drawLine(graphics, font, text.getString(), x, y, 255);
    }

    private static void renderActive(GuiGraphics graphics, Font font, int cx, int by) {
        if (active == null) {
            return;
        }
        long elapsed = System.currentTimeMillis() - active.startMs;
        float alpha = alphaFor(elapsed);
        if (alpha <= 0.01F) {
            active = null;
            return;
        }
        int tw = font.width(font.plainSubstrByWidth(active.text.getString(), MAX_W - PAD_X * 2));
        int bw = tw + PAD_X * 2;
        drawLine(graphics, font, active.text.getString(), cx - bw / 2, by, Mth.ceil(alpha * 255.0F));
    }

    private static void drawLine(GuiGraphics graphics, Font font, String raw, int bx, int by, int alpha) {
        String line = font.plainSubstrByWidth(raw, MAX_W - PAD_X * 2);
        if (line.length() < raw.length()) {
            line = line.trim() + "…";
        }
        int tw = font.width(line);
        int bw = tw + PAD_X * 2;
        int bh = font.lineHeight + PAD_Y * 2;
        // Item icons write depth ~100–200; lift to tooltip Z so the bubble stays in front.
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 400.0F);
        RenderSystem.enableBlend();
        graphics.fill(bx - 1, by - 1, bx + bw + 1, by + bh + 1, withAlpha(EDGE, alpha));
        graphics.fill(bx, by, bx + bw, by + bh, withAlpha(BG, alpha));
        graphics.drawString(font, line, bx + PAD_X, by + PAD_Y, withAlpha(TEXT, alpha), false);
        RenderSystem.disableBlend();
        graphics.pose().popPose();
    }

    private static float alphaFor(long elapsedMs) {
        if (elapsedMs < FADE_IN_MS) {
            return elapsedMs / (float) FADE_IN_MS;
        }
        if (elapsedMs < FADE_IN_MS + HOLD_MS) {
            return 1.0F;
        }
        long fade = elapsedMs - FADE_IN_MS - HOLD_MS;
        if (fade >= FADE_OUT_MS) {
            return 0.0F;
        }
        return 1.0F - fade / (float) FADE_OUT_MS;
    }

    private static int withAlpha(int rgb, int alpha) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (rgb & 0x00FFFFFF);
    }

    private record Active(Component text, long startMs) {
    }
}
