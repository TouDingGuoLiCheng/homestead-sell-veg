package com.homestead.sellveg.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.UUID;

/** Rounded row showing a player's head plus a label; used for the recipient picker. */
@OnlyIn(Dist.CLIENT)
public class PlayerFaceButton extends Button {
    private final UUID playerId;

    public PlayerFaceButton(int x, int y, int width, int height, Component message, UUID playerId, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.playerId = playerId;
    }

    /** Draws the 8x8 face at the requested size; falls back to a flat tile when the skin is unknown. */
    public static void drawFace(GuiGraphics graphics, UUID playerId, int x, int y, int size) {
        ResourceLocation skin = skinOf(playerId);
        if (skin == null) {
            graphics.fill(x, y, x + size, y + size, 0xFF9CA3AF);
            return;
        }
        graphics.blit(skin, x, y, size, size, 8.0F, 8.0F, 8, 8, 64, 64);
        graphics.blit(skin, x, y, size, size, 40.0F, 8.0F, 8, 8, 64, 64);
    }

    private static ResourceLocation skinOf(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return null;
        }
        PlayerInfo info = connection.getPlayerInfo(playerId);
        return info == null ? null : info.getSkinLocation();
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        RoundedCardButton.drawRounded(graphics, getX(), getY(), width, height, 3,
                isHoveredOrFocused() ? RoundedCardButton.BG_HOVER : RoundedCardButton.BG);
        drawFace(graphics, playerId, getX() + 3, getY() + (height - 12) / 2, 12);
        graphics.drawString(Minecraft.getInstance().font, getMessage(), getX() + 19,
                getY() + (height - 8) / 2, 0xFF3F2A14, false);
    }
}
