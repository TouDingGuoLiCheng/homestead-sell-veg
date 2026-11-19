package com.homestead.sellveg.shop;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GearEnchantRollTest {
    @Test
    void dailyCountIsBetweenThreeAndSeven() {
        Random random = new Random(1L);
        for (int i = 0; i < 40; i++) {
            int n = GearEnchantRoll.pickDailyCount(random);
            assertTrue(n >= 3 && n <= 7, "got " + n);
        }
    }

    @Test
    void enchantCountBiasedLowAndAtMostFive() {
        Random random = new Random(2L);
        int[] hist = new int[6];
        for (int i = 0; i < 3000; i++) {
            int n = GearEnchantRoll.pickEnchantCount(random);
            assertTrue(n >= 1 && n <= 5);
            hist[n]++;
        }
        assertTrue(hist[1] > hist[5]);
        assertTrue(hist[1] > hist[3]);
    }

    @Test
    void selectOffersCapsAtDailyCountAndOnePerItem() {
        List<String> items = List.of("a", "b", "c", "d", "e", "f", "g", "h", "i");
        Random random = new Random(3L);
        List<String> picked = GearEnchantRoll.pickItemKeys(items, 5, random);
        assertEquals(5, picked.size());
        assertEquals(5, picked.stream().distinct().count());
    }

    @Test
    void selectOffersWhenPoolSmallerThanTarget() {
        List<String> items = new ArrayList<>(List.of("a", "b"));
        List<String> picked = GearEnchantRoll.pickItemKeys(items, 7, new Random(4L));
        assertEquals(2, picked.size());
    }
}
