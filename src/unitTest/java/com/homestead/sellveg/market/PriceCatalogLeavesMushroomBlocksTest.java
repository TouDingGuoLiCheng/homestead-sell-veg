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
class PriceCatalogLeavesMushroomBlocksTest {
    @Test
    void mushroomBlocksAreBuildExclusiveAndDarkOakLeavesMovedOut() throws Exception {
        String src = readPriceCatalogSource();
        int buildSet = src.indexOf("private static Set<ResourceLocation> buildShopExclusiveSet");
        assertTrue(buildSet >= 0, "missing buildShopExclusiveSet");
        String buildSnippet = src.substring(buildSet, Math.min(buildSet + 1600, src.length()));
        assertFalse(buildSnippet.contains("minecraft:dark_oak_leaves"), "dark oak leaves should no longer be build exclusive");
        assertTrue(buildSnippet.contains("minecraft:brown_mushroom_block"), "brown mushroom block should be build exclusive");
        assertTrue(buildSnippet.contains("minecraft:red_mushroom_block"), "red mushroom block should be build exclusive");
        assertTrue(buildSnippet.contains("minecraft:mushroom_stem"), "mushroom stem should be build exclusive");
    }

    @Test
    void mushroomBlocksTwoToThreeAndStemBeforeLogBand() throws Exception {
        String src = readPriceCatalogSource();
        int bands = src.indexOf("private static void putBuildShopBands");
        assertTrue(bands >= 0, "missing putBuildShopBands method");
        String bandBody = src.substring(bands, Math.min(bands + 1600, src.length()));
        assertTrue(bandBody.contains("brown_mushroom_block"), "putBuildShopBands should mention brown_mushroom_block");
        assertTrue(bandBody.contains("red_mushroom_block"), "putBuildShopBands should mention red_mushroom_block");
        assertTrue(bandBody.contains("mushroom_stem"), "putBuildShopBands should mention mushroom_stem");
        int mushroomBand = bandBody.indexOf("mushroom_stem");
        int stemLogBand = bandBody.indexOf("endsWith(\"_stem\")");
        assertTrue(mushroomBand >= 0 && stemLogBand >= 0 && mushroomBand < stemLogBand,
                "mushroom_stem band must be assigned before endsWith(\"_stem\") log pricing");
    }

    @Test
    void catalogHasMushroomBlockBasePrices() throws Exception {
        String json = Files.readString(
                Path.of("src/main/resources/data/homestead_sell_veg/price_catalog/default.json"),
                StandardCharsets.UTF_8);
        assertTrue(json.contains("\"minecraft:brown_mushroom_block\""), "missing catalog price for brown mushroom block");
        assertTrue(json.contains("\"minecraft:red_mushroom_block\""), "missing catalog price for red mushroom block");
        assertTrue(json.contains("\"minecraft:mushroom_stem\""), "missing catalog price for mushroom stem");
    }

    private static String readPriceCatalogSource() throws Exception {
        return Files.readString(
                Path.of("src/main/java/com/homestead/sellveg/market/PriceCatalog.java"),
                StandardCharsets.UTF_8);
    }
}
