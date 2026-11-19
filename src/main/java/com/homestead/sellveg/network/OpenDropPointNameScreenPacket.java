package com.homestead.sellveg.network;

import com.homestead.sellveg.client.screen.DropPointNameScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record OpenDropPointNameScreenPacket(BlockPos pos, String currentName) {
    public static void encode(OpenDropPointNameScreenPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos);
        buf.writeUtf(packet.currentName, 64);
    }

    public static OpenDropPointNameScreenPacket decode(FriendlyByteBuf buf) {
        return new OpenDropPointNameScreenPacket(buf.readBlockPos(), buf.readUtf(64));
    }

    public static void handle(OpenDropPointNameScreenPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> openClient(packet));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void openClient(OpenDropPointNameScreenPacket packet) {
        Minecraft.getInstance().setScreen(new DropPointNameScreen(packet.pos(), packet.currentName()));
    }
}
