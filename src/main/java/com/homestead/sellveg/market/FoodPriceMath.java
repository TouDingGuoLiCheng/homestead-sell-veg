package com.homestead.sellveg.market;

/**
 * Food list price: ceil(materials * 1.x + hunger + saturation + 10*buffs - 10*debuffs), min 1.
 * x = ingredient count clamped to 1–5, so the markup is 1.1–1.5.
 * Crafted food is never cheaper than the ingredient sum (price ≥ floor(materials)+1).
 */
public final class FoodPriceMath {
    private FoodPriceMath() {
    }

    public static int compute(
            double materialSum,
            int ingredientCount,
            double hunger,
            double saturation,
            int beneficialEffects,
            int harmfulEffects
    ) {
        double materials = 0.0D;
        if (materialSum > 0.0D && ingredientCount > 0) {
            int x = Math.max(1, Math.min(5, ingredientCount));
            materials = materialSum * (1.0D + 0.1D * x);
        }
        double total = materials + hunger + saturation + 10.0D * beneficialEffects - 10.0D * harmfulEffects;
        int priced = Math.max(1, (int) Math.ceil(total));
        if (materialSum > 0.0D) {
            priced = Math.max(priced, (int) Math.floor(materialSum) + 1);
        }
        return priced;
    }
}
