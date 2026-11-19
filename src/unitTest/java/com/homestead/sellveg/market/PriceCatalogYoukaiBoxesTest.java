package com.homestead.sellveg.market;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure-JVM checks for Youkai's Homecoming packed-crop soft compat.
 */
class PriceCatalogYoukaiBoxesTest {
    private static final String[] BOXES = {
            "pod_crate", "cucumber_crate", "red_grape_crate", "white_grape_crate", "black_grape_crate",
            "soybean_bag", "redbean_bag", "coffee_bean_bag", "tea_leaf_bag", "green_tea_bag",
            "black_tea_bag", "oolong_tea_bag", "white_tea_bag", "dark_tea_bag", "yellow_tea_bag"
    };

    @Test
    void everyBoxHasCatalogPriceAndListedBand() throws Exception {
        String json = Files.readString(
                Path.of("src/main/resources/data/homestead_sell_veg/price_catalog/default.json"),
                StandardCharsets.UTF_8);
        String src = Files.readString(
                Path.of("src/main/java/com/homestead/sellveg/market/PriceCatalog.java"),
                StandardCharsets.UTF_8);
        for (String box : BOXES) {
            String id = "youkaishomecoming:" + box;
            assertTrue(json.contains("\"" + id + "\""), id + " missing catalog price");
            assertTrue(src.contains("youkaishomecoming:" + box + "\"), new int[]{"),
                    id + " missing listed band");
        }
    }

    @Test
    void bagSuffixCountsAsFarmShelf() throws Exception {
        String src = Files.readString(
                Path.of("src/main/java/com/homestead/sellveg/market/PriceCatalog.java"),
                StandardCharsets.UTF_8);
        assertTrue(src.contains("path.endsWith(\"_bag\")"), "_bag should classify as farm shelf");
    }
}
