package com.homestead.sellveg.block;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DropPointHarvestTest {
    @Test
    void destroyTimeMatchesVanillaChest() {
        assertEquals(2.5F, DropPointMining.DESTROY_TIME);
    }

    @Test
    void lootTableDropsTheDropPointItem() throws IOException {
        String json = readResource("/data/homestead_sell_veg/loot_tables/blocks/drop_point.json");
        assertTrue(json.contains("\"type\": \"minecraft:block\""));
        assertTrue(json.contains("homestead_sell_veg:drop_point"));
        assertTrue(json.contains("minecraft:survives_explosion"));
    }

    @Test
    void axeIsPreferredTool() throws IOException {
        String json = readResource("/data/minecraft/tags/blocks/mineable/axe.json");
        assertTrue(json.contains("homestead_sell_veg:drop_point"));
    }

    private static String readResource(String path) throws IOException {
        InputStream in = DropPointHarvestTest.class.getResourceAsStream(path);
        assertNotNull(in, "missing resource " + path);
        try (in) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
