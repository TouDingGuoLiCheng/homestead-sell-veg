package com.homestead.sellveg.shop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShopProgressionTest {
    @Test
    void levelZeroHasTenSlots() {
        assertEquals(0, ShopProgression.levelForSoldTotal(0));
        assertEquals(10, ShopProgression.listingCapacity(0));
    }

    @Test
    void firstThresholdUnlocksLevelOneAndTwentySlots() {
        assertEquals(1, ShopProgression.levelForSoldTotal(5_000));
        assertEquals(20, ShopProgression.listingCapacity(1));
        assertEquals(0, ShopProgression.levelForSoldTotal(4_999));
    }

    @Test
    void segmentUsesPreviousAndNextThreshold() {
        long[] t = ShopProgression.DEFAULT_THRESHOLDS;
        assertEquals(0L, ShopProgression.previousThreshold(0, t));
        assertEquals(5_000L, ShopProgression.nextThreshold(0, t));
        assertEquals(5_000L, ShopProgression.previousThreshold(1, t));
        assertEquals(10_000L, ShopProgression.nextThreshold(1, t));
        assertEquals(-1L, ShopProgression.nextThreshold(5, t));
        assertEquals(0.4F, ShopProgression.segmentFill(2_000, 0, t), 0.001F);
        assertEquals(0.5F, ShopProgression.segmentFill(7_500, 1, t), 0.001F);
        assertEquals(1.0F, ShopProgression.segmentFill(1_000_000, 5, t), 0.001F);
    }
}
