package com.homestead.sellveg.market;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LuxurySubstituteMathTest {
    @Test
    void goldEggSlotDroppedWhenPlainEggAlreadyPresent() {
        assertTrue(LuxurySubstituteMath.skipLuxurySlot(true, true));
    }

    @Test
    void twoPlainEggSlotsAreBothKept() {
        assertFalse(LuxurySubstituteMath.skipLuxurySlot(true, false));
    }

    @Test
    void goldEggAloneIsPricedIfNoCheapBaseSlot() {
        assertFalse(LuxurySubstituteMath.skipLuxurySlot(false, true));
    }

    @Test
    void goldIngotIsPreciousButGoldenCarrotIsNot() {
        assertTrue(LuxurySubstituteMath.isPreciousMaterialPath("gold_ingot"));
        assertFalse(LuxurySubstituteMath.isPreciousMaterialPath("golden_carrot"));
        assertFalse(LuxurySubstituteMath.isPreciousMaterialPath("egg"));
    }

    @Test
    void ironCopperAndGemsAreUpgradeExtrasToo() {
        assertTrue(LuxurySubstituteMath.isPreciousMaterialPath("iron_ingot"));
        assertTrue(LuxurySubstituteMath.isPreciousMaterialPath("copper_ingot"));
        assertTrue(LuxurySubstituteMath.isPreciousMaterialPath("iron_nugget"));
        assertTrue(LuxurySubstituteMath.isPreciousMaterialPath("raw_copper"));
        assertTrue(LuxurySubstituteMath.isPreciousMaterialPath("coal"));
        assertTrue(LuxurySubstituteMath.isPreciousMaterialPath("redstone"));
        assertTrue(LuxurySubstituteMath.isPreciousMaterialPath("amethyst_shard"));
        assertFalse(LuxurySubstituteMath.isPreciousMaterialPath("hay_block"));
        assertFalse(LuxurySubstituteMath.isPreciousMaterialPath("bread"));
    }
}
