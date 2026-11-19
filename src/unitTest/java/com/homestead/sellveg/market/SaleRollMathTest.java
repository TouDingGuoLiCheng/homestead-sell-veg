package com.homestead.sellveg.market;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.ToIntBiFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SaleRollMathTest {
    private static final ToIntBiFunction<String, String> PRICE_100 = (item, shop) -> 100;

    @Test
    void sameItemNeverExceedsThreeShops() {
        Map<String, List<String>> shops = Map.of(
                "bread", List.of("a", "b", "c", "d", "e", "f", "g", "h")
        );
        AtomicInteger seq = new AtomicInteger();
        List<SaleRollMath.Listing> listings = SaleRollMath.roll(
                List.of("bread"),
                shops::get,
                PRICE_100,
                1,
                n -> seq.getAndIncrement() % n
        );
        long breadShops = listings.stream().filter(l -> l.itemKey().equals("bread")).count();
        assertTrue(breadShops <= SaleRollMath.MAX_SHOPS_PER_ITEM);
        assertTrue(breadShops >= 1);
    }

    @Test
    void rollsSeveralDifferentItems() {
        Map<String, List<String>> shops = new HashMap<>();
        shops.put("bread", List.of("s1", "s2", "s3", "s4"));
        shops.put("apple", List.of("s1", "s2"));
        shops.put("carrot", List.of("s1", "s2", "s3"));
        shops.put("wheat", List.of("s1"));
        AtomicInteger seq = new AtomicInteger(1);
        List<SaleRollMath.Listing> listings = SaleRollMath.roll(
                List.of("bread", "apple", "carrot", "wheat"),
                shops::get,
                PRICE_100,
                4,
                n -> seq.getAndIncrement() % n
        );
        long kinds = listings.stream().map(SaleRollMath.Listing::itemKey).distinct().count();
        assertEquals(4, kinds);
        Map<String, Long> perItem = new HashMap<>();
        for (SaleRollMath.Listing listing : listings) {
            perItem.merge(listing.itemKey(), 1L, Long::sum);
        }
        for (long count : perItem.values()) {
            assertTrue(count <= SaleRollMath.MAX_SHOPS_PER_ITEM);
        }
    }

    @Test
    void skipsItemWhenDiscountDoesNotChangePrice() {
        Map<String, List<String>> shops = new HashMap<>();
        shops.put("cheap", List.of("s1"));
        shops.put("pricey", List.of("s1"));
        List<SaleRollMath.Listing> listings = SaleRollMath.roll(
                List.of("cheap", "pricey"),
                shops::get,
                (item, shop) -> "cheap".equals(item) ? 1 : 100,
                2,
                n -> 0
        );
        assertEquals(1, listings.size());
        assertEquals("pricey", listings.get(0).itemKey());
        assertTrue(listings.get(0).percentOff() >= 10);
    }
}
