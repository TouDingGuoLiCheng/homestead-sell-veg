package com.homestead.sellveg.order;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShipmentCapacityTest {
    @Test
    void canAddWithinCap() {
        assertTrue(ShipmentCapacity.canAdd(0, 1, 32));
        assertTrue(ShipmentCapacity.canAdd(31, 1, 32));
        assertTrue(ShipmentCapacity.canAdd(30, 2, 32));
    }

    @Test
    void canAddRejectsOverCap() {
        assertFalse(ShipmentCapacity.canAdd(32, 1, 32));
        assertFalse(ShipmentCapacity.canAdd(30, 3, 32));
    }

    @Test
    void zeroAdditionalAlwaysAllowed() {
        assertTrue(ShipmentCapacity.canAdd(999, 0, 32));
    }
}
