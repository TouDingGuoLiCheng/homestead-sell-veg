package com.homestead.sellveg.order;

/**
 * Pure helpers for logistics quantity labels (no Minecraft types).
 */
public final class ShipmentCountMath {
    private ShipmentCountMath() {
    }

    /**
     * Sum counts for the first non-empty slot's kind across all matching slots.
     * Used so a parcel with two stacks of 64 shows {@code 128}, not {@code 64}.
     */
    public static int primaryKindTotal(int[] counts, int[] kinds) {
        if (counts == null || kinds == null || counts.length == 0 || counts.length != kinds.length) {
            return 0;
        }
        int primaryKind = 0;
        boolean found = false;
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] > 0) {
                primaryKind = kinds[i];
                found = true;
                break;
            }
        }
        if (!found) {
            return 0;
        }
        int total = 0;
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] > 0 && kinds[i] == primaryKind) {
                total += counts[i];
            }
        }
        return total;
    }
}
