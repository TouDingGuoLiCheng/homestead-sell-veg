package com.homestead.sellveg.market;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure-JVM checks against PriceCatalog source + price catalog JSON (Minecraft types are not loadable here).
 */
class PriceCatalogCobbledDeepslateFlowerLeavesTest {

    private static final String[] ALL_LEAVES = {
            "oak_leaves", "spruce_leaves", "birch_leaves", "jungle_leaves", "acacia_leaves",
            "mangrove_leaves", "cherry_leaves", "azalea_leaves", "flowering_azalea_leaves",
            "dark_oak_leaves"
    };

    @Test
    void cobbledDeepslateIsBuildExclusiveLikeDeepslate() throws Exception {
        String src = readPriceCatalogSource();
        int buildSet = src.indexOf("private static Set<ResourceLocation> buildShopExclusiveSet");
        assertTrue(buildSet >= 0, "missing buildShopExclusiveSet");
        String buildSnippet = src.substring(buildSet, Math.min(buildSet + 1800, src.length()));
        assertTrue(buildSnippet.contains("minecraft:deepslate"), "deepslate should be build exclusive");
        assertTrue(buildSnippet.contains("minecraft:cobbled_deepslate"), "cobbled deepslate should be build exclusive");

        String json = readPriceCatalogJson();
        assertTrue(json.contains("\"minecraft:deepslate\""), "missing catalog price for deepslate");
        assertTrue(json.contains("\"minecraft:cobbled_deepslate\": 1"), "cobbled deepslate should share the deepslate base price 1");
    }

    @Test
    void vineIsFlowerShopExtraWithFiveToSixBand() throws Exception {
        String src = readPriceCatalogSource();
        int flowerExtra = src.indexOf("FLOWER_SHOP_EXTRA");
        assertTrue(flowerExtra >= 0, "missing FLOWER_SHOP_EXTRA");
        String snippet = src.substring(flowerExtra, Math.min(flowerExtra + 700, src.length()));
        assertTrue(snippet.contains("minecraft:vine"), "vine should be a flower-shop extra");

        assertTrue(src.contains("DEFAULT_FLOWER_VINE_MIN = 5"), "vine band min should be 5");
        assertTrue(src.contains("DEFAULT_FLOWER_VINE_MAX = 6"), "vine band max should be 6");
        assertTrue(src.contains("bands.put(new ResourceLocation(\"minecraft:vine\"), new int[]{DEFAULT_FLOWER_VINE_MIN, DEFAULT_FLOWER_VINE_MAX});"),
                "vine needs a listed 5-6 band");
        assertTrue(readPriceCatalogJson().contains("\"minecraft:vine\": 5"), "vine base price should be 5");
    }

    @Test
    void everyLeafIsFlowerShopExclusiveAtFixedOne() throws Exception {
        String src = readPriceCatalogSource();
        int flowerLeaf = src.indexOf("FLOWER_LEAF_PATHS");
        assertTrue(flowerLeaf >= 0, "missing FLOWER_LEAF_PATHS");
        String leafSnippet = src.substring(flowerLeaf, Math.min(flowerLeaf + 700, src.length()));
        String json = readPriceCatalogJson();
        for (String leaf : ALL_LEAVES) {
            assertTrue(leafSnippet.contains(leaf), leaf + " should be a flower-shop leaf");
            assertTrue(json.contains("\"minecraft:" + leaf + "\": 1"), "missing fixed-1 catalog price for " + leaf);
        }
        assertTrue(src.contains("bands.put(leaf, new int[]{1, 1});"), "flower leaves should keep the fixed-1 band");
    }

    @Test
    void darkOakLeavesMovedToFlowerShopAtFixedOne() throws Exception {
        String src = readPriceCatalogSource();
        int buildSet = src.indexOf("private static Set<ResourceLocation> buildShopExclusiveSet");
        String buildSnippet = src.substring(buildSet, Math.min(buildSet + 1800, src.length()));
        assertFalse(buildSnippet.contains("minecraft:dark_oak_leaves"), "dark oak leaves should have left build exclusives");
        assertTrue(readPriceCatalogJson().contains("\"minecraft:dark_oak_leaves\": 1"), "dark oak leaves keep their fixed-1 price");
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
