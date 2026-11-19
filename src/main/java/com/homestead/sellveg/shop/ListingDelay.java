package com.homestead.sellveg.shop;

import java.util.Random;

/**
 * Sell delay grows exponentially with price / marketRef.
 * {@code delay = base * e^(k*(r-1))}, clamped to min.
 * Never-sell is {@link #isUnsaleable(int, int, double)} (unit price vs live-band high).
 * If {@code r <= instantRatio}, chance to sell immediately (0 ticks).
 * <p>
 * {@code k} is calibrated so {@code r=2.4} ≈ 24 real hours when base is 6000 ticks.
 */
public final class ListingDelay {
    public static final int NEVER_SELLS = -1;

    /** ln(1728000/6000) / 1.4 — maps r=2.4 to one real day at base=6000. */
    public static final double EXP_K = Math.log(288.0D) / 1.4D;

    private ListingDelay() {
    }

    /** True when unit price is at least {@code liveBandHi * hiMultiplier}. */
    public static boolean isUnsaleable(int unitPrice, int liveBandHi, double hiMultiplier) {
        int hi = Math.max(1, liveBandHi);
        return unitPrice >= hi * hiMultiplier;
    }

    public static int computeDelayTicks(
            double ratio,
            int baseDelay,
            int minDelay,
            double instantRatio,
            double instantChance,
            Random random
    ) {
        if (ratio <= instantRatio && random.nextDouble() < instantChance) {
            return 0;
        }
        double scaled = Math.max(1, baseDelay) * Math.exp(EXP_K * (ratio - 1.0D));
        long delay = Math.round(scaled);
        return (int) Math.max(Math.max(0, minDelay), Math.min(Integer.MAX_VALUE, delay));
    }
}
