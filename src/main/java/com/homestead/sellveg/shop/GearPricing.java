package com.homestead.sellveg.shop;

import java.util.List;

/**
 * Pure pricing for weapon/armor: blank base × per-enchant multipliers × durability.
 */
public final class GearPricing {
    public enum Tier {
        COMMON(1.05D),
        ADVANCED(1.1D),
        TREASURE(1.5D);

        public final double perLevel;

        Tier(double perLevel) {
            this.perLevel = perLevel;
        }
    }

    private static final double CURSE_PER_LEVEL = 0.8D;

    /** One enchantment factor: perLevel^level (curses always use 0.8). */
    public record EnchantLine(Tier tier, int level, boolean curse) {
        public double factor() {
            int lv = Math.max(1, level);
            double per = curse ? CURSE_PER_LEVEL : (tier == null ? Tier.COMMON.perLevel : tier.perLevel);
            return Math.pow(per, lv);
        }
    }

    /** Listing UI suggest band + auto-fill unit price. */
    public record ListingSuggest(int lo, int hi, int fill) {
    }

    private GearPricing() {
    }

    public static int finalPrice(int blankBase, List<EnchantLine> lines, double durabilityRatio) {
        int base = Math.max(0, blankBase);
        double mult = enchantMultiplier(lines);
        double d = Double.isFinite(durabilityRatio) ? Math.max(0.0D, Math.min(1.0D, durabilityRatio)) : 1.0D;
        int priced = (int) Math.round(base * mult * d);
        return Math.max(1, priced);
    }

    public static double enchantMultiplier(List<EnchantLine> lines) {
        if (lines == null || lines.isEmpty()) {
            return 1.0D;
        }
        double product = 1.0D;
        for (EnchantLine line : lines) {
            if (line == null) {
                continue;
            }
            product *= line.factor();
        }
        return product;
    }

    /**
     * Suggest band + fill for the list panel. Non-gear keeps blank live band / ref;
     * gear applies B×M×D to lo, hi, and ref independently.
     */
    public static ListingSuggest listingSuggest(
            int blankLo,
            int blankHi,
            int blankRef,
            List<EnchantLine> lines,
            double durabilityRatio,
            boolean gear) {
        int lo = Math.max(1, blankLo);
        int hi = Math.max(lo, blankHi);
        int ref = Math.max(1, blankRef);
        if (!gear) {
            return new ListingSuggest(lo, hi, ref);
        }
        List<EnchantLine> safe = lines == null ? List.of() : lines;
        int adjLo = finalPrice(lo, safe, durabilityRatio);
        int adjHi = Math.max(adjLo, finalPrice(hi, safe, durabilityRatio));
        int fill = finalPrice(ref, safe, durabilityRatio);
        return new ListingSuggest(adjLo, adjHi, fill);
    }
}
