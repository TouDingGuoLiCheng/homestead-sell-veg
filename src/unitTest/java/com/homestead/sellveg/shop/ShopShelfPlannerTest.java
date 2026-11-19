package com.homestead.sellveg.shop;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopShelfPlannerTest {
    @Test
    void unionCoversEveryItem() {
        ShopShelfPlanner.Assignment a = sample();
        Set<String> union = new HashSet<>();
        for (PresetShop shop : PresetShops.ALL) {
            union.addAll(a.itemsOf(shop.id()));
        }
        assertEquals(Set.of("wheat", "iron", "bread", "stick", "stone"), union);
    }

    @Test
    void themedShopsCarryMatchingKind() {
        ShopShelfPlanner.Assignment a = sample();
        assertTrue(a.sells("pulpy_orange", "wheat"));
        assertTrue(a.sells("golden_wheat", "wheat"));
        assertTrue(a.sells("moonlight_market", "wheat"));
        assertTrue(a.sells("space_ore", "iron"));
        assertTrue(a.sells("torn_bread", "bread"));
        assertTrue(a.sells("cola_two", "stick"));
        assertTrue(a.sells("block_build_co", "stone"));
        assertTrue(a.sells("civil_bro_supply", "stone"));
    }

    @Test
    void oreShopsNeverSellNonOre() {
        ShopShelfPlanner.Assignment a = sample();
        for (String oreShop : List.of("space_ore", "villager_mining", "deep_down")) {
            assertFalse(a.sells(oreShop, "bread"), oreShop + " must not sell food");
            assertFalse(a.sells(oreShop, "wheat"), oreShop + " must not sell farm goods");
            assertFalse(a.sells(oreShop, "stick"), oreShop + " must not sell grocery");
            assertFalse(a.sells(oreShop, "stone"), oreShop + " must not sell build goods");
        }
    }

    @Test
    void buildExclusiveStaysOffGeneralAndOtherThemes() {
        ShopShelfPlanner.Assignment a = sample();
        assertTrue(a.sells("block_build_co", "stone"));
        assertTrue(a.sells("civil_bro_supply", "stone"));
        assertFalse(a.sells("omni_mart", "stone"));
        assertFalse(a.sells("beacon_flagship", "stone"));
        assertFalse(a.sells("cola_two", "stone"));
        assertFalse(a.sells("torn_bread", "stone"));
        assertFalse(a.sells("pulpy_orange", "stone"));
        assertFalse(a.sells("space_ore", "stone"));
    }

    @Test
    void flowerExclusiveStaysOffGeneralAndOtherThemes() {
        ShopShelfPlanner.Assignment a = sampleWithFlower();
        assertTrue(a.sells("forget_me_not", "dandelion"));
        assertTrue(a.sells("fragrant_tale", "dandelion"));
        assertFalse(a.sells("omni_mart", "dandelion"));
        assertFalse(a.sells("pulpy_orange", "dandelion"));
        assertFalse(a.sells("block_build_co", "dandelion"));
    }

    @Test
    void flowerExtraDecorExclusiveStaysOffGeneralAndOtherThemes() {
        ShopShelfPlanner.Assignment a = sampleWithFlowerExtra();
        assertTrue(a.sells("forget_me_not", "lily_pad"));
        assertTrue(a.sells("fragrant_tale", "spore_blossom"));
        assertTrue(a.sells("fragrant_tale", "azalea"));
        assertTrue(a.sells("forget_me_not", "sea_pickle"));
        assertFalse(a.sells("omni_mart", "lily_pad"));
        assertFalse(a.sells("block_build_co", "big_dripleaf"));
        assertFalse(a.sells("pulpy_orange", "small_dripleaf"));
        assertFalse(a.sells("omni_mart", "sea_pickle"));
    }

    @Test
    void redstoneExclusiveStaysOffGeneralAndOtherThemes() {
        ShopShelfPlanner.Assignment a = sampleWithRedstone();
        assertTrue(a.sells("red_stone_shop", "piston"));
        assertFalse(a.sells("omni_mart", "piston"));
        assertFalse(a.sells("space_ore", "piston"));
        assertFalse(a.sells("block_build_co", "piston"));
        assertFalse(a.sells("cola_two", "piston"));
    }

    @Test
    void weaponExclusiveStaysOffGeneralAndOtherThemes() {
        ShopShelfPlanner.Assignment a = sampleWithWeapon();
        assertTrue(a.sells("iron_bro_forge", "iron_sword"));
        assertFalse(a.sells("omni_mart", "iron_sword"));
        assertFalse(a.sells("scaredy_armor", "iron_sword"));
        assertFalse(a.sells("space_ore", "iron_sword"));
        assertFalse(a.sells("cola_two", "iron_sword"));
    }

    @Test
    void armorExclusiveStaysOffGeneralAndOtherThemes() {
        ShopShelfPlanner.Assignment a = sampleWithArmor();
        assertTrue(a.sells("scaredy_armor", "iron_chestplate"));
        assertFalse(a.sells("omni_mart", "iron_chestplate"));
        assertFalse(a.sells("iron_bro_forge", "iron_chestplate"));
        assertFalse(a.sells("block_build_co", "iron_chestplate"));
        assertFalse(a.sells("cola_two", "iron_chestplate"));
    }

    @Test
    void furnitureExclusiveStaysOffGeneralAndOtherThemes() {
        ShopShelfPlanner.Assignment a = sampleWithFurniture();
        assertTrue(a.sells("cozy_furniture", "oak_door"));
        assertFalse(a.sells("omni_mart", "oak_door"));
        assertFalse(a.sells("beacon_flagship", "oak_door"));
        assertFalse(a.sells("block_build_co", "oak_door"));
        assertFalse(a.sells("cola_two", "oak_door"));
    }

    @Test
    void furnitureDualListsBuildAndFurnitureShop() {
        ShopShelfPlanner.Assignment a = sampleWithFurnitureDualBuild();
        assertTrue(a.sells("block_build_co", "campfire"));
        assertTrue(a.sells("civil_bro_supply", "campfire"));
        assertTrue(a.sells("cozy_furniture", "campfire"));
        assertFalse(a.sells("omni_mart", "campfire"));
    }

    @Test
    void furnitureDualListsRedstoneAndFurnitureShop() {
        ShopShelfPlanner.Assignment a = sampleWithFurnitureDualRedstone();
        assertTrue(a.sells("red_stone_shop", "jukebox"));
        assertTrue(a.sells("cozy_furniture", "jukebox"));
        assertFalse(a.sells("omni_mart", "jukebox"));
    }

    @Test
    void redstoneDustDualListsOreAndRedstoneShop() {
        ShopShelfPlanner.Assignment a = sampleWithDualRedstone();
        assertTrue(a.sells("space_ore", "redstone"));
        assertTrue(a.sells("villager_mining", "redstone"));
        assertTrue(a.sells("deep_down", "redstone"));
        assertTrue(a.sells("red_stone_shop", "redstone"));
    }

    @Test
    void oreExclusiveStaysOffGeneralAndOtherThemes() {
        ShopShelfPlanner.Assignment a = sampleWithSand();
        assertTrue(a.sells("space_ore", "sand"));
        assertTrue(a.sells("villager_mining", "sand"));
        assertTrue(a.sells("deep_down", "sand"));
        assertFalse(a.sells("omni_mart", "sand"));
        assertFalse(a.sells("block_build_co", "sand"));
    }

    @Test
    void sameItemCanAppearInMultipleShops() {
        ShopShelfPlanner.Assignment a = sample();
        assertTrue(a.shopsFor("wheat").size() >= 3);
    }

    @Test
    void farmToolExclusiveStaysOffGeneralAndOtherThemes() {
        ShopShelfPlanner.Assignment a = sampleWithFarmTool();
        assertTrue(a.sells("pulpy_orange_tools", "iron_hoe"));
        assertFalse(a.sells("omni_mart", "iron_hoe"));
        assertFalse(a.sells("pulpy_orange", "iron_hoe"));
        assertFalse(a.sells("block_build_co", "iron_hoe"));
    }

    @Test
    void farmToolDualListsGroceryAndFarmToolShop() {
        ShopShelfPlanner.Assignment a = sampleWithFarmToolDualGrocery();
        assertTrue(a.sells("cola_two", "bone_meal"));
        assertTrue(a.sells("pulpy_orange_tools", "bone_meal"));
        assertFalse(a.sells("pulpy_orange", "bone_meal"));
        assertFalse(a.sells("space_ore", "bone_meal"));
    }

    @Test
    void farmToolDualListsFurnitureAndFarmToolShop() {
        ShopShelfPlanner.Assignment a = sampleWithFarmToolDualFurniture();
        assertTrue(a.sells("cozy_furniture", "composter"));
        assertTrue(a.sells("pulpy_orange_tools", "composter"));
        assertFalse(a.sells("omni_mart", "composter"));
    }

    @Test
    void unknownShopSellsNothing() {
        ShopShelfPlanner.Assignment a = sample();
        assertFalse(a.sells("missing_shop", "wheat"));
    }

    private static ShopShelfPlanner.Assignment sample() {
        List<ShopShelfPlanner.ItemRef> items = new ArrayList<>();
        items.add(new ShopShelfPlanner.ItemRef("wheat", ItemShelfKind.FARM, false));
        items.add(new ShopShelfPlanner.ItemRef("iron", ItemShelfKind.ORE, false));
        items.add(new ShopShelfPlanner.ItemRef("bread", ItemShelfKind.FOOD, false));
        items.add(new ShopShelfPlanner.ItemRef("stick", ItemShelfKind.GROCERY, false));
        items.add(new ShopShelfPlanner.ItemRef("stone", ItemShelfKind.BUILD, true));
        return ShopShelfPlanner.assign(PresetShops.ALL, items);
    }

    private static ShopShelfPlanner.Assignment sampleWithSand() {
        List<ShopShelfPlanner.ItemRef> items = new ArrayList<>();
        items.add(new ShopShelfPlanner.ItemRef("sand", ItemShelfKind.ORE, true));
        return ShopShelfPlanner.assign(PresetShops.ALL, items);
    }

    private static ShopShelfPlanner.Assignment sampleWithFlower() {
        List<ShopShelfPlanner.ItemRef> items = new ArrayList<>();
        items.add(new ShopShelfPlanner.ItemRef("dandelion", ItemShelfKind.FLOWER, true));
        return ShopShelfPlanner.assign(PresetShops.ALL, items);
    }

    private static ShopShelfPlanner.Assignment sampleWithFlowerExtra() {
        List<ShopShelfPlanner.ItemRef> items = new ArrayList<>();
        items.add(new ShopShelfPlanner.ItemRef("lily_pad", ItemShelfKind.FLOWER, true));
        items.add(new ShopShelfPlanner.ItemRef("spore_blossom", ItemShelfKind.FLOWER, true));
        items.add(new ShopShelfPlanner.ItemRef("big_dripleaf", ItemShelfKind.FLOWER, true));
        items.add(new ShopShelfPlanner.ItemRef("small_dripleaf", ItemShelfKind.FLOWER, true));
        items.add(new ShopShelfPlanner.ItemRef("sea_pickle", ItemShelfKind.FLOWER, true));
        items.add(new ShopShelfPlanner.ItemRef("azalea", ItemShelfKind.FLOWER, true));
        return ShopShelfPlanner.assign(PresetShops.ALL, items);
    }

    private static ShopShelfPlanner.Assignment sampleWithRedstone() {
        List<ShopShelfPlanner.ItemRef> items = new ArrayList<>();
        items.add(new ShopShelfPlanner.ItemRef("piston", ItemShelfKind.REDSTONE, true));
        return ShopShelfPlanner.assign(PresetShops.ALL, items);
    }

    private static ShopShelfPlanner.Assignment sampleWithDualRedstone() {
        List<ShopShelfPlanner.ItemRef> items = new ArrayList<>();
        items.add(new ShopShelfPlanner.ItemRef("redstone", ItemShelfKind.ORE, false, true));
        return ShopShelfPlanner.assign(PresetShops.ALL, items);
    }

    private static ShopShelfPlanner.Assignment sampleWithWeapon() {
        List<ShopShelfPlanner.ItemRef> items = new ArrayList<>();
        items.add(new ShopShelfPlanner.ItemRef("iron_sword", ItemShelfKind.WEAPON, true));
        return ShopShelfPlanner.assign(PresetShops.ALL, items);
    }

    private static ShopShelfPlanner.Assignment sampleWithArmor() {
        List<ShopShelfPlanner.ItemRef> items = new ArrayList<>();
        items.add(new ShopShelfPlanner.ItemRef("iron_chestplate", ItemShelfKind.ARMOR, true));
        return ShopShelfPlanner.assign(PresetShops.ALL, items);
    }

    private static ShopShelfPlanner.Assignment sampleWithFurniture() {
        List<ShopShelfPlanner.ItemRef> items = new ArrayList<>();
        items.add(new ShopShelfPlanner.ItemRef("oak_door", ItemShelfKind.FURNITURE, true));
        return ShopShelfPlanner.assign(PresetShops.ALL, items);
    }

    private static ShopShelfPlanner.Assignment sampleWithFurnitureDualBuild() {
        List<ShopShelfPlanner.ItemRef> items = new ArrayList<>();
        items.add(new ShopShelfPlanner.ItemRef("campfire", ItemShelfKind.BUILD, true, false, true));
        return ShopShelfPlanner.assign(PresetShops.ALL, items);
    }

    private static ShopShelfPlanner.Assignment sampleWithFurnitureDualRedstone() {
        List<ShopShelfPlanner.ItemRef> items = new ArrayList<>();
        items.add(new ShopShelfPlanner.ItemRef("jukebox", ItemShelfKind.REDSTONE, true, false, true));
        return ShopShelfPlanner.assign(PresetShops.ALL, items);
    }

    private static ShopShelfPlanner.Assignment sampleWithFarmTool() {
        List<ShopShelfPlanner.ItemRef> items = new ArrayList<>();
        items.add(new ShopShelfPlanner.ItemRef("iron_hoe", ItemShelfKind.FARM_TOOL, true));
        return ShopShelfPlanner.assign(PresetShops.ALL, items);
    }

    private static ShopShelfPlanner.Assignment sampleWithFarmToolDualGrocery() {
        List<ShopShelfPlanner.ItemRef> items = new ArrayList<>();
        items.add(new ShopShelfPlanner.ItemRef("bone_meal", ItemShelfKind.GROCERY, false, false, false, true));
        return ShopShelfPlanner.assign(PresetShops.ALL, items);
    }

    private static ShopShelfPlanner.Assignment sampleWithFarmToolDualFurniture() {
        List<ShopShelfPlanner.ItemRef> items = new ArrayList<>();
        items.add(new ShopShelfPlanner.ItemRef("composter", ItemShelfKind.FURNITURE, true, false, false, true));
        return ShopShelfPlanner.assign(PresetShops.ALL, items);
    }
}
