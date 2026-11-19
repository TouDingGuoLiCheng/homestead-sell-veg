package com.homestead.sellveg.block;

/**
 * Pure rules for parcel fall cargo salvage (no Minecraft types).
 */
public final class ParcelFallMergeMath {
    private ParcelFallMergeMath() {
    }

    /**
     * After setBlock, discard runs before BE merge. Skip scatter only for that empty
     * waiting box — not when overlapping another filled (or foreign) parcel.
     */
    public static boolean shouldDeferScatterForMerge(boolean parcelAtPos, boolean placedBeEmpty, boolean hasFallCargo) {
        return parcelAtPos && placedBeEmpty && hasFallCargo;
    }
}
