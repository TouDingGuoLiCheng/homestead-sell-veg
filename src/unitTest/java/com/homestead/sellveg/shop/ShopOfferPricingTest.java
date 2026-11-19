package com.homestead.sellveg.shop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopOfferPricingTest {
    @Test
    void pickStaysInsideBand() {
        int pick = ShopOfferPricing.bandPick(8, 12, 1L, "pulpy_orange", "minecraft:wheat");
        assertTrue(pick >= 8 && pick <= 12);
    }

    @Test
    void sameKeyIsStable() {
        int a = ShopOfferPricing.bandPick(8, 12, 3L, "omni_mart", "minecraft:carrot");
        int b = ShopOfferPricing.bandPick(8, 12, 3L, "omni_mart", "minecraft:carrot");
        assertEquals(a, b);
    }

    @Test
    void differentShopsCanDiffer() {
        int a = ShopOfferPricing.bandPick(1, 100, 1L, "pulpy_orange", "minecraft:wheat");
        int b = ShopOfferPricing.bandPick(1, 100, 1L, "golden_wheat", "minecraft:wheat");
        assertNotEquals(a, b);
    }

    @Test
    void buyAppliesMarkupAndPercentOff() {
        assertEquals(12, ShopOfferPricing.buyFromPick(10, 1.15D, 0));
        assertEquals(10, ShopOfferPricing.buyFromPick(10, 1.15D, 20));
        assertEquals(1, ShopOfferPricing.buyFromPick(10, 1.15D, 99));
    }
}
