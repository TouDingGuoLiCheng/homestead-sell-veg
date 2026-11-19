package com.homestead.sellveg.network;

import com.homestead.sellveg.client.QuickListClientPrices;
import com.homestead.sellveg.data.PlayerNetShopData;
import com.homestead.sellveg.market.MarketEvents;
import com.homestead.sellveg.market.PriceCatalog;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Syncs live suggest-mid prices for Quick Lister tooltips. */
public final class QuickListPriceSyncPacket {
    public final Map<ResourceLocation, Integer> mids = new HashMap<>();

    public static QuickListPriceSyncPacket build(ServerPlayer player) {
        MarketEvents.ensureMarketInitialized(player);
        PlayerNetShopData data = PlayerNetShopData.get(player);
        QuickListPriceSyncPacket packet = new QuickListPriceSyncPacket();
        for (Item item : PriceCatalog.collectTradeableItems()) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (id == null) {
                continue;
            }
            packet.mids.put(id, Math.max(1, data.market().getSuggestMid(item)));
        }
        return packet;
    }

    public static void send(ServerPlayer player) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), build(player));
    }

    public static void encode(QuickListPriceSyncPacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.mids.size());
        for (Map.Entry<ResourceLocation, Integer> e : p.mids.entrySet()) {
            buf.writeResourceLocation(e.getKey());
            buf.writeVarInt(e.getValue());
        }
    }

    public static QuickListPriceSyncPacket decode(FriendlyByteBuf buf) {
        QuickListPriceSyncPacket p = new QuickListPriceSyncPacket();
        int n = buf.readVarInt();
        for (int i = 0; i < n; i++) {
            ResourceLocation id = buf.readResourceLocation();
            int mid = Math.max(1, buf.readVarInt());
            p.mids.put(id, mid);
        }
        return p;
    }

    public static void handle(QuickListPriceSyncPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> QuickListClientPrices.apply(packet)));
        ctx.get().setPacketHandled(true);
    }
}
