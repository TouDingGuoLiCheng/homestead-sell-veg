package com.homestead.sellveg.network;

import com.homestead.sellveg.block.entity.DropPointBE;
import com.homestead.sellveg.data.DropPointIndex;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SetDropPointNamePacket(BlockPos pos, String name) {
    public static void encode(SetDropPointNamePacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos);
        buf.writeUtf(packet.name, 64);
    }

    public static SetDropPointNamePacket decode(FriendlyByteBuf buf) {
        return new SetDropPointNamePacket(buf.readBlockPos(), buf.readUtf(64));
    }

    public static void handle(SetDropPointNamePacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            if (player.distanceToSqr(packet.pos.getX() + 0.5, packet.pos.getY() + 0.5, packet.pos.getZ() + 0.5) > 64.0) {
                return;
            }
            BlockEntity be = player.level().getBlockEntity(packet.pos);
            if (be instanceof DropPointBE dropPoint) {
                if (dropPoint.getOwnerId() != null && !dropPoint.getOwnerId().equals(player.getUUID())) {
                    return;
                }
                dropPoint.setDropPointName(packet.name);
                dropPoint.setChanged();
                if (dropPoint.getPointId() != null) {
                    DropPointIndex.rename(player, dropPoint.getPointId(), dropPoint.getDropPointName());
                }
                player.level().sendBlockUpdated(packet.pos, be.getBlockState(), be.getBlockState(), 3);
            }
        });
        context.setPacketHandled(true);
    }
}
