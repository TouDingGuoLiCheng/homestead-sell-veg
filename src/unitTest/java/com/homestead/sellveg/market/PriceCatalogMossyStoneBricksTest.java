package com.homestead.sellveg.market;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure-JVM checks against PriceCatalog source + price catalog JSON (Minecraft types are not loadable here).
 */
class PriceCatalogMossyStoneBricksTest {

    @Test
    void mossyStoneBricksAreBuildExclusive() throws Exception {
        String src = readPriceCatalogSource();
        int buildSet = src.indexOf("private static Set<ResourceLocation> buildShopExclusiveSet");
        assertTrue(buildSet >= 0, "missing buildShopExclusiveSet");
        String buildSnippet = src.substring(buildSet, Math.min(buildSet + 2200, src.length()));
        assertTrue(buildSnippet.contains("minecraft:mossy_stone_bricks"), "mossy stone bricks should be build exclusive");
    }

    @Test
    void priceIsFixedFour() throws Exception {
        String src = readPriceCatalogSource();
        int bands = src.indexOf("private static void putBuildShopBands");
        assertTrue(bands >= 0, "missing putBuildShopBands method");
        String bandBody = src.substring(bands, Math.min(bands + 2800, src.length()));
        assertTrue(bandBody.contains("path.equals(\"mossy_stone_bricks\")"), "putBuildShopBands should mention mossy stone bricks");
        assertTrue(bandBody.contains("new int[]{4, 4}"), "mossy stone bricks band should be fixed 4");
        assertTrue(readPriceCatalogJson().contains("\"minecraft:mossy_stone_bricks\": 4"), "catalog price should be 4");
    }

    private static String readPriceCatalogSource() throws Exception {
        return Files.readString(
                Path.of("src/main/java/com/homestead/sellveg/market/PriceCatalog.java"),
                StandardCharsets.UTF_8);
    }

    private static String readPriceCatalogJson() throws Exception {
        return Files.readString(
                Path.of("src/main/resources/data/homestead_sell_veg/price_catalog/default.json"),
                StandardCharsets.UTF_8);
    }
}
