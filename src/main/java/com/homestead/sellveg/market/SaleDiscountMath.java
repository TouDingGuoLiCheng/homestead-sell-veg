package com.homestead.sellveg.market;

import java.util.function.IntUnaryOperator;

/**
 * Per listing sale depth: percent off in {@code [10, 99]}, biased toward shallow cuts.
 */
public final class SaleDiscountMath {
    private SaleDiscountMath() {
    }

    /**
     * Weighted roll: ~70% in 10–20, ~25% in 21–50, ~5% in 51–99.
     * {@code randomBelow} must return {@code 0 .. n-1} for positive {@code n}.
     */
    public static int rollPercentOff(IntUnaryOperator randomBelow) {
        int tier = randomBelow.applyAsInt(100);
        if (tier < 70) {
            return 10 + randomBelow.applyAsInt(11);
        }
        if (tier < 95) {
            return 21 + randomBelow.applyAsInt(30);
        }
        return 51 + randomBelow.applyAsInt(49);
    }

    public static int applyPercentOff(int buyPrice, int percentOff) {
        int p = Math.max(0, Math.min(99, percentOff));
        if (p <= 0) {
            return Math.max(1, buyPrice);
        }
        return Math.max(1, (int) Math.round(buyPrice * (100 - p) / 100.0D));
    }

    /** True when applying {@code percentOff} leaves the buy price unchanged. */
    public static boolean priceUnchanged(int buyPrice, int percentOff) {
        return applyPercentOff(buyPrice, percentOff) == Math.max(1, buyPrice);
    }
}
