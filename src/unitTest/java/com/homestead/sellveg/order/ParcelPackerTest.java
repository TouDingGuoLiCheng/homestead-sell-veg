package com.homestead.sellveg.order;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParcelPackerTest {
    @Test
    void twentyEightStacksNeedTwoBoxesOfTwentySeven() {
        assertEquals(28, ParcelPacker.stacksForCount(28 * 64, 64));
        assertEquals(2, ParcelPacker.boxesForStacks(28, 27));
    }

    @Test
    void oneStackFitsOneBox() {
        assertEquals(1, ParcelPacker.boxesForStacks(ParcelPacker.stacksForCount(16, 64), 27));
    }
}
