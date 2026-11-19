package com.homestead.sellveg.order;

import com.homestead.sellveg.currency.CurrencyHelper;
import com.homestead.sellveg.data.PlayerNetShopData;
import com.homestead.sellveg.inventory.PlayerItemHelper;
import com.homestead.sellveg.network.ShopFeedback;
import com.homestead.sellveg.network.ShopSyncPacket;
import com.homestead.sellveg.shop.Listing;
import com.homestead.sellveg.shop.ListingService;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Buys one whole listing from another online player. The listing itself is the unit:
 * no partial fills, no cart, no market statistics (goods stay in the market, just change hands).
 */
public final class PeerPurchaseService {
    public enum Result {
        OK, SELF, OWNER_OFFLINE, LISTING_GONE, NOT_ENOUGH_MONEY
    }

    private PeerPurchaseService() {
    }

    public static Result purchase(ServerPlayer buyer, UUID ownerId, UUID listingId) {
        if (buyer == null || ownerId == null || listingId == null) {
            return Result.LISTING_GONE;
        }
        if (ownerId.equals(buyer.getUUID())) {
            return Result.SELF;
        }
        if (buyer.getServer() == null) {
            return Result.OWNER_OFFLINE;
        }
        ServerPlayer owner = buyer.getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null) {
            return Result.OWNER_OFFLINE;
        }

        PlayerNetShopData ownerData = PlayerNetShopData.get(owner);
        Listing listing = null;
        for (Listing candidate : ownerData.shop().listings()) {
            if (candidate.getId().equals(listingId)) {
                listing = candidate;
                break;
            }
        }
        if (listing == null || listing.getStack().isEmpty()) {
            return Result.LISTING_GONE;
        }

        long price = listing.getPayout();
        PlayerNetShopData buyerData = PlayerNetShopData.get(buyer);
        if (buyerData.wallet().balance() < price) {
            return Result.NOT_ENOUGH_MONEY;
        }
        // removeListing is the single take-out point shared with the NPC countdown; empty means sniped.
        if (ownerData.shop().removeListing(listingId).isEmpty()) {
            return Result.LISTING_GONE;
        }
        CurrencyHelper.takeCoins(buyerData, price);

        String ownerName = owner.getGameProfile().getName();
        String buyerName = buyer.getGameProfile().getName();
        String itemName = listing.getStack().getHoverName().getString();
        int count = listing.getStack().getCount();

        buyerData.wallet().record(false, price,
                "\u2192 " + ownerName + " " + itemName + " x" + count, System.currentTimeMillis());
        PlayerNetShopData.save(buyer, buyerData);
        PlayerItemHelper.giveOrDrop(buyer, listing.getStack().copy());

        ListingService.settleToSeller(
                owner,
                ownerData,
                listing,
                "\u2190 " + buyerName + " " + itemName + " x" + count,
                "message.homestead_sell_veg.listing_bought_by_player",
                new Object[]{buyerName, itemName, count, price},
                false);

        ShopFeedback.toast(buyer, "message.homestead_sell_veg.peer_buy_ok", itemName, count, price);
        ShopSyncPacket.send(buyer, false);
        return Result.OK;
    }
}
