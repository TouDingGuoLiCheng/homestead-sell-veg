package com.homestead.sellveg.market;

/**
 * Any recipe slot with substitutes uses the cheapest item.
 * If a separate slot is only a cheap item plus metal/gem extras (gold egg, iron carrot, …), skip it.
 */
public final class LuxurySubstituteMath {
    private LuxurySubstituteMath() {
    }

    public static boolean skipLuxurySlot(boolean otherSlotHasBase, boolean craftedFromBasePlusPreciousOnly) {
        return otherSlotHasBase && craftedFromBasePlusPreciousOnly;
    }

    /** Ingots, nuggets, ores, gems — not hay blocks or the upgraded food itself. */
    public static boolean isPreciousMaterialPath(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        if (path.endsWith("_ingot") || path.endsWith("_nugget") || path.contains("_ore")) {
            return true;
        }
        if (path.startsWith("raw_")) {
            return true;
        }
        return switch (path) {
            case "diamond", "emerald", "coal", "redstone", "lapis_lazuli",
                    "quartz", "amethyst_shard", "netherite_scrap" -> true;
            case "gold_block", "iron_block", "copper_block", "diamond_block",
                    "emerald_block", "netherite_block", "coal_block", "lapis_block",
                    "redstone_block", "raw_iron_block", "raw_gold_block", "raw_copper_block" -> true;
            default -> false;
        };
    }
}
