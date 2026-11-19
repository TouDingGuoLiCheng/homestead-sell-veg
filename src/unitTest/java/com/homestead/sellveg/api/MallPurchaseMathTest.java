package com.homestead.sellveg.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MallPurchaseMathTest {
    @Test
    void affordableCountRespectsBalanceAndWanted() {
        assertEquals(0, MallPurchaseMath.affordableCount(0, 10, 5));
        assertEquals(0, MallPurchaseMath.affordableCount(9, 10, 5));
        assertEquals(1, MallPurchaseMath.affordableCount(10, 10, 5));
        assertEquals(3, MallPurchaseMath.affordableCount(35, 10, 5));
        assertEquals(5, MallPurchaseMath.affordableCount(1000, 10, 5));
        assertEquals(0, MallPurchaseMath.affordableCount(100, 0, 5));
        assertEquals(0, MallPurchaseMath.affordableCount(100, 10, 0));
    }

    @Test
    void requestedUnitsKeepsBulkShortageInsteadOfListingCap() {
        assertEquals(0, MallPurchaseMath.requestedUnits(false, 0));
        assertEquals(0, MallPurchaseMath.requestedUnits(true, 0));
        assertEquals(1, MallPurchaseMath.requestedUnits(true, 619));
        assertEquals(12, MallPurchaseMath.requestedUnits(false, 12));
        assertEquals(619, MallPurchaseMath.requestedUnits(false, 619));
        assertEquals(MallPurchaseMath.MAX_BULK_UNITS, MallPurchaseMath.requestedUnits(false, Integer.MAX_VALUE));
    }
}
