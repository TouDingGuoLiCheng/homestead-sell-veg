package com.homestead.sellveg.currency;

import com.homestead.sellveg.data.PlayerNetShopData;
import com.homestead.sellveg.network.ShopFeedback;
import com.homestead.sellveg.network.ShopSyncPacket;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/** Player-to-player transfer. Money only changes hands; no fee, no cap. */
public final class TransferService {
    public enum Result {
        OK, BAD_AMOUNT, SELF, TARGET_OFFLINE, NOT_ENOUGH
    }

    private TransferService() {
    }

    public static Result transfer(ServerPlayer from, UUID targetId, long amount) {
        if (from == null || targetId == null) {
            return Result.TARGET_OFFLINE;
        }
        if (amount <= 0L) {
            return Result.BAD_AMOUNT;
        }
        if (targetId.equals(from.getUUID())) {
            return Result.SELF;
        }
        if (from.getServer() == null) {
            return Result.TARGET_OFFLINE;
        }
        ServerPlayer target = from.getServer().getPlayerList().getPlayer(targetId);
        if (target == null) {
            return Result.TARGET_OFFLINE;
        }

        PlayerNetShopData fromData = PlayerNetShopData.get(from);
        if (!CurrencyHelper.takeCoins(fromData, amount)) {
            return Result.NOT_ENOUGH;
        }
        PlayerNetShopData targetData = PlayerNetShopData.get(target);
        CurrencyHelper.giveCoins(targetData, amount);

        long now = System.currentTimeMillis();
        String fromName = from.getGameProfile().getName();
        String targetName = target.getGameProfile().getName();
        fromData.wallet().record(false, amount, "\u2192 " + targetName, now);
        targetData.wallet().record(true, amount, "\u2190 " + fromName, now);
        PlayerNetShopData.save(from, fromData);
        PlayerNetShopData.save(target, targetData);

        ShopFeedback.toast(from, "message.homestead_sell_veg.transfer_sent", amount, targetName);
        ShopFeedback.toast(target, "message.homestead_sell_veg.transfer_received", amount, fromName);
        ShopSyncPacket.send(target, false);
        ShopSyncPacket.send(from, false);
        return Result.OK;
    }
}
