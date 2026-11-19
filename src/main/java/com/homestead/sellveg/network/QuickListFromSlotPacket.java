package com.homestead.sellveg.network;

import com.homestead.sellveg.shop.QuickListService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client asks server to quick-list from a menu slot. */
public final class QuickListFromSlotPacket {
    public final int containerId;
    public final int slotIndex;
    public final boolean wholeStack;

    public QuickListFromSlotPacket(int containerId, int slotIndex, boolean wholeStack) {
        this.containerId = containerId;
        this.slotIndex = slotIndex;
        this.wholeStack = wholeStack;
    }

    public static void encode(QuickListFromSlotPacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.containerId);
        buf.writeVarInt(p.slotIndex);
        buf.writeBoolean(p.wholeStack);
    }

    public static QuickListFromSlotPacket decode(FriendlyByteBuf buf) {
        return new QuickListFromSlotPacket(buf.readVarInt(), buf.readVarInt(), buf.readBoolean());
    }

    public static void handle(QuickListFromSlotPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            QuickListService.listFromMenuSlot(player, packet.containerId, packet.slotIndex, packet.wholeStack);
        });
        ctx.get().setPacketHandled(true);
    }
}
