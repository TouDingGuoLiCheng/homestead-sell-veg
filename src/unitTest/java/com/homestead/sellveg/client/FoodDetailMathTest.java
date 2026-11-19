package com.homestead.sellveg.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FoodDetailMathTest {
    @Test
    void saturationMatchesPricingFormula() {
        assertEquals(7.2D, FoodDetailMath.saturationRestored(8, 0.9F), 1e-6);
        assertEquals(1.2D, FoodDetailMath.saturationRestored(4, 0.3F), 1e-6);
    }

    @Test
    void saturationAmountKeepsOneDecimal() {
        assertEquals("7.2", FoodDetailMath.formatSaturationAmount(7.2D));
        assertEquals("7.2", FoodDetailMath.formatSaturationAmount(7.24D));
        assertEquals("7.0", FoodDetailMath.formatSaturationAmount(7.0D));
    }

    @Test
    void qtyPrefixesUseMultiplicationSign() {
        assertEquals("×5", FoodDetailMath.formatHungerQty(5));
        assertEquals("×7.2", FoodDetailMath.formatSaturationQty(7.2D));
    }

    @Test
    void chanceSuffixOnlyWhenBelowFull() {
        assertEquals("", FoodDetailMath.formatChanceSuffix(1.0F));
        assertEquals("", FoodDetailMath.formatChanceSuffix(0.999F));
        assertEquals(" 50%", FoodDetailMath.formatChanceSuffix(0.5F));
        assertEquals(" 1%", FoodDetailMath.formatChanceSuffix(0.01F));
    }

    @Test
    void durationTicksFormatsLikePotionClock() {
        assertEquals("0:05", FoodDetailMath.formatDurationTicks(100));
        assertEquals("1:00", FoodDetailMath.formatDurationTicks(1200));
        assertEquals("0:00", FoodDetailMath.formatDurationTicks(0));
    }

    @Test
    void zeroQtyFormats() {
        assertEquals("×0", FoodDetailMath.formatHungerQty(0));
        assertEquals("×0.0", FoodDetailMath.formatSaturationQty(0.0D));
    }
}
