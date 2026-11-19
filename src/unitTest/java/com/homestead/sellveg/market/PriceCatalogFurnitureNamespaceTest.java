package com.homestead.sellveg.market;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure-JVM checks: PriceCatalog pulls Minecraft types, so we assert the soft-compat
 * list and mods.toml text instead of reflecting the live Set.
 */
class PriceCatalogFurnitureNamespaceTest {
    @Test
    void handcraftedIsListedInFurnitureModNamespaces() throws Exception {
        String src = Files.readString(
                Path.of("src/main/java/com/homestead/sellveg/market/PriceCatalog.java"),
                StandardCharsets.UTF_8);
        int setStart = src.indexOf("FURNITURE_MOD_NAMESPACES");
        assertTrue(setStart >= 0, "missing FURNITURE_MOD_NAMESPACES");
        String snippet = src.substring(setStart, Math.min(setStart + 280, src.length()));
        assertTrue(snippet.contains("\"handcrafted\""),
                "FURNITURE_MOD_NAMESPACES should include handcrafted");
    }

    @Test
    void modsTomlDeclaresHandcraftedOptionalDependency() throws Exception {
        String toml = Files.readString(
                Path.of("src/main/resources/META-INF/mods.toml"),
                StandardCharsets.UTF_8);
        assertTrue(toml.contains("modId=\"handcrafted\""), "mods.toml should soft-depend handcrafted");
        assertTrue(toml.contains("mandatory=false"), "furniture soft-deps stay optional");
    }
}
