package com.homestead.sellveg.market;

import java.util.Locale;

public final class PriceMath {
    private PriceMath() {
    }

    /**
     * Slide both band edges by original-midpoint × volume ratio (capped).
     * Cheap original bands move fewer coins; expensive bands move more.
     * Volume is scaled by {@link #volumeWeight} so common cheap goods need more trades to hit the cap.
     */
    public static int[] shiftBand(int lo, int hi, int origLo, int origHi,
            int sold, int bought, int volumeUnit, double maxShiftRatio,
            double priceAnchor, double minWeight) {
        int delta = bandDelta(origLo, origHi, sold, bought, volumeUnit, maxShiftRatio, priceAnchor, minWeight);
        int nextLo = Math.max(1, lo + delta);
        int nextHi = Math.max(nextLo, hi + delta);
        return new int[]{nextLo, nextHi};
    }

    /**
     * Weight for daily volume: higher original midpoint → closer to 1; floored so cheap goods
     * still reach the max shift around {@code volumeUnit / minWeight} trades (default 640).
     */
    public static double volumeWeight(double origMid, double priceAnchor, double minWeight) {
        double floor = Math.max(0.0D, Math.min(1.0D, minWeight));
        if (priceAnchor <= 0.0D) {
            return 1.0D;
        }
        if (origMid <= 0.0D) {
            return floor;
        }
        return Math.max(floor, Math.min(1.0D, origMid / priceAnchor));
    }

    public static int bandDelta(int origLo, int origHi, int sold, int bought,
            int volumeUnit, double maxShiftRatio, double priceAnchor, double minWeight) {
        if (sold == bought || volumeUnit <= 0) {
            return 0;
        }
        int volume = sold > bought ? sold : bought;
        double origMid = (origLo + origHi) / 2.0D;
        double weight = volumeWeight(origMid, priceAnchor, minWeight);
        double ratio = Math.min(maxShiftRatio, (volume * weight) / (double) volumeUnit);
        int mag = (int) Math.round(origMid * ratio);
        if (mag == 0 && origMid > 0 && volume > 0) {
            mag = 1;
        }
        return sold > bought ? -mag : mag;
    }

    /** Idle day: lerp current band toward the original (initial) band. */
    public static int[] recoverBand(int lo, int hi, int origLo, int origHi, double recoverLerp) {
        return new int[]{toward(lo, origLo, recoverLerp), toward(hi, origHi, recoverLerp)};
    }

    private static int toward(int current, int original, double t) {
        if (current == original) {
            return current;
        }
        double clampedT = Math.max(0.0D, Math.min(1.0D, t));
        int next = (int) Math.round(current + (original - current) * clampedT);
        if (next == current) {
            return current + Integer.signum(original - current);
        }
        return Math.max(1, next);
    }

    public static int adjustForVolume(int current, int base, int sold, int bought, double minRatio, double maxRatio) {
        double factor = 1.0D;
        if (sold > bought) {
            factor -= Math.min(0.25D, sold / 256.0D);
        } else if (bought > sold) {
            factor += Math.min(0.25D, bought / 256.0D);
        }
        int next = (int) Math.round(current * factor);
        return clampToBaseBand(next, base, minRatio, maxRatio);
    }

    /** Keep band width; clamp midpoint to original-mid × [minRatio, maxRatio]. */
    public static int[] clampBandToOrigRatios(int lo, int hi, int origLo, int origHi,
            double minRatio, double maxRatio) {
        double origMid = (origLo + origHi) / 2.0D;
        int width = Math.max(0, hi - lo);
        double mid = (lo + hi) / 2.0D;
        double minMid = Math.max(1.0D, origMid * minRatio);
        double maxMid = Math.max(minMid, origMid * maxRatio);
        double clamped = Math.max(minMid, Math.min(maxMid, mid));
        int nextLo = Math.max(1, (int) Math.round(clamped - width / 2.0D));
        return new int[]{nextLo, nextLo + width};
    }

    public static int clampRange(int price, int min, int max) {
        int lo = Math.max(1, Math.min(min, max));
        int hi = Math.max(lo, Math.max(min, max));
        return Math.max(lo, Math.min(hi, price));
    }

    /**
     * Keep a stored live band as-is. Do not snap back to the catalog band when a dump
     * slides entirely below (or a squeeze entirely above) the original range — that
     * used to cancel diamond-sized shifts and pin ref to the old floor.
     */
    public static int[] resolveLiveBand(int lo, int hi) {
        int low = Math.max(1, lo);
        int high = Math.max(low, hi);
        return new int[]{low, high};
    }

    /** Move a daily sample with the band midpoint so a dump cannot re-roll back up. */
    public static int slideReference(int ref, int oldLo, int oldHi, int newLo, int newHi) {
        double oldMid = (oldLo + oldHi) / 2.0D;
        double newMid = (newLo + newHi) / 2.0D;
        int next = (int) Math.round(ref + (newMid - oldMid));
        return clampRange(next, newLo, newHi);
    }

    public static int clampToBaseBand(int price, int base, double minRatio, double maxRatio) {
        int next = (int) Math.max(base * minRatio, Math.min(base * maxRatio, price));
        return Math.max(1, next);
    }

    public static final int UNPRICED_CRAFT_UNIT = 10;
    /** Cap recipe nesting so huge modpacks cannot hang world join. */
    public static final int MAX_CRAFT_NESTING = 12;
    /** When a tag has no catalog-priced option, only expand this many substitutes via craft. */
    public static final int TAG_CRAFT_EXPAND_LIMIT = 12;

    /** Catalog price for a recipe ingredient; unpriced materials count as 10 each. */
    public static int craftMaterialUnit(int catalogPrice) {
        return catalogPrice > 0 ? catalogPrice : UNPRICED_CRAFT_UNIT;
    }

    /**
     * Nested ingredient price: use catalog/base when known, otherwise the craft-material sum.
     * Keeps chest/furnace/tnt from collapsing to the flat unpriced unit when they have recipes.
     */
    public static int nestedMaterialOrCraft(int catalogBase, int craftCost) {
        if (catalogBase > 0) {
            return catalogBase;
        }
        return Math.max(0, craftCost);
    }

    /** One recipe slot: among substitutes, use the lowest cost (not gold-egg etc.). */
    public static int cheapestSlotCost(int... optionCosts) {
        int cheapest = Integer.MAX_VALUE;
        if (optionCosts != null) {
            for (int cost : optionCosts) {
                cheapest = Math.min(cheapest, cost);
            }
        }
        return cheapest == Integer.MAX_VALUE ? craftMaterialUnit(0) : cheapest;
    }

    /**
     * Prefer substitutes with a known catalog/craft price over unpriced fillers (unit 10).
     * Tag ingredients often list dozens of unpriced mod items that must not undercut iron_ingot.
     */
    public static int cheapestSlotPreferKnown(int[] knownCosts, int[] unknownCosts) {
        int bestKnown = Integer.MAX_VALUE;
        if (knownCosts != null) {
            for (int cost : knownCosts) {
                bestKnown = Math.min(bestKnown, cost);
            }
        }
        if (bestKnown != Integer.MAX_VALUE) {
            return bestKnown;
        }
        int bestUnknown = Integer.MAX_VALUE;
        if (unknownCosts != null) {
            for (int cost : unknownCosts) {
                bestUnknown = Math.min(bestUnknown, cost);
            }
        }
        return bestUnknown == Integer.MAX_VALUE ? craftMaterialUnit(0) : bestUnknown;
    }

    /**
     * Melting, cutting-apart, crushing, etc. consume the finished item (or an assembled
     * cousin) and must not undercut the crafting-table price.
     */
    public static boolean isDisassemblyRecipeType(String typeId) {
        if (typeId == null || typeId.isEmpty()) {
            return false;
        }
        String t = typeId.toLowerCase(Locale.ROOT);
        return hasTypeToken(t, "melting")
                || hasTypeToken(t, "smeltery")
                || hasTypeToken(t, "cutting")
                || t.contains("salvag")
                || hasTypeToken(t, "crushing")
                || hasTypeToken(t, "milling")
                || t.contains("un-craft")
                || hasTypeToken(t, "uncraft")
                || t.contains("recycl");
    }

    private static boolean hasTypeToken(String typeId, String token) {
        return typeId.equals(token)
                || typeId.contains(":" + token)
                || typeId.contains(token + "_")
                || typeId.contains("_" + token)
                || typeId.contains("/" + token);
    }

    /**
     * @param outputIsIngredient true when the priced item appears among this recipe's inputs
     *                           (burger-from-bread, book-from-bread, uncraft loops)
     */
    public static boolean skipRecipeForPricing(String typeId, boolean outputEdible, boolean outputIsIngredient) {
        if (outputIsIngredient) {
            return true;
        }
        return !outputEdible && isDisassemblyRecipeType(typeId);
    }

    /** Extra field scans are only for custom machines whose vanilla result is empty. */
    public static boolean indexExtraOutput(boolean primaryEmpty, boolean extraSameAsPrimary) {
        return primaryEmpty;
    }

    /** Vanilla result count wins so extra field scans cannot divide a 5-ingot craft down. */
    public static int resolvingOutputCount(int vanillaCountIfMatch, int extraMatchingCount) {
        if (vanillaCountIfMatch > 0) {
            return vanillaCountIfMatch;
        }
        return Math.max(1, extraMatchingCount);
    }

    /** Gold-egg style slot skipping is for food; iron+nugget chains must keep the ingot. */
    public static boolean allowLuxurySlotSkip(boolean outputEdible) {
        return outputEdible;
    }

    public static int applySaleBuy(int buyPrice, double saleMultiplier) {
        return Math.max(1, (int) Math.round(buyPrice * saleMultiplier));
    }

    /**
     * Catalog-listed cheap goods (seeds, terracotta, …) ignore craft when sizing live bands.
     * Assembled shop parts still pass through the raw craft cost.
     */
    public static int craftFloorForLiveBand(int craftCost, boolean catalogIgnoresCraft) {
        if (catalogIgnoresCraft || craftCost <= 0) {
            return 0;
        }
        return craftCost;
    }

    /** Widen catalog hi to craft+1 only when craft floor applies to this item. */
    public static int bandHiWithCraftFloor(int origHi, int craftCost, boolean catalogIgnoresCraft) {
        int floor = craftFloorForLiveBand(craftCost, catalogIgnoresCraft);
        return floor > 0 ? Math.max(origHi, floor + 1) : Math.max(1, origHi);
    }

    /**
     * Saved live bands from an older pricing ruleset can sit entirely below the new craft floor
     * (e.g. hopper stuck at 4–12 after redstone craft pricing). Those must reset to catalog.
     * Pass {@link #craftFloorForLiveBand} so catalog cheap items are not force-widened.
     */
    public static boolean needsCatalogBandRebase(int liveLo, int liveHi, int origLo, int origHi, int craftFloor) {
        if (liveHi < liveLo) {
            return true;
        }
        int need = craftFloor > 0 ? craftFloor + 1 : 0;
        if (need > 0 && liveHi < need) {
            return true;
        }
        return origLo > liveHi;
    }

    public static int applyHotDelay(int delayTicks, double hotMultiplier, int minDelay) {
        if (delayTicks <= 0) {
            return 0;
        }
        return Math.max(minDelay, (int) Math.round(delayTicks * hotMultiplier));
    }
}
