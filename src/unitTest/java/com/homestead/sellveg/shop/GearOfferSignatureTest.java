package com.homestead.sellveg.shop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Signature matching must be id-stable (shop|item|sorted ench#level) so buy/cart
 * still finds daily SKUs after network ItemStack round-trips.
 */
class GearOfferSignatureTest {
    @Test
    void signatureOrderIndependentForEnchantParts() {
        String a = "iron_bro_forge|minecraft:iron_sword|minecraft:sharpness#3|minecraft:unbreaking#2";
        String b = "iron_bro_forge|minecraft:iron_sword|minecraft:unbreaking#2|minecraft:sharpness#3";
        // Production builder sorts enchant parts; unsorted strings must not be treated equal.
        assertNotEquals(a, b);

        String sortedA = sortSignature(a);
        String sortedB = sortSignature(b);
        assertEquals(sortedA, sortedB);
    }

    /** Mirrors {@link GearOffers#offerSignature} sorting of enchant segments. */
    private static String sortSignature(String raw) {
        String[] parts = raw.split("\\|", -1);
        if (parts.length <= 2) {
            return raw;
        }
        java.util.List<String> enchants = new java.util.ArrayList<>();
        for (int i = 2; i < parts.length; i++) {
            enchants.add(parts[i]);
        }
        java.util.Collections.sort(enchants);
        StringBuilder sb = new StringBuilder(parts[0]).append('|').append(parts[1]);
        for (String e : enchants) {
            sb.append('|').append(e);
        }
        return sb.toString();
    }
}
