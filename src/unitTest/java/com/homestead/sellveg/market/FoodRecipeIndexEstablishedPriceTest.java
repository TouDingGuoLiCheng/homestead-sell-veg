package com.homestead.sellveg.market;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure-JVM checks that recipe ingredient pricing reuses established catalog prices.
 */
class FoodRecipeIndexEstablishedPriceTest {
    @Test
    void nestedPriceConsultsEstablishedCatalogPrice() throws Exception {
        String src = read("src/main/java/com/homestead/sellveg/market/FoodRecipeIndex.java");
        int method = src.indexOf("private static int nestedPrice");
        assertTrue(method >= 0, "missing nestedPrice");
        String body = src.substring(method, Math.min(method + 1500, src.length()));
        assertTrue(body.contains("establishedPriceCache"), "nestedPrice should read the established price cache");
        assertTrue(body.contains("PriceCatalog.getBasePrice(item)"),
                "nestedPrice should fall back to the established catalog price");
        assertTrue(body.contains("establishingPrice"), "nestedPrice should guard re-entrancy");
    }

    @Test
    void establishedCacheIsDroppedOnClearAndRebuild() throws Exception {
        String src = read("src/main/java/com/homestead/sellveg/market/FoodRecipeIndex.java");
        int clear = src.indexOf("public static void clear()");
        assertTrue(clear >= 0, "missing clear()");
        String clearBody = src.substring(clear, Math.min(clear + 600, src.length()));
        assertTrue(clearBody.contains("establishedPriceCache.clear()"), "clear() must drop the established price cache");
        int rebuild = src.indexOf("public static void rebuild");
        assertTrue(rebuild >= 0, "missing rebuild()");
        String rebuildBody = src.substring(rebuild, Math.min(rebuild + 900, src.length()));
        assertTrue(rebuildBody.contains("establishedPriceCache.clear()"), "rebuild() must drop the established price cache");
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }
}
