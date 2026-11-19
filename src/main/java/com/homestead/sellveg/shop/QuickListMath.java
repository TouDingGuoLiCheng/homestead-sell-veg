package com.homestead.sellveg.shop;

import com.homestead.sellveg.stock.StockPacker;

/** Pure helpers for the Quick Lister item (qty + suggest midpoint). */
public final class QuickListMath {
    private QuickListMath() {
    }

    public static int listCount(int slotCount, boolean wholeStack, int maxList) {
        return listCount(slotCount, wholeStack, maxList, false);
    }

    public static int listCount(int slotCount, boolean wholeStack, int maxList, boolean gear) {
        if (slotCount < 1) {
            return 0;
        }
        int want = wholeStack ? slotCount : 1;
        return GearTradeRules.clampTradeCount(gear, want, maxList);
    }

    /** Suggested list unit price: live band midpoint, at least 1. */
    public static int suggestMid(int bandLo, int bandHi) {
        int lo = Math.max(1, bandLo);
        int hi = Math.max(lo, bandHi);
        return Math.max(1, (lo + hi) / 2);
    }
}
