package com.homestead.sellveg.shop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductReviewPickerTest {
    @Test
    void sameShopAndItemIsStable() {
        ProductReviewPicker.Pick a = ProductReviewPicker.pick("pulpy_orange", "minecraft:wheat", ItemShelfKind.FARM);
        ProductReviewPicker.Pick b = ProductReviewPicker.pick("pulpy_orange", "minecraft:wheat", ItemShelfKind.FARM);
        assertEquals(a, b);
    }

    @Test
    void differentShopsCanChangeCopy() {
        ProductReviewPicker.Pick a = ProductReviewPicker.pick("pulpy_orange", "minecraft:wheat", ItemShelfKind.FARM);
        ProductReviewPicker.Pick b = ProductReviewPicker.pick("golden_wheat", "minecraft:wheat", ItemShelfKind.FARM);
        assertNotEquals(a, b);
    }

    @Test
    void twoLinesUseDifferentNicks() {
        ProductReviewPicker.Pick pick = ProductReviewPicker.pick("omni_mart", "minecraft:stick", ItemShelfKind.GROCERY);
        assertNotEquals(pick.line0().nickIndex(), pick.line1().nickIndex());
    }

    @Test
    void ratingIsHalfStarAndAtMostFive() {
        ProductReviewPicker.Pick pick = ProductReviewPicker.pick("space_ore", "minecraft:iron_ingot", ItemShelfKind.ORE);
        assertTrue(pick.rating() >= 1.0F && pick.rating() <= 5.0F);
        assertEquals(pick.rating(), Math.round(pick.rating() * 2) / 2.0F);
    }

    @Test
    void furnitureKindUsesFurniturePoolWhenNotSpecial() {
        String pool = ProductReviewPicker.poolFor("minecraft:oak_door", ItemShelfKind.FURNITURE);
        assertEquals("furniture", pool);
    }

    @Test
    void farmToolKindUsesFarmToolPoolWhenNotSpecial() {
        String pool = ProductReviewPicker.poolFor("minecraft:iron_hoe", ItemShelfKind.FARM_TOOL);
        assertEquals("farm_tool", pool);
    }

    @Test
    void someOffersIncludeABadReview() {
        boolean found = false;
        for (int i = 0; i < 400; i++) {
            ProductReviewPicker.Pick pick = ProductReviewPicker.pick("omni_mart", "minecraft:item_" + i, ItemShelfKind.GROCERY);
            if ("bad".equals(pick.line0().pool()) || "bad".equals(pick.line1().pool())) {
                found = true;
                float low = Math.min(pick.line0().stars(), pick.line1().stars());
                assertTrue(low <= 2.5F);
                break;
            }
        }
        assertTrue(found);
    }
}
