package com.homestead.sellveg.market;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketBandRebaseTest {
    @Test
    void pricingRulesVersionBumpsPastCheapCatalogCraftSkip() {
        assertTrue(MarketData.PRICING_RULES_VERSION >= 7);
    }

    @Test
    void logBuildBandIsWiderThanCheapStone() {
        assertEquals(8, PriceCatalog.LOG_BUILD_MIN);
        assertEquals(12, PriceCatalog.LOG_BUILD_MAX);
        assertEquals(3, PriceCatalog.CHEAP_BUILD_MAX);
    }

    @Test
    void coralBuildBandIsTenToThirteen() {
        assertEquals(10, PriceCatalog.CORAL_BUILD_MIN);
        assertEquals(13, PriceCatalog.CORAL_BUILD_MAX);
    }

    @Test
    void froglightBuildBandIsFortyToFifty() {
        assertEquals(40, PriceCatalog.FROGLIGHT_BUILD_MIN);
        assertEquals(50, PriceCatalog.FROGLIGHT_BUILD_MAX);
    }

    @Test
    void endRodBuildBandIsThirtyToThirtyFive() {
        assertEquals(30, PriceCatalog.END_ROD_BUILD_MIN);
        assertEquals(35, PriceCatalog.END_ROD_BUILD_MAX);
    }

    @Test
    void shroomlightBuildBandIsFifteenToTwenty() {
        assertEquals(15, PriceCatalog.SHROOMLIGHT_BUILD_MIN);
        assertEquals(20, PriceCatalog.SHROOMLIGHT_BUILD_MAX);
    }

    @Test
    void coralBlocksHaveCatalogBasePrices() throws Exception {
        String json = readDefaultCatalogJson();
        String[] corals = {
                "minecraft:tube_coral_block", "minecraft:brain_coral_block", "minecraft:bubble_coral_block",
                "minecraft:fire_coral_block", "minecraft:horn_coral_block",
                "minecraft:dead_tube_coral_block", "minecraft:dead_brain_coral_block",
                "minecraft:dead_bubble_coral_block", "minecraft:dead_fire_coral_block",
                "minecraft:dead_horn_coral_block"
        };
        for (String id : corals) {
            assertTrue(json.contains("\"" + id + "\""), "missing catalog price for " + id);
        }
    }

    @Test
    void froglightsHaveCatalogBasePrices() throws Exception {
        String json = readDefaultCatalogJson();
        String[] froglights = {
                "minecraft:ochre_froglight",
                "minecraft:verdant_froglight",
                "minecraft:pearlescent_froglight"
        };
        for (String id : froglights) {
            assertTrue(json.contains("\"" + id + "\""), "missing catalog price for " + id);
        }
    }

    @Test
    void deepslateHasCatalogBasePrice() throws Exception {
        String json = readDefaultCatalogJson();
        assertTrue(json.contains("\"minecraft:deepslate\""), "missing catalog price for deepslate");
    }

    @Test
    void podzolHasCatalogBasePrice() throws Exception {
        String json = readDefaultCatalogJson();
        assertTrue(json.contains("\"minecraft:podzol\""), "missing catalog price for podzol");
    }

    @Test
    void endRodAndShroomlightHaveCatalogBasePrices() throws Exception {
        String json = readDefaultCatalogJson();
        assertTrue(json.contains("\"minecraft:end_rod\""), "missing catalog price for end_rod");
        assertTrue(json.contains("\"minecraft:shroomlight\""), "missing catalog price for shroomlight");
    }

    private static String readDefaultCatalogJson() throws Exception {
        var stream = PriceCatalog.class.getResourceAsStream("/data/homestead_sell_veg/price_catalog/default.json");
        assertNotNull(stream);
        try (var reader = new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8)) {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[2048];
            int n;
            while ((n = reader.read(buf)) >= 0) {
                sb.append(buf, 0, n);
            }
            return sb.toString();
        }
    }

    @Test
    void oldTinyBandBelowNewCraftFloorMustRebase() {
        assertTrue(PriceMath.needsCatalogBandRebase(4, 12, 80, 210, 160));
    }

    @Test
    void healthyBandAboveCraftFloorStays() {
        assertFalse(PriceMath.needsCatalogBandRebase(80, 210, 80, 210, 160));
    }

    @Test
    void invertedBandMustRebase() {
        assertTrue(PriceMath.needsCatalogBandRebase(50, 10, 80, 210, 0));
    }

    @Test
    void catalogOrigMovedEntirelyAboveLiveMustRebase() {
        assertTrue(PriceMath.needsCatalogBandRebase(4, 12, 160, 220, 0));
    }

    @Test
    void catalogCheapItemsIgnoreCraftWhenWideningBand() {
        // Terracotta-style: catalog 1–3, clay craft ≈40 must not become 1–41.
        assertEquals(0, PriceMath.craftFloorForLiveBand(40, true));
        assertEquals(3, PriceMath.bandHiWithCraftFloor(3, 40, true));
        assertFalse(PriceMath.needsCatalogBandRebase(1, 3, 1, 3, PriceMath.craftFloorForLiveBand(40, true)));
    }

    @Test
    void assembledItemsStillWidenBandWithCraftFloor() {
        assertEquals(160, PriceMath.craftFloorForLiveBand(160, false));
        assertEquals(161, PriceMath.bandHiWithCraftFloor(12, 160, false));
        assertTrue(PriceMath.needsCatalogBandRebase(4, 12, 80, 210,
                PriceMath.craftFloorForLiveBand(160, false)));
    }

    @Test
    void seedAndTerracottaCatalogBandsStayTiny() {
        assertEquals(1, PriceCatalog.SEED_MIN);
        assertEquals(3, PriceCatalog.SEED_MAX);
        assertEquals(3, PriceCatalog.CHEAP_BUILD_MAX);
    }
}
