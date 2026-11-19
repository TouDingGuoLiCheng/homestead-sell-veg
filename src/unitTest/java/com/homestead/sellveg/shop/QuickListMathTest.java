package com.homestead.sellveg.shop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuickListMathTest {
    @Test
    void rightClickListsOne() {
        assertEquals(1, QuickListMath.listCount(32, false, 64));
    }

    @Test
    void shiftRightClickListsWholeStackCapped() {
        assertEquals(64, QuickListMath.listCount(80, true, 64));
        assertEquals(32, QuickListMath.listCount(32, true, 64));
    }

    @Test
    void gearListsAlwaysOneEvenWholeStack() {
        assertEquals(1, QuickListMath.listCount(32, true, 64, true));
        assertEquals(1, QuickListMath.listCount(32, false, 64, true));
    }

    @Test
    void emptySlotListsNothing() {
        assertEquals(0, QuickListMath.listCount(0, true, 64));
        assertEquals(0, QuickListMath.listCount(0, false, 64));
    }

    @Test
    void suggestMidIsBandMidpointAtLeastOne() {
        assertEquals(7, QuickListMath.suggestMid(5, 10));
        assertEquals(1, QuickListMath.suggestMid(1, 1));
        assertEquals(1, QuickListMath.suggestMid(0, 0));
    }
}
