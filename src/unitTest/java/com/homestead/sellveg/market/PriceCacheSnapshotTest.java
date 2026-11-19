package com.homestead.sellveg.market;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PriceCacheSnapshotTest {
    @Test
    void sameModsRecipesShareFingerprint() {
        String a = PriceCacheSnapshot.fingerprint(List.of("a@1", "b@2"), 10, 99L);
        String b = PriceCacheSnapshot.fingerprint(List.of("a@1", "b@2"), 10, 99L);
        assertTrue(a.startsWith(PriceCacheSnapshot.FORMULA_VERSION));
        assertEquals(a, b);
    }

    @Test
    void formulaVersionIsPastStaleQuartzCache() {
        assertNotEquals("quartz-restore-v1", PriceCacheSnapshot.FORMULA_VERSION);
    }

    @Test
    void modVersionChangeBreaksFingerprint() {
        String a = PriceCacheSnapshot.fingerprint(List.of("a@1"), 10, 99L);
        String b = PriceCacheSnapshot.fingerprint(List.of("a@2"), 10, 99L);
        assertNotEquals(a, b);
    }

    @Test
    void recipeHashChangeBreaksFingerprint() {
        String a = PriceCacheSnapshot.fingerprint(List.of("a@1"), 10, 1L);
        String b = PriceCacheSnapshot.fingerprint(List.of("a@1"), 10, 2L);
        assertNotEquals(a, b);
    }

    @Test
    void roundTripKeepsPricesAndTradeable() {
        PriceCacheSnapshot snap = new PriceCacheSnapshot(
                "fp",
                Map.of("minecraft:wheat", 10, "minecraft:apple", 4),
                List.of("minecraft:wheat", "minecraft:apple"),
                Map.of("minecraft:bread", 3)
        );
        PriceCacheSnapshot loaded = PriceCacheSnapshot.decode(snap.encode());
        assertTrue(loaded.matches("fp"));
        assertEquals(10, loaded.prices().get("minecraft:wheat"));
        assertEquals(List.of("minecraft:wheat", "minecraft:apple"), loaded.tradeable());
        assertEquals(3, loaded.crafts().get("minecraft:bread"));
    }

    @Test
    void v1BlobStillLoadsWithoutCrafts() {
        String blob = "v1\nfp\n1\nminecraft:wheat=10\n1\nminecraft:wheat\n";
        PriceCacheSnapshot loaded = PriceCacheSnapshot.decode(blob);
        assertEquals(10, loaded.prices().get("minecraft:wheat"));
        assertTrue(loaded.crafts().isEmpty());
    }
}
