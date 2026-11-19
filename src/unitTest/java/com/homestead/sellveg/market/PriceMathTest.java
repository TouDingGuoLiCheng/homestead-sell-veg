package com.homestead.sellveg.market;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PriceMathTest {
    private static final double ANCHOR = 128.0D;
    private static final double WEIGHT_MIN = 0.2D;

    @Test
    void volumeWeightScalesWithOriginalMidpoint() {
        assertEquals(0.2D, PriceMath.volumeWeight(2.0D, ANCHOR, WEIGHT_MIN), 1e-9);
        assertEquals(0.2D, PriceMath.volumeWeight(7.5D, ANCHOR, WEIGHT_MIN), 1e-9);
        assertEquals(0.5D, PriceMath.volumeWeight(64.0D, ANCHOR, WEIGHT_MIN), 1e-9);
        assertEquals(1.0D, PriceMath.volumeWeight(128.0D, ANCHOR, WEIGHT_MIN), 1e-9);
        assertEquals(1.0D, PriceMath.volumeWeight(225.0D, ANCHOR, WEIGHT_MIN), 1e-9);
    }

    @Test
    void cheapBandNeedsAbout640ToHitMaxDailyShift() {
        // wheat mid 7.5 → weight floor 0.2; 256 → ratio 0.2; 640 → ratio 0.5
        int[] at256 = PriceMath.shiftBand(5, 10, 5, 10, 256, 0, 256, 0.5D, ANCHOR, WEIGHT_MIN);
        int[] at640 = PriceMath.shiftBand(5, 10, 5, 10, 640, 0, 256, 0.5D, ANCHOR, WEIGHT_MIN);
        assertArrayEquals(new int[]{3, 8}, at256);
        assertArrayEquals(new int[]{1, 6}, at640);
    }

    @Test
    void expensiveBandStillHitsMaxAt256() {
        // mid 225, weight 1.0 → volumeUnit 256 fills maxShift 0.5
        int[] partial = PriceMath.shiftBand(200, 250, 200, 250, 100, 0, 256, 0.5D, ANCHOR, WEIGHT_MIN);
        int[] at256 = PriceMath.shiftBand(200, 250, 200, 250, 256, 0, 256, 0.5D, ANCHOR, WEIGHT_MIN);
        assertArrayEquals(new int[]{87, 137}, at256);
        assertTrue(200 - partial[0] < 113);
    }

    @Test
    void sellingHitsCapShiftsWholeWheatBandDownByOriginalMid() {
        // identity weight (anchor=1,min=1): orig mid 7.5, cap 25% → delta -2; 5–10 → 3–8
        assertArrayEquals(new int[]{3, 8}, PriceMath.shiftBand(5, 10, 5, 10, 100, 0, 256, 0.25D, 1.0D, 1.0D));
    }

    @Test
    void buyingShiftsDiamondBandMoreThanWheat() {
        int[] wheat = PriceMath.shiftBand(5, 10, 5, 10, 0, 64, 256, 0.25D, 1.0D, 1.0D);
        int[] diamond = PriceMath.shiftBand(200, 250, 200, 250, 0, 64, 256, 0.25D, 1.0D, 1.0D);
        assertArrayEquals(new int[]{7, 12}, wheat);
        assertArrayEquals(new int[]{256, 306}, diamond);
    }

    @Test
    void idleRecoversTowardOriginalBand() {
        assertArrayEquals(new int[]{4, 9}, PriceMath.recoverBand(3, 8, 5, 10, 0.15D));
    }

    @Test
    void noVolumeDoesNotShift() {
        assertArrayEquals(new int[]{5, 10}, PriceMath.shiftBand(5, 10, 5, 10, 0, 0, 256, 0.25D, ANCHOR, WEIGHT_MIN));
    }

    @Test
    void dumpSlidesWheatReferenceDownWithTheBand() {
        // identity weight so 2000 still hits 25% cap
        int[] next = PriceMath.shiftBand(5, 10, 5, 10, 2000, 0, 256, 0.25D, 1.0D, 1.0D);
        assertArrayEquals(new int[]{3, 8}, next);
        assertEquals(8, PriceMath.slideReference(10, 5, 10, next[0], next[1]));
    }

    @Test
    void diamondDumpKeepsLiveBandBelowOriginalInsteadOfSnappingBack() {
        // 50% of mid 225 → -113; then mid clamped to origMid×0.5
        int[] shifted = PriceMath.shiftBand(200, 250, 200, 250, 100000, 0, 256, 0.5D, ANCHOR, WEIGHT_MIN);
        assertArrayEquals(new int[]{87, 137}, shifted);
        int[] clamped = PriceMath.clampBandToOrigRatios(shifted[0], shifted[1], 200, 250, 0.5D, 2.0D);
        assertArrayEquals(new int[]{88, 138}, clamped);
        assertArrayEquals(new int[]{88, 138}, PriceMath.resolveLiveBand(clamped[0], clamped[1]));
        assertEquals(138, PriceMath.slideReference(250, 200, 250, clamped[0], clamped[1]));
    }

    @Test
    void saleBuyIsEightyPercent() {
        assertEquals(8, PriceMath.applySaleBuy(10, 0.8D));
    }

    @Test
    void hotDelayIsSeventyPercent() {
        assertEquals(4200, PriceMath.applyHotDelay(6000, 0.7D, 20));
        assertEquals(0, PriceMath.applyHotDelay(0, 0.7D, 20));
    }

    @Test
    void clampRangeStaysInsideBand() {
        assertEquals(30, PriceMath.clampRange(10, 30, 35));
        assertEquals(35, PriceMath.clampRange(99, 30, 35));
        assertEquals(32, PriceMath.clampRange(32, 30, 35));
    }

    @Test
    void unpricedCraftMaterialCountsAsTen() {
        assertEquals(10, PriceMath.craftMaterialUnit(0));
        assertEquals(10, PriceMath.craftMaterialUnit(-3));
        assertEquals(32, PriceMath.craftMaterialUnit(32));
    }

    @Test
    void recipeSlotUsesCheapestSubstituteNotGoldEgg() {
        assertEquals(10, PriceMath.cheapestSlotCost(115, 10, 80));
    }

    @Test
    void recipeSlotStillSeesCheapOptionPastFirstTwentyFour() {
        int[] costs = new int[30];
        for (int i = 0; i < 29; i++) {
            costs[i] = 80 + i;
        }
        costs[29] = 10;
        assertEquals(10, PriceMath.cheapestSlotCost(costs));
    }

    @Test
    void emptyRecipeSlotFallsBackToUnpricedUnit() {
        assertEquals(10, PriceMath.cheapestSlotCost());
    }

    @Test
    void knownCatalogPriceBeatsUnpricedTagFillers() {
        // forge:ingots/iron may list unpriced mod metals (unit 10) beside iron_ingot 32
        assertEquals(32, PriceMath.cheapestSlotPreferKnown(new int[]{32}, new int[]{10, 10, 10}));
    }

    @Test
    void amongKnownSubstitutesStillPicksCheapest() {
        assertEquals(10, PriceMath.cheapestSlotPreferKnown(new int[]{115, 10, 80}, new int[]{}));
    }

    @Test
    void allUnpricedSlotStillUsesUnpricedUnit() {
        assertEquals(10, PriceMath.cheapestSlotPreferKnown(new int[]{}, new int[]{10, 10}));
        assertEquals(10, PriceMath.cheapestSlotPreferKnown(new int[]{}, new int[]{}));
    }

    @Test
    void nestedIngredientUsesCraftWhenCatalogMissing() {
        assertEquals(80, PriceMath.nestedMaterialOrCraft(0, 80));
        assertEquals(32, PriceMath.nestedMaterialOrCraft(32, 999));
        assertEquals(0, PriceMath.nestedMaterialOrCraft(0, 0));
    }

    @Test
    void craftNestingAndTagExpandAreCapped() {
        assertEquals(12, PriceMath.MAX_CRAFT_NESTING);
        assertEquals(12, PriceMath.TAG_CRAFT_EXPAND_LIMIT);
    }

    @Test
    void meltingAndSalvageAreDisassembly() {
        assertTrue(PriceMath.isDisassemblyRecipeType("tconstruct:melting"));
        assertTrue(PriceMath.isDisassemblyRecipeType("farmersdelight:cutting"));
        assertTrue(PriceMath.isDisassemblyRecipeType("create:crushing"));
        assertFalse(PriceMath.isDisassemblyRecipeType("minecraft:crafting_shaped"));
        assertFalse(PriceMath.isDisassemblyRecipeType("minecraft:smelting"));
        assertFalse(PriceMath.isDisassemblyRecipeType("farmersdelight:cooking"));
    }

    @Test
    void nonFoodMustSkipDisassemblyButFoodMayUseCutting() {
        assertTrue(PriceMath.skipRecipeForPricing("tconstruct:melting", false, false));
        assertTrue(PriceMath.skipRecipeForPricing("farmersdelight:cutting", false, false));
        assertFalse(PriceMath.skipRecipeForPricing("farmersdelight:cutting", true, false));
        assertFalse(PriceMath.skipRecipeForPricing("minecraft:crafting_shaped", false, false));
    }

    @Test
    void reverseRecipesAreSkippedEvenWhenShaped() {
        assertTrue(PriceMath.skipRecipeForPricing("minecraft:crafting_shaped", true, true));
        assertTrue(PriceMath.skipRecipeForPricing("minecraft:crafting_shaped", false, true));
    }

    @Test
    void extraOutputsAreIndexedOnlyWhenPrimaryResultIsEmpty() {
        assertTrue(PriceMath.indexExtraOutput(true, false));
        assertFalse(PriceMath.indexExtraOutput(false, false));
        assertFalse(PriceMath.indexExtraOutput(false, true));
    }

    @Test
    void vanillaResultCountWinsOverInflatedExtraScans() {
        assertEquals(1, PriceMath.resolvingOutputCount(1, 7));
        assertEquals(3, PriceMath.resolvingOutputCount(0, 3));
        assertEquals(1, PriceMath.resolvingOutputCount(0, 0));
    }

    @Test
    void luxurySkipIsForEdibleOutputsOnly() {
        assertTrue(PriceMath.allowLuxurySlotSkip(true));
        assertFalse(PriceMath.allowLuxurySlotSkip(false));
    }
}
