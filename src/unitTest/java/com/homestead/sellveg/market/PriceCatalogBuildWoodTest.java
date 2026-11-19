package com.homestead.sellveg.market;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure-JVM checks against PriceCatalog source (Minecraft types are not loadable here).
 */
class PriceCatalogBuildWoodTest {
    @Test
    void buildWoodPathsIncludeBarkAndStrippedVariants() throws Exception {
        String src = readPriceCatalogSource();
        int start = src.indexOf("BUILD_WOOD_PATHS");
        assertTrue(start >= 0, "missing BUILD_WOOD_PATHS");
        String snippet = src.substring(start, Math.min(start + 900, src.length()));
        assertTrue(snippet.contains("\"cherry_wood\""), "expected cherry_wood");
        assertTrue(snippet.contains("\"stripped_cherry_wood\""), "expected stripped_cherry_wood");
        assertTrue(snippet.contains("\"crimson_hyphae\""), "expected crimson_hyphae");
        assertTrue(snippet.contains("\"stripped_warped_hyphae\""), "expected stripped_warped_hyphae");
        assertTrue(snippet.contains("\"pale_oak_wood\""), "expected pale_oak_wood");
        assertFalse(snippet.contains("\"cherry_planks\""), "planks must stay out of BUILD_WOOD_PATHS");
    }

    @Test
    void buildWoodUsesCraftPricingBranch() throws Exception {
        String src = readPriceCatalogSource();
        assertTrue(src.contains("isBuildCraftPricedWood"), "expected craft-priced wood helper");
        assertTrue(src.contains("BUILD_WOOD_CRAFT_PRICED"), "expected BUILD_WOOD_CRAFT_PRICED set");
        int bands = src.indexOf("private static void putBuildShopBands");
        assertTrue(bands >= 0, "missing putBuildShopBands method");
        String bandBody = src.substring(bands, Math.min(bands + 800, src.length()));
        assertTrue(bandBody.contains("isBuildCraftPricedWood"),
                "putBuildShopBands should skip craft-priced wood listed bands");
    }

    private static String readPriceCatalogSource() throws Exception {
        return Files.readString(
                Path.of("src/main/java/com/homestead/sellveg/market/PriceCatalog.java"),
                StandardCharsets.UTF_8);
    }
}