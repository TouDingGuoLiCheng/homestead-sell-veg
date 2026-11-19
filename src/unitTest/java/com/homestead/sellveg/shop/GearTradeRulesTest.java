package com.homestead.sellveg.shop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GearTradeRulesTest {
    @Test
    void durabilityPercentRoundsRemaining() {
        assertEquals(100, GearTradeRules.durabilityPercent(0, 250));
        assertEquals(0, GearTradeRules.durabilityPercent(250, 250));
        assertEquals(98, GearTradeRules.durabilityPercent(5, 250));
        assertEquals(-1, GearTradeRules.durabilityPercent(0, 0));
        assertEquals("98%", GearTradeRules.durabilityPercentLabel(5, 250));
        assertNull(GearTradeRules.durabilityPercentLabel(0, 0));
    }

    @Test
    void clampTradeCountGearAlwaysOne() {
        assertEquals(1, GearTradeRules.clampTradeCount(true, 64, 64));
        assertEquals(1, GearTradeRules.clampTradeCount(true, 1, 64));
        assertEquals(0, GearTradeRules.clampTradeCount(true, 0, 64));
        assertEquals(64, GearTradeRules.clampTradeCount(false, 200, 64));
        assertEquals(12, GearTradeRules.clampTradeCount(false, 12, 64));
    }

    @Test
    void tradeQtyCapGearAtMostOne() {
        assertEquals(0, GearTradeRules.tradeQtyCap(true, 0));
        assertEquals(1, GearTradeRules.tradeQtyCap(true, 32));
        assertEquals(32, GearTradeRules.tradeQtyCap(false, 32));
    }
}
