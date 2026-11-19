package com.homestead.sellveg.network;

import com.homestead.sellveg.NetShopMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL = "10";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(NetShopMod.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    private static int id;

    private ModNetwork() {
    }

    public static void register() {
        CHANNEL.messageBuilder(OpenDropPointNameScreenPacket.class, id++)
                .decoder(OpenDropPointNameScreenPacket::decode)
                .encoder(OpenDropPointNameScreenPacket::encode)
                .consumerMainThread(OpenDropPointNameScreenPacket::handle)
                .add();
        CHANNEL.messageBuilder(SetDropPointNamePacket.class, id++)
                .decoder(SetDropPointNamePacket::decode)
                .encoder(SetDropPointNamePacket::encode)
                .consumerMainThread(SetDropPointNamePacket::handle)
                .add();
        CHANNEL.messageBuilder(RequestOpenShopPacket.class, id++)
                .decoder(RequestOpenShopPacket::decode)
                .encoder(RequestOpenShopPacket::encode)
                .consumerMainThread(RequestOpenShopPacket::handle)
                .add();
        CHANNEL.messageBuilder(ShopSyncPacket.class, id++)
                .decoder(ShopSyncPacket::decode)
                .encoder(ShopSyncPacket::encode)
                .consumerMainThread(ShopSyncPacket::handle)
                .add();
        CHANNEL.messageBuilder(ShopActionPacket.class, id++)
                .decoder(ShopActionPacket::decode)
                .encoder(ShopActionPacket::encode)
                .consumerMainThread(ShopActionPacket::handle)
                .add();
        CHANNEL.messageBuilder(QuickListPriceSyncPacket.class, id++)
                .decoder(QuickListPriceSyncPacket::decode)
                .encoder(QuickListPriceSyncPacket::encode)
                .consumerMainThread(QuickListPriceSyncPacket::handle)
                .add();
        CHANNEL.messageBuilder(QuickListFromSlotPacket.class, id++)
                .decoder(QuickListFromSlotPacket::decode)
                .encoder(QuickListFromSlotPacket::encode)
                .consumerMainThread(QuickListFromSlotPacket::handle)
                .add();
    }
}
