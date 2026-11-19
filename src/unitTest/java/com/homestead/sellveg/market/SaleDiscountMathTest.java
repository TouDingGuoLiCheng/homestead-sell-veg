package com.homestead.sellveg.market;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SaleDiscountMathTest {
    @Test
    void applyPercentOffRoundsAndFloorsAtOne() {
        assertEquals(10, SaleDiscountMath.applyPercentOff(10, 0));
        assertEquals(9, SaleDiscountMath.applyPercentOff(10, 10));
        assertEquals(1, SaleDiscountMath.applyPercentOff(10, 99));
        assertEquals(1, SaleDiscountMath.applyPercentOff(1, 50));
    }

    @Test
    void rollStaysInTenToNinetyNine() {
        AtomicInteger seq = new AtomicInteger();
        for (int i = 0; i < 300; i++) {
            int p = SaleDiscountMath.rollPercentOff(n -> {
                int v = seq.getAndIncrement();
                return n <= 0 ? 0 : Math.floorMod(v, n);
            });
            assertTrue(p >= 10 && p <= 99, "percent=" + p);
        }
    }

    @Test
    void shallowTierIsTenToTwentyAndMoreCommon() {
        int[] counts = new int[3];
        for (int seed = 0; seed < 1000; seed++) {
            AtomicInteger seq = new AtomicInteger(seed * 17);
            int p = SaleDiscountMath.rollPercentOff(n -> n <= 0 ? 0 : Math.floorMod(seq.getAndIncrement(), n));
            if (p <= 20) {
                counts[0]++;
            } else if (p <= 50) {
                counts[1]++;
            } else {
                counts[2]++;
            }
            assertTrue(p >= 10, "shallow floor=" + p);
        }
        assertTrue(counts[0] > counts[1], "shallow should beat mid");
        assertTrue(counts[1] > counts[2], "mid should beat deep");
    }
}
