package com.homestead.sellveg.market;

/**
 * Split machine fluid cost across bottles, and pick the cheapest tag option.
 */
public final class FluidShareMath {
    private FluidShareMath() {
    }

    public static double share(double totalCost, int producedMb, int usedMb) {
        if (totalCost <= 0.0D || producedMb <= 0 || usedMb <= 0) {
            return 0.0D;
        }
        return totalCost * usedMb / (double) producedMb;
    }

    public static int cheapest(int[] optionCosts) {
        int best = Integer.MAX_VALUE;
        for (int cost : optionCosts) {
            if (cost >= 0) {
                best = Math.min(best, cost);
            }
        }
        return best == Integer.MAX_VALUE ? -1 : best;
    }

    public static boolean incompleteFluid(int usedMb, double fluidCost) {
        return usedMb > 0 && fluidCost <= 0.0D;
    }
}
