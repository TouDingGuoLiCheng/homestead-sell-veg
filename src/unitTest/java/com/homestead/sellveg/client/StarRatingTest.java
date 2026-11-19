package com.homestead.sellveg.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StarRatingTest {
    @Test
    void fourAndAHalfUsesHalfOnLastStar() {
        assertEquals(StarRating.Fill.FULL, StarRating.fill(0, 4.5F));
        assertEquals(StarRating.Fill.FULL, StarRating.fill(3, 4.5F));
        assertEquals(StarRating.Fill.HALF, StarRating.fill(4, 4.5F));
    }

    @Test
    void zeroIsAllEmpty() {
        for (int i = 0; i < 5; i++) {
            assertEquals(StarRating.Fill.EMPTY, StarRating.fill(i, 0.0F));
        }
    }

    @Test
    void roundsToNearestHalf() {
        assertEquals(4.5F, StarRating.roundToHalf(4.4F));
        assertEquals(4.0F, StarRating.roundToHalf(4.2F));
    }
}
