package com.homestead.sellveg.market;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FluidShareMathTest {
    @Test
    void bottleUsesQuarterOfBucketFerment() {
        assertEquals(10.0D, FluidShareMath.share(40.0D, 1000, 250), 0.001D);
    }

    @Test
    void missingAmountsAreZero() {
        assertEquals(0.0D, FluidShareMath.share(40.0D, 0, 250), 0.001D);
        assertEquals(0.0D, FluidShareMath.share(40.0D, 1000, 0), 0.001D);
        assertEquals(0.0D, FluidShareMath.share(0.0D, 1000, 250), 0.001D);
    }

    @Test
    void cheapestIgnoresMissingAndPicksMin() {
        assertEquals(3, FluidShareMath.cheapest(new int[]{9, 3, 40}));
        assertEquals(-1, FluidShareMath.cheapest(new int[]{}));
    }

    @Test
    void fillingWithoutFluidPriceIsInvalid() {
        assertEquals(true, FluidShareMath.incompleteFluid(250, 0.0D));
        assertEquals(false, FluidShareMath.incompleteFluid(250, 10.0D));
        assertEquals(false, FluidShareMath.incompleteFluid(0, 0.0D));
    }
}
