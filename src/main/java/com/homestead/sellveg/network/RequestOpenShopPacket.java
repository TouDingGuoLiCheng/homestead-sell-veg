package com.homestead.sellveg.network;

import com.homestead.sellveg.market.MarketEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public final class RequestOpenShopPacket {
    private static final UUID NONE = new UUID(0, 0);

    public UUID peerTarget = NONE;

    public static RequestOpenShopPacket openSelf() {
        return new RequestOpenShopPacket();
    }

    /** Asks the server for one online peer's shelf only; the mall payload is not re-sent. */
    public static RequestOpenShopPacket openPeer(UUID peerTarget) {
        RequestOpenShopPacket p = new RequestOpenShopPacket();
        p.peerTarget = peerTarget == null ? NONE : peerTarget;
        return p;
    }

    public static void encode(RequestOpenShopPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.peerTarget == null ? NONE : packet.peerTarget);
    }

    public static RequestOpenShopPacket decode(FriendlyByteBuf buf) {
        RequestOpenShopPacket p = new RequestOpenShopPacket();
        p.peerTarget = buf.readUUID();
        return p;
    }

    public static void handle(RequestOpenShopPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                MarketEvents.ensureMarketInitialized(player);
                UUID target = packet.peerTarget;
                if (target == null || NONE.equals(target)) {
                    ShopSyncPacket.send(player, true);
                } else {
                    ShopSyncPacket.send(player, false, target);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
