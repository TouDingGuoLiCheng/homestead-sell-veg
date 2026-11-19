package com.homestead.sellveg.shop;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GearPricingTest {
    @Test
    void plainFullDurabilityIsBase() {
        assertEquals(100, GearPricing.finalPrice(100, List.of(), 1.0D));
    }

    @Test
    void durabilityScalesTotal() {
        assertEquals(50, GearPricing.finalPrice(100, List.of(), 0.5D));
    }

    @Test
    void commonEnchantMultipliesPerLevel() {
        // 100 * 1.05^2 = 110.25 → 110
        int price = GearPricing.finalPrice(100, List.of(
                new GearPricing.EnchantLine(GearPricing.Tier.COMMON, 2, false)
        ), 1.0D);
        assertEquals(110, price);
    }

    @Test
    void multipleEnchantsMultiplyTogether() {
        // 10 * 1.05 * 1.5 = 15.75 → 16
        int price = GearPricing.finalPrice(10, List.of(
                new GearPricing.EnchantLine(GearPricing.Tier.COMMON, 1, false),
                new GearPricing.EnchantLine(GearPricing.Tier.TREASURE, 1, false)
        ), 1.0D);
        assertEquals(16, price);
    }

    @Test
    void advancedAndTreasureUseTheirFactors() {
        // 100 * 1.1 * 1.5 = 165
        int price = GearPricing.finalPrice(100, List.of(
                new GearPricing.EnchantLine(GearPricing.Tier.ADVANCED, 1, false),
                new GearPricing.EnchantLine(GearPricing.Tier.TREASURE, 1, false)
        ), 1.0D);
        assertEquals(165, price);
    }

    @Test
    void curseMultipliesDown() {
        // 100 * 1.05 * 0.8 = 84
        int price = GearPricing.finalPrice(100, List.of(
                new GearPricing.EnchantLine(GearPricing.Tier.COMMON, 1, false),
                new GearPricing.EnchantLine(GearPricing.Tier.COMMON, 1, true)
        ), 1.0D);
        assertEquals(84, price);
    }

    @Test
    void curseLevelStacksMultiplicatively() {
        // 100 * 0.8^2 = 64
        int price = GearPricing.finalPrice(100, List.of(
                new GearPricing.EnchantLine(GearPricing.Tier.COMMON, 2, true)
        ), 1.0D);
        assertEquals(64, price);
    }

    @Test
    void floorAtOne() {
        assertEquals(1, GearPricing.finalPrice(1, List.of(), 0.01D));
    }

    @Test
    void allPositiveEnchantsCount() {
        // 100 * 1.05^5 * 1.5 ≈ 191.41 → 191
        List<GearPricing.EnchantLine> six = List.of(
                new GearPricing.EnchantLine(GearPricing.Tier.COMMON, 1, false),
                new GearPricing.EnchantLine(GearPricing.Tier.COMMON, 1, false),
                new GearPricing.EnchantLine(GearPricing.Tier.COMMON, 1, false),
                new GearPricing.EnchantLine(GearPricing.Tier.COMMON, 1, false),
                new GearPricing.EnchantLine(GearPricing.Tier.COMMON, 1, false),
                new GearPricing.EnchantLine(GearPricing.Tier.TREASURE, 1, false)
        );
        assertEquals(191, GearPricing.finalPrice(100, six, 1.0D));
    }

    @Test
    void listingSuggestNonGearUsesBlankBandAndRef() {
        GearPricing.ListingSuggest s = GearPricing.listingSuggest(
                42, 66, 54, List.of(), 0.5D, false);
        assertEquals(42, s.lo());
        assertEquals(66, s.hi());
        assertEquals(54, s.fill());
    }

    @Test
    void listingSuggestGearScalesBandByDurabilityAndEnchant() {
        List<GearPricing.EnchantLine> lines = List.of(
                new GearPricing.EnchantLine(GearPricing.Tier.COMMON, 1, false)
        );
        // lo=42*1.05*0.5→22, hi=66*1.05*0.5→35, fill=54*1.05*0.5→28
        GearPricing.ListingSuggest s = GearPricing.listingSuggest(
                42, 66, 54, lines, 0.5D, true);
        assertEquals(22, s.lo());
        assertEquals(35, s.hi());
        assertEquals(28, s.fill());
    }
}
