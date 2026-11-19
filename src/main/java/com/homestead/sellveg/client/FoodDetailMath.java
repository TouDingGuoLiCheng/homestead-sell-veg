package com.homestead.sellveg.client;

/**
 * Pure formatting for food detail nutrition / chance / duration (no Minecraft types).
 */
public final class FoodDetailMath {
    private FoodDetailMath() {
    }

    public static double saturationRestored(int nutrition, float saturationModifier) {
        return nutrition * (double) saturationModifier;
    }

    /** One decimal place, e.g. {@code 7.2}. */
    public static String formatSaturationAmount(double saturation) {
        double rounded = Math.round(saturation * 10.0D) / 10.0D;
        return String.format(java.util.Locale.ROOT, "%.1f", rounded);
    }

    public static String formatHungerQty(int nutrition) {
        return "×" + Math.max(0, nutrition);
    }

    public static String formatSaturationQty(double saturation) {
        return "×" + formatSaturationAmount(saturation);
    }

    /**
     * Chance suffix when below 100%; empty otherwise.
     * {@code 0.5F} → {@code " 50%"}.
     */
    public static String formatChanceSuffix(float chance) {
        if (!(chance < 0.999F)) {
            return "";
        }
        int pct = Math.round(chance * 100.0F);
        if (pct < 0) {
            pct = 0;
        } else if (pct > 99) {
            pct = 99;
        }
        return " " + pct + "%";
    }

    /** Tick duration → {@code m:ss} (same idea as potion tooltips). */
    public static String formatDurationTicks(int ticks) {
        int totalSec = Math.max(0, ticks) / 20;
        int min = totalSec / 60;
        int sec = totalSec % 60;
        return min + ":" + (sec < 10 ? "0" : "") + sec;
    }
}
