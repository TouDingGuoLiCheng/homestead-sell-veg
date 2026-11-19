package com.homestead.sellveg.order;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShipmentSummaryTest {
    @Test
    void etaMinutesCeilAndZero() {
        assertEquals(0, ShipmentSummary.etaMinutes(0));
        assertEquals(1, ShipmentSummary.etaMinutes(1));
        assertEquals(1, ShipmentSummary.etaMinutes(1200));
        assertEquals(2, ShipmentSummary.etaMinutes(1201));
    }
}
