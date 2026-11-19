package com.homestead.sellveg.shop;

import com.homestead.sellveg.market.SaleDiscountMath;

public final class ShopOfferPricing {
    private ShopOfferPricing() {
    }

    public static int bandPick(int lo, int hi, long dayIndex, String shopId, String itemId) {
        int low = Math.max(1, Math.min(lo, hi));
        int high = Math.max(low, Math.max(lo, hi));
        int span = high - low + 1;
        int n = Math.floorMod(hash(dayIndex, shopId, itemId), span);
        return low + n;
    }

    /** {@code salePercentOff} 0 = no sale; 10–99 = percent off after buy markup. */
    public static int buyFromPick(int pick, double buyMul, int salePercentOff) {
        int buy = Math.max(1, (int) Math.round(Math.max(1, pick) * buyMul));
        if (salePercentOff > 0) {
            buy = SaleDiscountMath.applyPercentOff(buy, salePercentOff);
        }
        return buy;
    }

    private static int hash(long dayIndex, String shopId, String itemId) {
        int h = Long.hashCode(dayIndex);
        h = 31 * h + (shopId == null ? 0 : shopId.hashCode());
        h = 31 * h + (itemId == null ? 0 : itemId.hashCode());
        return h;
    }
}
