package com.homestead.sellveg.order;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourierTest {
    @Test
    void pickCyclesThroughThreeCouriers() {
        assertEquals(Courier.ORANGE_PULP, Courier.pick(0));
        assertEquals(Courier.DOORSTEP, Courier.pick(1));
        assertEquals(Courier.CART, Courier.pick(2));
        assertEquals(Courier.ORANGE_PULP, Courier.pick(3));
    }

    @Test
    void unknownIdFallsBackToDoorstep() {
        assertEquals(Courier.DOORSTEP, Courier.fromId(null));
        assertEquals(Courier.DOORSTEP, Courier.fromId(""));
        assertEquals(Courier.DOORSTEP, Courier.fromId("unknown"));
        assertEquals(Courier.ORANGE_PULP, Courier.fromId("orange_pulp"));
    }

    @Test
    void delayScalesFastMidSlow() {
        int base = 10_000;
        assertEquals(5500, Courier.ORANGE_PULP.scaleDelay(base));
        assertEquals(10_000, Courier.DOORSTEP.scaleDelay(base));
        assertEquals(14_500, Courier.CART.scaleDelay(base));
        assertEquals(1, Courier.ORANGE_PULP.scaleDelay(1));
    }

    @Test
    void langKeysMatchIds() {
        assertTrue(Courier.ORANGE_PULP.langKey().endsWith("orange_pulp"));
        assertTrue(Courier.DOORSTEP.langKey().endsWith("doorstep"));
        assertTrue(Courier.CART.langKey().endsWith("cart"));
    }
}
