package com.homestead.sellveg.block;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParcelBoxFallMergeTest {
    @Test
    void emptyJustPlacedBoxDefersScatter() {
        assertTrue(ParcelFallMergeMath.shouldDeferScatterForMerge(true, true, true));
    }

    @Test
    void filledNeighborBoxDoesNotDefer() {
        assertFalse(ParcelFallMergeMath.shouldDeferScatterForMerge(true, false, true));
    }

    @Test
    void airOrTorchDoesNotDefer() {
        assertFalse(ParcelFallMergeMath.shouldDeferScatterForMerge(false, true, true));
        assertFalse(ParcelFallMergeMath.shouldDeferScatterForMerge(false, false, true));
    }

    @Test
    void noCargoNothingToDefer() {
        assertFalse(ParcelFallMergeMath.shouldDeferScatterForMerge(true, true, false));
    }
}
