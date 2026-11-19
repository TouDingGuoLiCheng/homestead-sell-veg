package com.homestead.sellveg.api;

/** Pure math helpers for mall purchase (safe for JVM unit tests). */
public final class MallPurchaseMath {
    /**
     * Companion bulk buys (construction shortages) are not shop listings.
     * Listing actions stay at {@code StockPacker.LIST_MAX}; this cap only stops a runaway count.
     */
    public static final int MAX_BULK_UNITS = 1_000_000;

    private MallPurchaseMath() {
    }

    /**
     * How many units a purchase request may ask for before wallet and sale-day limits.
     * Gear is always 0 or 1. Other items keep the requested count up to {@link #MAX_BULK_UNITS}.
     */
    public static int requestedUnits(boolean gear, int count) {
        if (count < 1) {
            return 0;
        }
        if (gear) {
            return 1;
        }
        return Math.min(count, MAX_BULK_UNITS);
    }

    /** How many units {@code balance} can pay for at {@code unitPrice}, capped by {@code wanted}. */
    public static int affordableCount(long balance, int unitPrice, int wanted) {
        if (wanted < 1 || unitPrice < 1 || balance < unitPrice) {
            return 0;
        }
        long max = balance / (long) unitPrice;
        return (int) Math.min(wanted, Math.min(Integer.MAX_VALUE, max));
    }
}
