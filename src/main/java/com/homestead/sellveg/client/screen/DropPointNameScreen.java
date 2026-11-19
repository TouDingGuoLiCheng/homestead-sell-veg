package com.homestead.sellveg.client.screen;

import com.homestead.sellveg.network.ModNetwork;
import com.homestead.sellveg.network.SetDropPointNamePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DropPointNameScreen extends Screen {
    private final BlockPos pos;
    private final String initialName;
    private EditBox nameBox;

    public DropPointNameScreen(BlockPos pos, String initialName) {
        super(Component.translatable("gui.homestead_sell_veg.drop_point_name"));
        this.pos = pos;
        this.initialName = initialName;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        nameBox = new EditBox(this.font, centerX - 100, centerY - 10, 200, 20,
                Component.translatable("gui.homestead_sell_veg.drop_point_name"));
        nameBox.setMaxLength(32);
        nameBox.setValue(initialName);
        addRenderableWidget(nameBox);
        setInitialFocus(nameBox);

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> confirm())
                .bounds(centerX - 100, centerY + 20, 98, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(centerX + 2, centerY + 20, 98, 20)
                .build());
    }

    private void confirm() {
        ModNetwork.CHANNEL.sendToServer(new SetDropPointNamePacket(pos, nameBox.getValue()));
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 40, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
