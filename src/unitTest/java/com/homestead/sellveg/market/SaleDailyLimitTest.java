package com.homestead.sellveg.market;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SaleDailyLimitTest {
    @Test
    void remainingSubtractsCartAndBought() {
        assertEquals(44, SaleDailyLimit.remaining(10, 10, 64));
    }

    @Test
    void canAddRejectsWhenOverCap() {
        assertFalse(SaleDailyLimit.canAdd(60, 0, 5, 64));
        assertTrue(SaleDailyLimit.canAdd(60, 0, 4, 64));
    }

    @Test
    void zeroAdditionalAlwaysAllowed() {
        assertTrue(SaleDailyLimit.canAdd(999, 999, 0));
    }
}
