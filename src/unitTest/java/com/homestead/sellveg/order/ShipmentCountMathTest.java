package com.homestead.sellveg.order;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShipmentCountMathTest {
    @Test
    void primaryKindTotalSumsAllStacksOfFirstItem() {
        // two stacks of kind 7 (64+64) plus another kind → primary shows 128
        int[] counts = {64, 64, 10};
        int[] kinds = {7, 7, 9};
        assertEquals(128, ShipmentCountMath.primaryKindTotal(counts, kinds));
    }

    @Test
    void primaryKindTotalIgnoresEmptyLeadingSlots() {
        int[] counts = {0, 32, 16};
        int[] kinds = {0, 3, 3};
        assertEquals(48, ShipmentCountMath.primaryKindTotal(counts, kinds));
    }

    @Test
    void primaryKindTotalEmptyIsZero() {
        assertEquals(0, ShipmentCountMath.primaryKindTotal(new int[]{0, 0}, new int[]{1, 1}));
        assertEquals(0, ShipmentCountMath.primaryKindTotal(null, null));
    }
}
