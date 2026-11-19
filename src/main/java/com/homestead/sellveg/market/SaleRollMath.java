package com.homestead.sellveg.market;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import java.util.function.ToIntBiFunction;

/**
 * Daily sale listings: many item kinds, at most three shops per item,
 * each listing carries its own percent-off.
 * Items whose discount does not change any shop buy price are skipped.
 */
public final class SaleRollMath {
    public static final int MAX_SHOPS_PER_ITEM = 3;

    public record Listing(String itemKey, String shopId, int percentOff) {
    }

    private SaleRollMath() {
    }

    public static List<Listing> roll(
            List<String> itemKeys,
            Function<String, List<String>> shopsOf,
            ToIntBiFunction<String, String> baseBuyPriceOf,
            int distinctItems,
            IntUnaryOperator randomBelow
    ) {
        List<String> pool = new ArrayList<>(itemKeys);
        List<Listing> out = new ArrayList<>();
        int want = Math.max(0, Math.min(distinctItems, pool.size()));
        int accepted = 0;
        while (accepted < want && !pool.isEmpty()) {
            String item = pool.remove(randomBelow.applyAsInt(pool.size()));
            List<String> shops = new ArrayList<>();
            List<String> available = shopsOf.apply(item);
            if (available != null) {
                shops.addAll(available);
            }
            if (shops.isEmpty()) {
                continue;
            }
            int cap = Math.min(MAX_SHOPS_PER_ITEM, shops.size());
            int n = cap <= 1 ? 1 : 1 + randomBelow.applyAsInt(cap);
            List<Listing> batch = new ArrayList<>();
            boolean anyUnchanged = false;
            for (int s = 0; s < n && !shops.isEmpty(); s++) {
                String shop = shops.remove(randomBelow.applyAsInt(shops.size()));
                int percent = SaleDiscountMath.rollPercentOff(randomBelow);
                int base = Math.max(1, baseBuyPriceOf.applyAsInt(item, shop));
                if (SaleDiscountMath.priceUnchanged(base, percent)) {
                    anyUnchanged = true;
                    break;
                }
                batch.add(new Listing(item, shop, percent));
            }
            if (anyUnchanged || batch.isEmpty()) {
                continue;
            }
            out.addAll(batch);
            accepted++;
        }
        return out;
    }
}
