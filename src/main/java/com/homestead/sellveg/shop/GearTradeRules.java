package com.homestead.sellveg.shop;

import com.homestead.sellveg.stock.StockPacker;

/**
 * Pure buy/list quantity and durability percent helpers for weapon/armor trades.
 */
public final class GearTradeRules {
    private GearTradeRules() {
    }

    /** Pure clamp: gear → 0/1; otherwise {@link StockPacker#clampListCount}. */
    public static int clampTradeCount(boolean gear, int count, int maxNonGear) {
        if (gear) {
            return count < 1 ? 0 : 1;
        }
        return StockPacker.clampListCount(count, maxNonGear);
    }

    public static int tradeQtyCap(boolean gear, int available) {
        if (gear) {
            return available < 1 ? 0 : 1;
        }
        return StockPacker.listQtyCap(available);
    }

    /**
     * Remaining durability as percent 0–100, or {@code -1} if not damageable.
     */
    public static int durabilityPercent(int damage, int maxDamage) {
        if (maxDamage <= 0) {
            return -1;
        }
        int left = Math.max(0, maxDamage - Math.max(0, damage));
        int pct = (int) Math.round(100.0D * left / (double) maxDamage);
        return Math.max(0, Math.min(100, pct));
    }

    /** {@code "87%"} or null when percent is unavailable. */
    public static String durabilityPercentLabel(int damage, int maxDamage) {
        int pct = durabilityPercent(damage, maxDamage);
        return pct < 0 ? null : pct + "%";
    }
}
