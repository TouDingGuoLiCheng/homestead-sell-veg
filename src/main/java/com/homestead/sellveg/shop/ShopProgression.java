package com.homestead.sellveg.shop;

import com.homestead.sellveg.config.SellVegConfig;

import java.util.List;

public final class ShopProgression {
    public static final long[] DEFAULT_THRESHOLDS = {5_000L, 10_000L, 100_000L, 500_000L, 1_000_000L};

    private ShopProgression() {
    }

    public static long[] thresholds() {
        try {
            List<? extends Integer> configured = SellVegConfig.SHOP_UPGRADE_THRESHOLDS.get();
            if (configured == null || configured.size() < 5) {
                return DEFAULT_THRESHOLDS;
            }
            long[] values = new long[5];
            for (int i = 0; i < 5; i++) {
                values[i] = configured.get(i).longValue();
            }
            return values;
        } catch (Exception ignored) {
            return DEFAULT_THRESHOLDS;
        }
    }

    public static int levelForSoldTotal(long soldTotal) {
        return levelForSoldTotal(soldTotal, DEFAULT_THRESHOLDS);
    }

    public static int levelForSoldTotal(long soldTotal, long[] thresholds) {
        int next = 0;
        for (long threshold : thresholds) {
            if (soldTotal >= threshold) {
                next++;
            }
        }
        return Math.min(5, next);
    }

    public static int listingCapacity(int level) {
        return 10 + Math.max(0, Math.min(5, level)) * 10;
    }

    /** Sold total already counted toward the current level (0 at Lv.0). */
    public static long previousThreshold(int level, long[] thresholds) {
        if (level <= 0 || thresholds.length == 0) {
            return 0L;
        }
        return thresholds[Math.min(level, thresholds.length) - 1];
    }

    /** Sold total needed for the next level, or {@code -1} at max level. */
    public static long nextThreshold(int level, long[] thresholds) {
        if (level < 0 || level >= thresholds.length) {
            return -1L;
        }
        return thresholds[level];
    }

    public static float segmentFill(long soldTotal, int level, long[] thresholds) {
        long next = nextThreshold(level, thresholds);
        if (next < 0) {
            return 1.0F;
        }
        long prev = previousThreshold(level, thresholds);
        long span = Math.max(1L, next - prev);
        long cur = Math.max(0L, soldTotal - prev);
        return Math.min(1.0F, cur / (float) span);
    }
}
