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
class PriceCatalogFurnitureStorageTest {

    @Test
    void chestAndBarrelAreFurnitureExclusive() throws Exception {
        String src = readPriceCatalogSource();
        int whitelist = src.indexOf("private static Set<ResourceLocation> furnitureVanillaWhitelist");
        assertTrue(whitelist >= 0, "missing furnitureVanillaWhitelist");
        String snippet = src.substring(whitelist, Math.min(whitelist + 1800, src.length()));
        assertTrue(snippet.contains("minecraft:chest\""), "chest should be in the furniture whitelist");
        assertTrue(snippet.contains("minecraft:barrel"), "barrel should be in the furniture whitelist");

        int dual = src.indexOf("FURNITURE_DUAL_LISTED = Set.of(");
        assertTrue(dual >= 0, "missing FURNITURE_DUAL_LISTED");
        String dualSnippet = src.substring(dual, Math.min(dual + 400, src.length()));
        assertFalse(dualSnippet.contains("minecraft:chest"), "chest should not be dual-listed with build/furniture");
        assertFalse(dualSnippet.contains("minecraft:barrel"), "barrel should not be dual-listed with build/furniture");
    }

    private static String readPriceCatalogSource() throws Exception {
        return Files.readString(
                Path.of("src/main/java/com/homestead/sellveg/market/PriceCatalog.java"),
                StandardCharsets.UTF_8);
    }
}
