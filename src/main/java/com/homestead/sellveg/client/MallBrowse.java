package com.homestead.sellveg.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;

public final class MallBrowse {
    private MallBrowse() {
    }

    public static boolean matchesQuery(String query, String itemDisplayName, String itemId) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            return true;
        }
        String name = itemDisplayName == null ? "" : itemDisplayName.toLowerCase(Locale.ROOT);
        String id = itemId == null ? "" : itemId.toLowerCase(Locale.ROOT);
        return name.contains(q) || id.contains(q);
    }

    /**
     * Filters mall-like rows by optional shop id and search query, preserving source order
     * (callers that already {@link #featureFirst} the source need not re-sort).
     */
    public static <T> List<T> filterByShopAndQuery(
            List<T> source,
            String shopFilter,
            String query,
            Function<T, String> shopId,
            Function<T, String> displayName,
            Function<T, String> itemId) {
        String shop = shopFilter == null ? "" : shopFilter;
        boolean hasQuery = query != null && !query.trim().isEmpty();
        List<T> out = new ArrayList<>();
        for (T row : source) {
            if (!shop.isEmpty() && !shop.equals(shopId.apply(row))) {
                continue;
            }
            if (!hasQuery || matchesQuery(query, displayName.apply(row), itemId.apply(row))) {
                out.add(row);
            }
        }
        return out;
    }

    /** Prefer sale rows, then hot rows (sale+hot highest). */
    public static <T> void featureFirst(List<T> rows, Predicate<T> sale, Predicate<T> hot) {
        rows.sort((a, b) -> Integer.compare(featureRank(b, sale, hot), featureRank(a, sale, hot)));
    }

    public static <T> void saleFirst(List<T> rows, Predicate<T> sale) {
        featureFirst(rows, sale, t -> false);
    }

    private static <T> int featureRank(T row, Predicate<T> sale, Predicate<T> hot) {
        int rank = 0;
        if (sale.test(row)) {
            rank += 2;
        }
        if (hot.test(row)) {
            rank += 1;
        }
        return rank;
    }
}
