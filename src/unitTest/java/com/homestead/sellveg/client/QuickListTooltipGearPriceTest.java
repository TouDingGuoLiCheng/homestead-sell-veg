package com.homestead.sellveg.client;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure-JVM check that the quick-list tooltip scales gear prices by enchant/durability.
 */
class QuickListTooltipGearPriceTest {
    @Test
    void hoverPriceAppliesGearMultipliers() throws Exception {
        String src = Files.readString(
                Path.of("src/main/java/com/homestead/sellveg/client/QuickListTooltipClient.java"),
                StandardCharsets.UTF_8);
        int method = src.indexOf("private static QuickListHints.Hover hintFor");
        assertTrue(method >= 0, "missing hintFor");
        String body = src.substring(method, Math.min(method + 1200, src.length()));
        assertTrue(body.contains("GearOffers.isGearItem(hovered.getItem())"),
                "hover price should detect gear stacks");
        assertTrue(body.contains("GearOffers.suggestUnitPrice(mid, hovered)"),
                "hover price should apply the gear enchant/durability multiplier");
    }
}
