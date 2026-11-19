package com.homestead.sellveg.market;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FoodPriceMathTest {
    @Test
    void rawFoodIsHungerPlusSaturation() {
        assertEquals(5, FoodPriceMath.compute(0, 0, 3, 1.8, 0, 0));
    }

    @Test
    void oneIngredientUsesMarkup11() {
        // 10 * 1.1 + 8 + 12.8 = 31.8 → 32
        assertEquals(32, FoodPriceMath.compute(10, 1, 8, 12.8, 0, 0));
    }

    @Test
    void fiveIngredientsUseMarkup15() {
        assertEquals(15, FoodPriceMath.compute(10, 5, 0, 0, 0, 0));
    }

    @Test
    void eachBuffAddsTen() {
        assertEquals(15, FoodPriceMath.compute(0, 0, 5, 0, 1, 0));
    }

    @Test
    void debuffSubtractsTenButNotBelowOne() {
        assertEquals(1, FoodPriceMath.compute(0, 0, 4, 0, 0, 1));
    }

    @Test
    void debuffCannotUndercutMaterials() {
        assertEquals(11, FoodPriceMath.compute(10, 1, 0, 0, 0, 5));
    }

    @Test
    void roundsUp() {
        assertEquals(2, FoodPriceMath.compute(0, 0, 1.1, 0, 0, 0));
    }

    @Test
    void threeWheatBreadBeatsWheatAndShopTwelve() {
        int wheat = 10;
        int bread = FoodPriceMath.compute(wheat * 3.0D, 3, 5, 3.0D, 0, 0);
        assertTrue(bread > wheat);
        assertTrue(bread > 12);
    }
}
