package com.homestead.sellveg.client;

/**
 * Five-star display in 0.5 steps. Index 0 is the leftmost star.
 */
public final class StarRating {
    public enum Fill {
        EMPTY, HALF, FULL
    }

    private StarRating() {
    }

    public static float roundToHalf(float rating) {
        if (Float.isNaN(rating) || rating <= 0.0F) {
            return 0.0F;
        }
        float stepped = Math.round(rating * 2.0F) / 2.0F;
        return Math.min(5.0F, stepped);
    }

    public static Fill fill(int index, float rating) {
        float rounded = roundToHalf(rating);
        float remain = rounded - index;
        if (remain >= 1.0F) {
            return Fill.FULL;
        }
        if (remain >= 0.5F) {
            return Fill.HALF;
        }
        return Fill.EMPTY;
    }
}
