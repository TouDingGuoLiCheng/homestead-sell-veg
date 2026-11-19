package com.homestead.sellveg.client;

import com.homestead.sellveg.client.screen.NetShopScreen;
import com.homestead.sellveg.network.ShopSyncPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Collections;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class ShopClientState {
    public static ShopSyncPacket DATA = new ShopSyncPacket();
    /** Game time when {@link #DATA} was last applied; used for local ETA countdown. */
    public static long syncGameTime;

    private ShopClientState() {
    }

    public static void apply(ShopSyncPacket packet) {
        List<ShopSyncPacket.MallRow> keepMall = DATA.mall;
        List<ShopSyncPacket.PeerListingRow> keepPeers = DATA.peerListings;
        DATA = packet;
        if (!packet.includeMall) {
            DATA.mall.addAll(keepMall);
        }
        // A plain action sync keeps the shelf currently being browsed; opening the mall clears it.
        if (!packet.includePeerListings && !packet.open) {
            DATA.peerListings.addAll(keepPeers);
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            syncGameTime = mc.level.getGameTime();
        }
        if (!packet.toastKey.isEmpty()) {
            Object[] args = packet.toastArgs.toArray(String[]::new);
            ShopToasts.show(net.minecraft.network.chat.Component.translatable(packet.toastKey, args));
        }
        if (packet.open || mc.screen instanceof NetShopScreen) {
            if (!(mc.screen instanceof NetShopScreen)) {
                mc.setScreen(new NetShopScreen());
            } else {
                ((NetShopScreen) mc.screen).refresh();
            }
        }
    }

    public static void clear() {
        DATA = new ShopSyncPacket();
        syncGameTime = 0L;
    }

    public static int displayRemainingTicks(ShopSyncPacket.ShipmentRow row) {
        long now = 0L;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            now = mc.level.getGameTime();
        }
        long elapsed = Math.max(0L, now - syncGameTime);
        return (int) Math.max(0L, row.remainingTicks() - elapsed);
    }

    public static void shuffleMall() {
        Collections.shuffle(DATA.mall);
        MallBrowse.featureFirst(DATA.mall, ShopSyncPacket.MallRow::sale, ShopSyncPacket.MallRow::hot);
    }
}
