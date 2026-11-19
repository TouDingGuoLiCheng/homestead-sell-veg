package com.homestead.sellveg.market;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure-JVM checks against PriceCatalog source + price catalog JSON (Minecraft types are not loadable here).
 */
class PriceCatalogMudBlocksTest {

    @Test
    void mudAndPackedMudAreBuildExclusive() throws Exception {
        String src = readPriceCatalogSource();
        int buildSet = src.indexOf("private static Set<ResourceLocation> buildShopExclusiveSet");
        assertTrue(buildSet >= 0, "missing buildShopExclusiveSet");
        String buildSnippet = src.substring(buildSet, Math.min(buildSet + 2000, src.length()));
        assertTrue(buildSnippet.contains("minecraft:mud\""), "mud should be build exclusive");
        assertTrue(buildSnippet.contains("minecraft:packed_mud"), "packed mud should be build exclusive");
    }

    @Test
    void bandOneToTwoAndEightToTen() throws Exception {
        String src = readPriceCatalogSource();
        int bands = src.indexOf("private static void putBuildShopBands");
        assertTrue(bands >= 0, "missing putBuildShopBands method");
        String bandBody = src.substring(bands, Math.min(bands + 2400, src.length()));
        assertTrue(bandBody.contains("path.equals(\"mud\")"), "putBuildShopBands should mention mud");
        assertTrue(bandBody.contains("path.equals(\"packed_mud\")"), "putBuildShopBands should mention packed mud");
        assertTrue(bandBody.contains("new int[]{1, 2}"), "mud band should be 1-2");
        assertTrue(bandBody.contains("new int[]{8, 10}"), "packed mud band should be 8-10");
    }

    @Test
    void catalogHasMudBasePrices() throws Exception {
        String json = readPriceCatalogJson();
        assertTrue(json.contains("\"minecraft:mud\": 1"), "mud catalog price should be 1");
        assertTrue(json.contains("\"minecraft:packed_mud\": 9"), "packed mud catalog price should be 9");
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
