package com.homestead.sellveg.market;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure-JVM checks against PriceCatalog source + price catalog JSON (Minecraft types are not loadable here).
 */
class PriceCatalogConcretePowderTest {

    private static final String[] COLORS = {
            "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
            "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
    };

    @Test
    void everyConcretePowderIsBuildExclusive() throws Exception {
        String src = readPriceCatalogSource();
        assertTrue(src.contains("set.add(new ResourceLocation(\"minecraft\", color + \"_concrete_powder\"));"),
                "the dye loop should add concrete_powder for every color");
    }

    @Test
    void concretePowderSharesConcreteBand() throws Exception {
        String src = readPriceCatalogSource();
        int bands = src.indexOf("private static void putBuildShopBands");
        assertTrue(bands >= 0, "missing putBuildShopBands method");
        String bandBody = src.substring(bands, Math.min(bands + 2600, src.length()));
        assertTrue(bandBody.contains("path.endsWith(\"_concrete_powder\")"),
                "putBuildShopBands should route concrete powder");
        assertTrue(bandBody.contains("new int[]{13, 16}"), "concrete powder band should be 13-16");
    }

    @Test
    void catalogHasAllConcretePowderBasePrices() throws Exception {
        String json = readPriceCatalogJson();
        for (String color : COLORS) {
            assertTrue(json.contains("\"minecraft:" + color + "_concrete_powder\": 14"),
                    "missing catalog price for " + color + "_concrete_powder");
        }
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
