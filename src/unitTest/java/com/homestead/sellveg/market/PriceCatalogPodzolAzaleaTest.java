package com.homestead.sellveg.market;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure-JVM checks against PriceCatalog source (Minecraft types are not loadable here).
 */
class PriceCatalogPodzolAzaleaTest {
    @Test
    void azaleaIsFlowerShopExtraNotBuildExclusive() throws Exception {
        String src = readPriceCatalogSource();
        int flowerExtra = src.indexOf("FLOWER_SHOP_EXTRA");
        assertTrue(flowerExtra >= 0, "missing FLOWER_SHOP_EXTRA");
        String flowerSnippet = src.substring(flowerExtra, Math.min(flowerExtra + 500, src.length()));
        assertTrue(flowerSnippet.contains("minecraft:azalea"), "azalea should be in FLOWER_SHOP_EXTRA");

        int buildSet = src.indexOf("private static Set<ResourceLocation> buildShopExclusiveSet");
        assertTrue(buildSet >= 0, "missing buildShopExclusiveSet");
        String buildSnippet = src.substring(buildSet, Math.min(buildSet + 1200, src.length()));
        assertTrue(buildSnippet.contains("minecraft:podzol"), "podzol should be build exclusive");
        assertTrue(!buildSnippet.contains("minecraft:azalea"), "azalea must not be build exclusive");
    }

    @Test
    void podzolUsesTwoToThreeBand() throws Exception {
        String src = readPriceCatalogSource();
        int bands = src.indexOf("private static void putBuildShopBands");
        assertTrue(bands >= 0, "missing putBuildShopBands method");
        String bandBody = src.substring(bands, Math.min(bands + 1800, src.length()));
        assertTrue(bandBody.contains("podzol"), "putBuildShopBands should mention podzol");
        assertTrue(bandBody.contains("new int[]{2, 3}"), "podzol band should be 2-3");
    }

    private static String readPriceCatalogSource() throws Exception {
        return Files.readString(
                Path.of("src/main/java/com/homestead/sellveg/market/PriceCatalog.java"),
                StandardCharsets.UTF_8);
    }
}
