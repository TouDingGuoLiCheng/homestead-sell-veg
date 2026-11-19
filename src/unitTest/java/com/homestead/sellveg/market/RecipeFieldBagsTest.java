package com.homestead.sellveg.market;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeFieldBagsTest {
    public static final class DummyPot {
        public final List<Object> input = new ArrayList<>();
        public final List<Object> first = new ArrayList<>();
        public final List<Object> second = new ArrayList<>();
    }

    @Test
    void scansInputAndSplitCuisineFields() {
        DummyPot pot = new DummyPot();
        List<String> names = RecipeFieldBags.ingredientBagNames(pot);
        assertTrue(names.contains("input"));
        assertTrue(names.contains("first"));
        assertTrue(names.contains("second"));
    }
}
