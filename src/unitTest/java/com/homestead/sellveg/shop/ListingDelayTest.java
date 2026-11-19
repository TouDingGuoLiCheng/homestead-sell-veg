package com.homestead.sellveg.shop;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListingDelayTest {
    private static final int BASE = 6000;
    private static final int MIN = 20;
    private static final double HI_MULT = 2.0D;
    private static final double INSTANT_RATIO = 0.85D;
    private static final double INSTANT_CHANCE = 0.5D;

    @Test
    void equalPriceUsesBaseDelay() {
        assertEquals(BASE, delay(1.0D, alwaysMissInstant()));
    }

    @Test
    void nearCutoffApproachesOneRealDay() {
        assertEquals(1_728_000, delay(2.4D, alwaysMissInstant()));
    }

    @Test
    void unsaleableUsesTwiceLiveBandHigh() {
        assertFalse(ListingDelay.isUnsaleable(28, 28, HI_MULT));
        assertFalse(ListingDelay.isUnsaleable(55, 28, HI_MULT));
        assertTrue(ListingDelay.isUnsaleable(56, 28, HI_MULT));
        assertTrue(ListingDelay.isUnsaleable(57, 28, HI_MULT));
    }

    @Test
    void highRefRatioStillComputesWait() {
        int at25 = delay(2.5D, alwaysMissInstant());
        int at30 = delay(3.0D, alwaysMissInstant());
        assertTrue(at25 > 1_728_000);
        assertTrue(at30 > at25);
        assertTrue(at25 != ListingDelay.NEVER_SELLS);
        assertTrue(at30 != ListingDelay.NEVER_SELLS);
    }

    @Test
    void exponentialGrowsWithRatio() {
        int at15 = delay(1.5D, alwaysMissInstant());
        int at20 = delay(2.0D, alwaysMissInstant());
        assertTrue(at15 > BASE);
        assertTrue(at20 > at15);
        assertTrue(at20 > 300_000);
    }

    @Test
    void underpricedCanInstantSell() {
        assertEquals(0, delay(0.5D, alwaysInstant()));
    }

    @Test
    void underpricedWithoutInstantStillFaster() {
        int wait = delay(0.5D, alwaysMissInstant());
        assertTrue(wait < BASE);
        assertTrue(wait >= MIN);
    }

    private static int delay(double ratio, Random random) {
        return ListingDelay.computeDelayTicks(ratio, BASE, MIN, INSTANT_RATIO, INSTANT_CHANCE, random);
    }

    private static Random alwaysInstant() {
        return new Random() {
            @Override
            public double nextDouble() {
                return 0.0D;
            }
        };
    }

    private static Random alwaysMissInstant() {
        return new Random() {
            @Override
            public double nextDouble() {
                return 0.99D;
            }
        };
    }
}
