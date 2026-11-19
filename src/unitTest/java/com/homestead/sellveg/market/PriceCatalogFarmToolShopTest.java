package com.homestead.sellveg.market;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure-JVM checks against PriceCatalog source (Minecraft types are not loadable here).
 */
class PriceCatalogFarmToolShopTest {
    @Test
    void farmToolSetCoversHoesShovelsShearsLeadAndSupplies() throws Exception {
        String src = readPriceCatalogSource();
        int set = src.indexOf("private static Set<ResourceLocation> farmToolShopExclusiveSet");
        assertTrue(set >= 0, "missing farmToolShopExclusiveSet");
        String body = src.substring(set, Math.min(set + 900, src.length()));
        assertTrue(body.contains("_hoe"), "hoes should be farm-tool exclusive");
        assertTrue(body.contains("_shovel"), "shovels should be farm-tool exclusive");
        assertTrue(body.contains("minecraft:shears"), "shears should be farm-tool exclusive");
        assertTrue(body.contains("minecraft:lead"), "lead should be farm-tool exclusive");
        assertTrue(body.contains("farmersdelight:organic_compost"), "organic compost should be farm-tool exclusive");
        assertTrue(body.contains("farmersdelight:straw"), "straw should be farm-tool exclusive");
        assertTrue(body.contains("farmersdelight:rich_soil"), "rich soil should be farm-tool exclusive");
    }

    @Test
    void farmToolDualListedKeepsBoneMealAndComposter() throws Exception {
        String src = readPriceCatalogSource();
        int set = src.indexOf("private static Set<ResourceLocation> farmToolDualListedSet");
        assertTrue(set >= 0, "missing farmToolDualListedSet");
        String body = src.substring(set, Math.min(set + 500, src.length()));
        assertTrue(body.contains("minecraft:bone_meal"), "bone meal should dual-list to farm tools");
        assertTrue(body.contains("minecraft:composter"), "composter should dual-list to farm tools");
    }

    @Test
    void strawSellsInTwoToFourBand() throws Exception {
        String src = readPriceCatalogSource();
        assertTrue(src.contains("farmersdelight:straw\"), new int[]{2, 4}"),
                "straw band should be 2-4");
    }

    @Test
    void richSoilMirrorsOrganicCompostPrice() throws Exception {
        String src = readPriceCatalogSource();
        int map = src.indexOf("FARM_TOOL_PRICE_MIRROR");
        assertTrue(map >= 0, "missing FARM_TOOL_PRICE_MIRROR");
        String body = src.substring(map, Math.min(map + 400, src.length()));
        assertTrue(body.contains("farmersdelight:rich_soil"), "rich soil should be the mirror key");
        assertTrue(body.contains("farmersdelight:organic_compost"), "organic compost should be the mirror target");
        assertTrue(src.contains("FARM_TOOL_PRICE_MIRROR.get(id)"), "mirror should be used in pricing");
    }

    private static String readPriceCatalogSource() throws Exception {
        return Files.readString(
                Path.of("src/main/java/com/homestead/sellveg/market/PriceCatalog.java"),
                StandardCharsets.UTF_8);
    }
}
