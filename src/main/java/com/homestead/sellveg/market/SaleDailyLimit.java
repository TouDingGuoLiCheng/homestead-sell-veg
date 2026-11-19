package com.homestead.sellveg.market;

import com.homestead.sellveg.config.SellVegConfig;

/**
 * Daily cap on how many discounted (sale) items a player may buy.
 */
public final class SaleDailyLimit {
    public static final int DEFAULT_CAP = 64;

    private SaleDailyLimit() {
    }

    public static int cap() {
        try {
            return SellVegConfig.SALE_DAILY_CAP.get();
        } catch (Throwable ignored) {
            return DEFAULT_CAP;
        }
    }

    public static int remaining(int boughtToday, int saleInCart) {
        return remaining(boughtToday, saleInCart, cap());
    }

    public static int remaining(int boughtToday, int saleInCart, int cap) {
        return Math.max(0, cap - boughtToday - saleInCart);
    }

    /** {@code additionalSaleUnits} is the increase in sale-item count for this action. */
    public static boolean canAdd(int boughtToday, int saleInCart, int additionalSaleUnits) {
        return canAdd(boughtToday, saleInCart, additionalSaleUnits, cap());
    }

    public static boolean canAdd(int boughtToday, int saleInCart, int additionalSaleUnits, int cap) {
        if (additionalSaleUnits <= 0) {
            return true;
        }
        return (long) boughtToday + saleInCart + additionalSaleUnits <= cap;
    }
}
