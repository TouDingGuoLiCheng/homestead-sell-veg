package com.homestead.sellveg.market;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.homestead.sellveg.NetShopMod;
import com.homestead.sellveg.config.SellVegConfig;
import com.homestead.sellveg.shop.ItemShelfKind;
import com.homestead.sellveg.shop.ShopOffers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.food.FoodProperties;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Catalog eligibility + base prices loaded from JSON.
 */
public final class PriceCatalog {
    private static final Logger LOGGER = LoggerFactory.getLogger(PriceCatalog.class);
    private static List<Item> tradeableCache;

    public static final int DEFAULT_CROP_PRICE = 8;
    public static final int DEFAULT_SEED_PRICE = 3;
    public static final int WHEAT_ANCHOR_PRICE = 10;
    public static final int SEED_MIN = 1;
    public static final int SEED_MAX = 3;
    /** Stone / terracotta and similar build-shop bulk blocks. */
    public static final int CHEAP_BUILD_MIN = 1;
    public static final int CHEAP_BUILD_MAX = 3;
    /**
     * There are no fallback bands: an item whose price cannot be derived (listed price, recipe
     * materials, mirrored sibling, or explicit constant) is unpriced and never reaches a shelf.
     */
    /** Build-shop logs / stems (and stripped variants). */
    public static final int LOG_BUILD_MIN = 8;
    public static final int LOG_BUILD_MAX = 12;
    /** Build-shop coral blocks (live + dead). */
    public static final int CORAL_BUILD_MIN = 10;
    public static final int CORAL_BUILD_MAX = 13;
    /** Build-shop froglights (ochre / verdant / pearlescent). */
    public static final int FROGLIGHT_BUILD_MIN = 40;
    public static final int FROGLIGHT_BUILD_MAX = 50;
    /** Build-shop end rod. */
    public static final int END_ROD_BUILD_MIN = 30;
    public static final int END_ROD_BUILD_MAX = 35;
    /** Build-shop shroomlight. */
    public static final int SHROOMLIGHT_BUILD_MIN = 15;
    public static final int SHROOMLIGHT_BUILD_MAX = 20;
    public static final int CROP_MIN = 5;
    public static final int CROP_MAX = 10;
    public static final int DEFAULT_SMALL_FLOWER_PRICE = 6;
    public static final int DEFAULT_TALL_FLOWER_PRICE = 10;
    public static final int DEFAULT_FLOWER_EXTRA_PRICE = 8;
    public static final int DEFAULT_FLOWER_VINE_MIN = 5;
    public static final int DEFAULT_FLOWER_VINE_MAX = 6;
    public static final int DEFAULT_FLOWER_POT_PRICE = 4;
    public static final int WITHER_ROSE_PRICE = 15;

    private static final ResourceLocation MINECRAFT_FLOWERS = new ResourceLocation("minecraft", "flowers");
    private static final ResourceLocation MINECRAFT_SMALL_FLOWERS = new ResourceLocation("minecraft", "small_flowers");
    private static final ResourceLocation MINECRAFT_TALL_FLOWERS = new ResourceLocation("minecraft", "tall_flowers");
    /** Decorative plants not in {@code #minecraft:flowers}, sold only in flower shops. */
    private static final Set<ResourceLocation> FLOWER_SHOP_EXTRA = Set.of(
            new ResourceLocation("minecraft:small_dripleaf"),
            new ResourceLocation("minecraft:big_dripleaf"),
            new ResourceLocation("minecraft:spore_blossom"),
            new ResourceLocation("minecraft:lily_pad"),
            new ResourceLocation("minecraft:sea_pickle"),
            // Not in #minecraft:flowers (unlike flowering_azalea); same decorative-plant price band.
            new ResourceLocation("minecraft:azalea"),
            new ResourceLocation("minecraft:vine")
    );

    /** Vanilla leaves sold only in flower shops, all at the fixed 1 of the former dark oak entry. */
    private static final String[] FLOWER_LEAF_PATHS = {
            "oak_leaves", "spruce_leaves", "birch_leaves", "jungle_leaves", "acacia_leaves",
            "mangrove_leaves", "cherry_leaves", "azalea_leaves", "flowering_azalea_leaves",
            "dark_oak_leaves"
    };
    private static final Set<ResourceLocation> FLOWER_SHOP_LEAVES = flowerShopLeafSet();
    private static final Set<ResourceLocation> EDGE_FARM = Set.of(
            new ResourceLocation("minecraft:sugar_cane"),
            new ResourceLocation("minecraft:sugar"),
            new ResourceLocation("minecraft:sweet_berries"),
            new ResourceLocation("minecraft:glow_berries"),
            new ResourceLocation("minecraft:melon"),
            new ResourceLocation("minecraft:melon_slice"),
            new ResourceLocation("minecraft:pumpkin"),
            new ResourceLocation("minecraft:carved_pumpkin"),
            new ResourceLocation("minecraft:cocoa_beans"),
            new ResourceLocation("minecraft:cactus"),
            new ResourceLocation("minecraft:bamboo"),
            new ResourceLocation("minecraft:kelp"),
            new ResourceLocation("minecraft:dried_kelp"),
            new ResourceLocation("minecraft:nether_wart"),
            new ResourceLocation("minecraft:apple"),
            new ResourceLocation("minecraft:chorus_fruit")
    );

    private static final Set<ResourceLocation> VANILLA_ORES = Set.of(
            new ResourceLocation("minecraft:coal"), new ResourceLocation("minecraft:coal_ore"),
            new ResourceLocation("minecraft:deepslate_coal_ore"), new ResourceLocation("minecraft:raw_coal"),
            new ResourceLocation("minecraft:iron_ingot"), new ResourceLocation("minecraft:iron_ore"),
            new ResourceLocation("minecraft:deepslate_iron_ore"), new ResourceLocation("minecraft:raw_iron"),
            new ResourceLocation("minecraft:copper_ingot"), new ResourceLocation("minecraft:copper_ore"),
            new ResourceLocation("minecraft:deepslate_copper_ore"), new ResourceLocation("minecraft:raw_copper"),
            new ResourceLocation("minecraft:gold_ingot"), new ResourceLocation("minecraft:gold_ore"),
            new ResourceLocation("minecraft:deepslate_gold_ore"), new ResourceLocation("minecraft:raw_gold"),
            new ResourceLocation("minecraft:redstone"), new ResourceLocation("minecraft:redstone_ore"),
            new ResourceLocation("minecraft:deepslate_redstone_ore"),
            new ResourceLocation("minecraft:lapis_lazuli"), new ResourceLocation("minecraft:lapis_ore"),
            new ResourceLocation("minecraft:deepslate_lapis_ore"),
            new ResourceLocation("minecraft:diamond"), new ResourceLocation("minecraft:diamond_ore"),
            new ResourceLocation("minecraft:deepslate_diamond_ore"),
            new ResourceLocation("minecraft:emerald"), new ResourceLocation("minecraft:emerald_ore"),
            new ResourceLocation("minecraft:deepslate_emerald_ore"),
            new ResourceLocation("minecraft:quartz"), new ResourceLocation("minecraft:nether_quartz_ore"),
            new ResourceLocation("minecraft:netherite_ingot")
    );

    /** Cheap bulk sand/gravel sold only in ore-themed shops. */
    private static final Set<ResourceLocation> ORE_SHOP_EXCLUSIVE = Set.of(
            new ResourceLocation("minecraft:cobblestone"),
            new ResourceLocation("minecraft:sand"),
            new ResourceLocation("minecraft:gravel")
    );

    private static final String[] DYE_COLOR_SUFFIXES = {
            "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
            "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
    };

    private static final String[] BUILD_LOG_PATHS = {
            "oak_log", "spruce_log", "birch_log", "jungle_log", "acacia_log", "dark_oak_log",
            "mangrove_log", "cherry_log", "pale_oak_log", "crimson_stem", "warped_stem",
            "stripped_oak_log", "stripped_spruce_log", "stripped_birch_log", "stripped_jungle_log",
            "stripped_acacia_log", "stripped_dark_oak_log", "stripped_mangrove_log", "stripped_cherry_log",
            "stripped_pale_oak_log", "stripped_crimson_stem", "stripped_warped_stem"
    };

    /** Bark-on-all-sides wood / hyphae (and stripped); priced from assembling recipes. */
    private static final String[] BUILD_WOOD_PATHS = {
            "oak_wood", "spruce_wood", "birch_wood", "jungle_wood", "acacia_wood", "dark_oak_wood",
            "mangrove_wood", "cherry_wood", "pale_oak_wood", "crimson_hyphae", "warped_hyphae",
            "stripped_oak_wood", "stripped_spruce_wood", "stripped_birch_wood", "stripped_jungle_wood",
            "stripped_acacia_wood", "stripped_dark_oak_wood", "stripped_mangrove_wood", "stripped_cherry_wood",
            "stripped_pale_oak_wood", "stripped_crimson_hyphae", "stripped_warped_hyphae"
    };

    private static final String[] BUILD_CORAL_BLOCK_PATHS = {
            "tube_coral_block", "brain_coral_block", "bubble_coral_block", "fire_coral_block", "horn_coral_block",
            "dead_tube_coral_block", "dead_brain_coral_block", "dead_bubble_coral_block",
            "dead_fire_coral_block", "dead_horn_coral_block"
    };

    private static final String[] BUILD_FROGLIGHT_PATHS = {
            "ochre_froglight", "verdant_froglight", "pearlescent_froglight"
    };

    private static final Set<ResourceLocation> BUILD_SHOP_EXCLUSIVE = buildShopExclusiveSet();
    /** Bark wood / hyphae priced from recipes (subset of build exclusives). */
    private static final Set<ResourceLocation> BUILD_WOOD_CRAFT_PRICED = buildWoodCraftPricedSet();
    private static final Set<ResourceLocation> REDSTONE_SHOP_EXCLUSIVE = redstoneShopExclusiveSet();
    private static final Set<ResourceLocation> WEAPON_SHOP_EXCLUSIVE = weaponShopExclusiveSet();
    private static final Set<ResourceLocation> ARMOR_SHOP_EXCLUSIVE = armorShopExclusiveSet();
    private static final Set<ResourceLocation> FURNITURE_VANILLA_WHITELIST = furnitureVanillaWhitelist();
    /** Soft-compat furniture mods: Another Furniture, Refurbished CFM, classic CFM, Handcrafted. */
    private static final Set<String> FURNITURE_MOD_NAMESPACES = Set.of(
            "another_furniture", "refurbished_furniture", "cfm", "handcrafted"
    );
    /** Build/redstone exclusives that also stock the furniture shop. */
    private static final Set<ResourceLocation> FURNITURE_DUAL_LISTED = Set.of(
            new ResourceLocation("minecraft:campfire"),
            new ResourceLocation("minecraft:lantern"),
            new ResourceLocation("minecraft:note_block"),
            new ResourceLocation("minecraft:jukebox")
    );
    /** Farm-tool shop exclusives: vanilla hoes/shovels, shears, lead, plus soft-compat farm supplies. */
    private static final Set<ResourceLocation> FARM_TOOL_SHOP_EXCLUSIVE = farmToolShopExclusiveSet();
    /** Grocery/furniture items the farm-tool shop also stocks. */
    private static final Set<ResourceLocation> FARM_TOOL_DUAL_LISTED = farmToolDualListedSet();
    /** Farm-tool items priced like a sibling (rich soil has no recipe; it comes from organic compost in-world). */
    private static final Map<ResourceLocation, ResourceLocation> FARM_TOOL_PRICE_MIRROR = Map.of(
            new ResourceLocation("farmersdelight:rich_soil"),
            new ResourceLocation("farmersdelight:organic_compost"));
    /** Grocery items priced from assembling recipes (cost + 1), like redstone parts. */
    private static final Set<ResourceLocation> GROCERY_CRAFT_PRICED = Set.of(
            new ResourceLocation("minecraft:arrow"),
            new ResourceLocation("minecraft:firework_rocket")
    );
    /** Ore-shelf dust/block that also stock the redstone shop. */
    private static final Set<ResourceLocation> REDSTONE_DUAL_LISTED = Set.of(
            new ResourceLocation("minecraft:redstone"),
            new ResourceLocation("minecraft:redstone_block")
    );

    private static final Map<String, String> ORE_BLOCKS = Map.ofEntries(
            Map.entry("coal_block", "coal"),
            Map.entry("iron_block", "iron_ingot"),
            Map.entry("gold_block", "gold_ingot"),
            Map.entry("diamond_block", "diamond"),
            Map.entry("lapis_block", "lapis_lazuli"),
            Map.entry("redstone_block", "redstone"),
            Map.entry("copper_block", "copper_ingot"),
            Map.entry("emerald_block", "emerald"),
            Map.entry("quartz_block", "quartz"),
            Map.entry("netherite_block", "netherite_ingot"),
            Map.entry("raw_iron_block", "raw_iron"),
            Map.entry("raw_gold_block", "raw_gold"),
            Map.entry("raw_copper_block", "raw_copper")
    );

    /** Catalog materials used only for recipe costing — never sold in shops. */
    private static final Set<ResourceLocation> TRADE_DENY = Set.of(
            new ResourceLocation("minecraft:milk_bucket"),
            new ResourceLocation("minecraft:turtle_egg"),
            new ResourceLocation("minecraft:sniffer_egg"),
            new ResourceLocation("minecraft:frogspawn"),
            new ResourceLocation("minecraft:flint"),
            new ResourceLocation("minecraft:stick")
    );

    private static final Set<ResourceLocation> FOOD_BLACKLIST = Set.of(
            new ResourceLocation("minecraft:golden_apple"),
            new ResourceLocation("minecraft:enchanted_golden_apple"),
            new ResourceLocation("minecraft:suspicious_stew")
    );

    private static final Map<ResourceLocation, Integer> BASE_PRICES = loadBasePrices();
    private static final Map<ResourceLocation, int[]> LISTED_BANDS = listedBands();

    private PriceCatalog() {
    }

    public static boolean isTradeable(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) {
            return false;
        }
        if (TRADE_DENY.contains(id) || id.getPath().equals("ancient_debris")) {
            return false;
        }
        if (FOOD_BLACKLIST.contains(id)) {
            return false;
        }
        if (EatProperlyTradeFilter.isTokenFood(id.getNamespace(), stack.getItem().getFoodProperties() != null)) {
            return false;
        }
        if (ORE_BLOCKS.containsKey(id.getPath())) {
            return true;
        }
        if (isAnimalDrop(id)) {
            return true;
        }
        if (EDGE_FARM.contains(id) || VANILLA_ORES.contains(id) || ORE_SHOP_EXCLUSIVE.contains(id)
                || BUILD_SHOP_EXCLUSIVE.contains(id) || REDSTONE_SHOP_EXCLUSIVE.contains(id)
                || WEAPON_SHOP_EXCLUSIVE.contains(id) || ARMOR_SHOP_EXCLUSIVE.contains(id)
                || FARM_TOOL_SHOP_EXCLUSIVE.contains(id)
                || GROCERY_CRAFT_PRICED.contains(id)
                || isFlowerCatalogItem(stack.getItem())
                || isFurnitureCatalogItem(stack.getItem())
                || BASE_PRICES.containsKey(id)) {
            return true;
        }
        Item item = stack.getItem();
        FoodProperties food = item.getFoodProperties();
        if (food != null) {
            return true;
        }
        return inTag(id, new ResourceLocation("forge", "crops"))
                || inTag(id, new ResourceLocation("forge", "seeds"))
                || inTag(id, new ResourceLocation("forge", "tea_leaves"))
                || isSeed(id);
    }

    public static ItemShelfKind shelfKind(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) {
            return ItemShelfKind.GROCERY;
        }
        if (BUILD_SHOP_EXCLUSIVE.contains(id)) {
            return ItemShelfKind.BUILD;
        }
        if (WEAPON_SHOP_EXCLUSIVE.contains(id)) {
            return ItemShelfKind.WEAPON;
        }
        if (ARMOR_SHOP_EXCLUSIVE.contains(id)) {
            return ItemShelfKind.ARMOR;
        }
        if (FARM_TOOL_SHOP_EXCLUSIVE.contains(id)) {
            return ItemShelfKind.FARM_TOOL;
        }
        if (REDSTONE_SHOP_EXCLUSIVE.contains(id)) {
            return ItemShelfKind.REDSTONE;
        }
        if (isFurnitureShopExclusive(item)) {
            return ItemShelfKind.FURNITURE;
        }
        if (isFlowerCatalogItem(item)) {
            return ItemShelfKind.FLOWER;
        }
        if (ORE_BLOCKS.containsKey(id.getPath()) || VANILLA_ORES.contains(id) || ORE_SHOP_EXCLUSIVE.contains(id)) {
            return ItemShelfKind.ORE;
        }
        if (isSeed(id) || EDGE_FARM.contains(id)
                || inTag(id, new ResourceLocation("forge", "crops"))
                || inTag(id, new ResourceLocation("forge", "seeds"))
                || inTag(id, new ResourceLocation("forge", "tea_leaves"))
                || isEggOrWool(id)
                || isFarmExtra(id)) {
            return ItemShelfKind.FARM;
        }
        if (item.isEdible() && !FOOD_BLACKLIST.contains(id)) {
            return ItemShelfKind.FOOD;
        }
        return ItemShelfKind.GROCERY;
    }

    public static boolean isOreShopExclusive(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return id != null && ORE_SHOP_EXCLUSIVE.contains(id);
    }

    public static boolean isBuildShopExclusive(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return id != null && BUILD_SHOP_EXCLUSIVE.contains(id);
    }

    /** Flowers / mod plants in {@code #minecraft:flowers}, empty pots, plus cave/water decor extras — not {@code potted_*}. */
    public static boolean isFlowerCatalogItem(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) {
            return false;
        }
        if (id.getPath().equals("flower_pot") || FLOWER_SHOP_EXTRA.contains(id) || FLOWER_SHOP_LEAVES.contains(id)) {
            return true;
        }
        return inTag(id, MINECRAFT_FLOWERS);
    }

    public static boolean isFlowerShopExclusive(Item item) {
        return isFlowerCatalogItem(item);
    }

    public static boolean isRedstoneShopExclusive(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return id != null && REDSTONE_SHOP_EXCLUSIVE.contains(id);
    }

    public static boolean isWeaponShopExclusive(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return id != null && WEAPON_SHOP_EXCLUSIVE.contains(id);
    }

    public static boolean isArmorShopExclusive(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return id != null && ARMOR_SHOP_EXCLUSIVE.contains(id);
    }

    /** Farm tools (hoes/shovels/shears/lead) plus soft-compat farm supplies. */
    public static boolean isFarmToolCatalogItem(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return id != null && FARM_TOOL_SHOP_EXCLUSIVE.contains(id);
    }

    public static boolean isFarmToolShopExclusive(Item item) {
        return isFarmToolCatalogItem(item);
    }

    /** Bone meal / composter: their own shop keeps them, and the farm-tool shop also stocks them. */
    public static boolean isFarmToolDualListed(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return id != null && FARM_TOOL_DUAL_LISTED.contains(id);
    }

    public static boolean isFurnitureModNamespace(ResourceLocation id) {
        return id != null && FURNITURE_MOD_NAMESPACES.contains(id.getNamespace());
    }

    public static boolean isFurnitureDualListed(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return id != null && FURNITURE_DUAL_LISTED.contains(id);
    }

    /** Vanilla whitelist + soft-compat furniture mod items + dual-listed home accents. */
    public static boolean isFurnitureCatalogItem(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) {
            return false;
        }
        if (FURNITURE_DUAL_LISTED.contains(id) || FURNITURE_VANILLA_WHITELIST.contains(id)) {
            return true;
        }
        return isFurnitureModNamespace(id);
    }

    /** Specialty furniture shelf only (not dual-listed build/redstone accents). */
    public static boolean isFurnitureShopExclusive(Item item) {
        return isFurnitureCatalogItem(item) && !isFurnitureDualListed(item);
    }

    /** Redstone dust / block: ore shops keep them, and the redstone shop also stocks them. */
    public static boolean isRedstoneDualListed(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return id != null && REDSTONE_DUAL_LISTED.contains(id);
    }

    public static boolean isSpecialtyShopExclusive(Item item) {
        return isOreShopExclusive(item) || isBuildShopExclusive(item) || isFlowerShopExclusive(item)
                || isRedstoneShopExclusive(item) || isWeaponShopExclusive(item) || isArmorShopExclusive(item)
                || isFarmToolShopExclusive(item)
                || isFurnitureShopExclusive(item);
    }

    public static int getBasePrice(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null || !isTradeable(new ItemStack(item))) {
            return 0;
        }
        int raw = computeRawBase(item, id);
        if (raw <= 0) {
            return 0;
        }
        raw = raiseToCraftFloor(item, raw);
        return clampMarketPrice(item, raw);
    }

    /**
     * Fast path for tag slots: listed catalog price only (no craft recursion).
     * Returns 0 when the item would need recipe expansion.
     */
    static int listedBaseOnly(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) {
            return 0;
        }
        if (BASE_PRICES.containsKey(id)) {
            return BASE_PRICES.get(id);
        }
        if (item.isEdible() && item.getFoodProperties() != null && !FOOD_BLACKLIST.contains(id)) {
            return FoodRecipeIndex.cachedFoodPriceOrZero(item);
        }
        if (ORE_BLOCKS.containsKey(id.getPath())) {
            Item unit = BuiltInRegistries.ITEM.get(new ResourceLocation("minecraft", ORE_BLOCKS.get(id.getPath())));
            int unitPrice = listedBaseOnly(unit);
            return unitPrice > 0 ? unitPrice * 9 + 1 : 0;
        }
        if (isWool(id)) {
            return BASE_PRICES.getOrDefault(new ResourceLocation("minecraft:white_wool"), 8);
        }
        if (isSeed(id)) {
            return DEFAULT_SEED_PRICE;
        }
        if (inTag(id, new ResourceLocation("forge", "crops")) || EDGE_FARM.contains(id)) {
            return DEFAULT_CROP_PRICE;
        }
        if (VANILLA_ORES.contains(id)) {
            return oreFallback(id.getPath());
        }
        if (isFlowerCatalogItem(item)) {
            return flowerFallback(item, id);
        }
        return 0;
    }

    /**
     * Non-food catalog price used as recipe ingredients (no food formula).
     */
    public static int materialBasePrice(Item item) {
        return materialBasePrice(item, null, null);
    }

    /**
     * Nested material price used while resolving recipes; preserves cycle detection.
     */
    static int materialBasePrice(Item item, Set<Item> visiting, Set<String> visitingFluids) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) {
            return 0;
        }
        return Math.max(0, computeRawBase(item, id, visiting, visitingFluids));
    }

    private static int computeRawBase(Item item, ResourceLocation id) {
        return computeRawBase(item, id, null, null);
    }

    private static int computeRawBase(Item item, ResourceLocation id, Set<Item> visiting, Set<String> visitingFluids) {
        if (BASE_PRICES.containsKey(id)) {
            return BASE_PRICES.get(id);
        }
        if (item.isEdible() && item.getFoodProperties() != null && !FOOD_BLACKLIST.contains(id)) {
            return visiting != null
                    ? FoodRecipeIndex.priceFor(item, visiting, visitingFluids)
                    : FoodRecipeIndex.priceFor(item);
        }
        if (ORE_BLOCKS.containsKey(id.getPath())) {
            Item unit = BuiltInRegistries.ITEM.get(new ResourceLocation("minecraft", ORE_BLOCKS.get(id.getPath())));
            int unitPrice = materialBasePrice(unit, visiting, visitingFluids);
            if (unitPrice <= 0) {
                return 0;
            }
            int recipeCost = craftCostFor(item, visiting, visitingFluids);
            int materials = recipeCost > 0 ? recipeCost : unitPrice * 9;
            return materials + 1;
        }
        if (BASE_PRICES.containsKey(id)) {
            return BASE_PRICES.get(id);
        }
        if (isWool(id)) {
            return BASE_PRICES.getOrDefault(new ResourceLocation("minecraft:white_wool"), 8);
        }
        if (isSeed(id)) {
            return DEFAULT_SEED_PRICE;
        }
        if (inTag(id, new ResourceLocation("forge", "crops")) || EDGE_FARM.contains(id)) {
            return DEFAULT_CROP_PRICE;
        }
        if (VANILLA_ORES.contains(id)) {
            return oreFallback(id.getPath());
        }
        if (isFlowerCatalogItem(item)) {
            return flowerFallback(item, id);
        }
        if (REDSTONE_SHOP_EXCLUSIVE.contains(id) || GROCERY_CRAFT_PRICED.contains(id)) {
            int craft = craftCostFor(item, visiting, visitingFluids);
            return craft > 0 ? craft + 1 : 0;
        }
        if (isBuildCraftPricedWood(id)) {
            int craft = craftCostFor(item, visiting, visitingFluids);
            return craft > 0 ? craft + 1 : 0;
        }
        if (WEAPON_SHOP_EXCLUSIVE.contains(id) || ARMOR_SHOP_EXCLUSIVE.contains(id)) {
            int craft = craftCostFor(item, visiting, visitingFluids);
            return craft > 0 ? craft + 1 : 0;
        }
        if (FARM_TOOL_SHOP_EXCLUSIVE.contains(id)) {
            int[] band = LISTED_BANDS.get(id);
            if (band != null && band.length >= 2) {
                return Math.max(1, (band[0] + band[1]) / 2);
            }
            ResourceLocation mirror = FARM_TOOL_PRICE_MIRROR.get(id);
            if (mirror != null) {
                Item other = BuiltInRegistries.ITEM.get(mirror);
                if (other != null) {
                    int mirrored = materialBasePrice(other, visiting, visitingFluids);
                    if (mirrored > 0) {
                        return mirrored;
                    }
                }
            }
            int craft = craftCostFor(item, visiting, visitingFluids);
            return craft > 0 ? craft + 1 : 0;
        }
        if (isFurnitureShopExclusive(item)) {
            int craft = craftCostFor(item, visiting, visitingFluids);
            return craft > 0 ? craft + 1 : 0;
        }
        // Build exclusives with listed bands but missing JSON still get a mid-band base
        // (collectTradeableItems requires getBasePrice > 0).
        if (BUILD_SHOP_EXCLUSIVE.contains(id)) {
            int[] band = LISTED_BANDS.get(id);
            if (band != null && band.length >= 2) {
                return Math.max(1, (band[0] + band[1]) / 2);
            }
        }
        return 0;
    }

    private static boolean isBuildCraftPricedWood(ResourceLocation id) {
        return id != null && BUILD_WOOD_CRAFT_PRICED.contains(id);
    }

    private static Set<ResourceLocation> buildWoodCraftPricedSet() {
        java.util.HashSet<ResourceLocation> set = new java.util.HashSet<>();
        for (String wood : BUILD_WOOD_PATHS) {
            set.add(new ResourceLocation("minecraft", wood));
        }
        return Set.copyOf(set);
    }

    private static int craftCostFor(Item item, Set<Item> visiting, Set<String> visitingFluids) {
        if (visiting != null) {
            return FoodRecipeIndex.cheapestCraftCostPerResult(item, visiting, visitingFluids);
        }
        return FoodRecipeIndex.cheapestCraftCostPerResult(item);
    }

    public static int clampMarketPrice(Item item, int price) {
        int[] band = marketBand(item);
        if (band == null) {
            return 0;
        }
        return raiseToCraftFloor(item, PriceMath.clampRange(price, band[0], band[1]));
    }

    /**
     * Listed catalog / seed / flower prices are intentional and must not be lifted by recipe cost
     * (clay→terracotta would otherwise inflate a 1–3 band to ~40).
     */
    public static boolean ignoresCraftFloor(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) {
            return false;
        }
        return LISTED_BANDS.containsKey(id) || BASE_PRICES.containsKey(id) || isSeed(id)
                || isFlowerCatalogItem(item);
    }

    public static int raiseToCraftFloor(Item item, int price) {
        if (ignoresCraftFloor(item)) {
            return Math.max(1, price);
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        int floor = FoodRecipeIndex.cheapestCraftCostPerResult(item);
        if (floor <= 0 && isOreBlock(item) && id != null) {
            Item unit = BuiltInRegistries.ITEM.get(new ResourceLocation("minecraft", ORE_BLOCKS.get(id.getPath())));
            int unitPrice = materialBasePrice(unit);
            floor = unitPrice > 0 ? unitPrice * 9 : 0;
        }
        if (floor <= 0) {
            return Math.max(1, price);
        }
        return Math.max(price, floor + 1);
    }

    private static boolean isOreBlock(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return id != null && ORE_BLOCKS.containsKey(id.getPath());
    }

    /**
     * Catalog band for an item, or {@code null} when its price cannot be derived.
     * Unpriced items are skipped everywhere: no shelf row, no listing, no live band.
     */
    public static int[] marketBand(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) {
            return null;
        }
        int[] listed = LISTED_BANDS.get(id);
        if (listed != null) {
            return listed;
        }
        return switch (id.getPath()) {
            case "netherite_ingot" -> new int[]{14000, 15000};
            case "diamond", "diamond_ore", "deepslate_diamond_ore" -> new int[]{200, 250};
            case "gold_ingot" -> new int[]{100, 130};
            case "raw_gold", "gold_ore", "deepslate_gold_ore" -> new int[]{70, 95};
            case "iron_ingot" -> new int[]{30, 35};
            case "raw_iron", "iron_ore", "deepslate_iron_ore" -> new int[]{18, 24};
            case "coal", "coal_ore", "deepslate_coal_ore" -> new int[]{10, 15};
            case "lapis_lazuli", "lapis_ore", "deepslate_lapis_ore" -> new int[]{20, 25};
            case "redstone", "redstone_ore", "deepslate_redstone_ore" -> new int[]{20, 25};
            case "copper_ingot" -> new int[]{30, 40};
            case "raw_copper", "copper_ore", "deepslate_copper_ore" -> new int[]{18, 28};
            case "emerald", "emerald_ore", "deepslate_emerald_ore" -> new int[]{200, 300};
            case "quartz", "nether_quartz_ore" -> new int[]{3, 5};
            default -> categoryBand(id, item);
        };
    }

    private static int[] categoryBand(ResourceLocation id, Item item) {
        if (ORE_BLOCKS.containsKey(id.getPath())) {
            Item unit = BuiltInRegistries.ITEM.get(new ResourceLocation("minecraft", ORE_BLOCKS.get(id.getPath())));
            int[] inner = marketBand(unit);
            int lo = inner[0] * 9 + 1;
            int hi = inner[1] * 9 + Math.max(1, inner[1]);
            return new int[]{lo, Math.max(lo, hi)};
        }
        // Catalog / crop / seed bands beat edible food formula (e.g. melon_slice is food but priced as crop).
        if (BASE_PRICES.containsKey(id) || EDGE_FARM.contains(id)
                || inTag(id, new ResourceLocation("forge", "crops"))
                || inTag(id, new ResourceLocation("forge", "seeds"))
                || isSeed(id)) {
            if (isSeed(id) || inTag(id, new ResourceLocation("forge", "seeds"))) {
                return new int[]{SEED_MIN, SEED_MAX};
            }
            int base = Math.max(1, BASE_PRICES.getOrDefault(id, DEFAULT_CROP_PRICE));
            return new int[]{CROP_MIN, Math.max(CROP_MAX, base)};
        }
        if (item != null && item.isEdible() && !FOOD_BLACKLIST.contains(id)) {
            int base = Math.max(1, FoodRecipeIndex.priceFor(item));
            int craft = FoodRecipeIndex.cheapestCraftCostPerResult(item);
            int floor = craft > 0 ? craft + 1 : 1;
            int lo = Math.max(floor, (int) Math.floor(base * 0.75D));
            int hi = Math.max(base, (int) Math.ceil(base * 1.15D));
            return new int[]{Math.max(1, lo), Math.max(lo, hi)};
        }
        if (isWool(id)) {
            return new int[]{5, 12};
        }
        if (isAnimalDrop(id)) {
            int base = Math.max(1, computeRawBase(item, id));
            return new int[]{Math.max(1, base / 2), Math.max(base, base * 2)};
        }
        if (isFlowerCatalogItem(item)) {
            return flowerBand(item, id);
        }
        if (REDSTONE_SHOP_EXCLUSIVE.contains(id) || GROCERY_CRAFT_PRICED.contains(id)
                || WEAPON_SHOP_EXCLUSIVE.contains(id) || ARMOR_SHOP_EXCLUSIVE.contains(id)
                || isBuildCraftPricedWood(id)) {
            int craft = FoodRecipeIndex.cheapestCraftCostPerResult(item);
            if (craft <= 0) {
                return null;
            }
            int base = craft + 1;
            int lo = Math.max(1, (int) Math.floor(base * 0.75D));
            int hi = Math.max(base, (int) Math.ceil(base * 1.15D));
            return new int[]{lo, Math.max(lo, hi)};
        }
        if (FARM_TOOL_SHOP_EXCLUSIVE.contains(id)) {
            ResourceLocation mirror = FARM_TOOL_PRICE_MIRROR.get(id);
            if (mirror != null) {
                Item other = BuiltInRegistries.ITEM.get(mirror);
                if (other != null) {
                    return categoryBand(mirror, other);
                }
            }
            int craft = FoodRecipeIndex.cheapestCraftCostPerResult(item);
            if (craft <= 0) {
                return null;
            }
            int base = craft + 1;
            int lo = Math.max(1, (int) Math.floor(base * 0.75D));
            int hi = Math.max(base, (int) Math.ceil(base * 1.15D));
            return new int[]{lo, Math.max(lo, hi)};
        }
        if (isFurnitureShopExclusive(item)) {
            int craft = FoodRecipeIndex.cheapestCraftCostPerResult(item);
            if (craft <= 0) {
                return null;
            }
            int base = craft + 1;
            int lo = Math.max(1, (int) Math.floor(base * 0.75D));
            int hi = Math.max(base, (int) Math.ceil(base * 1.15D));
            return new int[]{lo, Math.max(lo, hi)};
        }
        return null;
    }

    private static int flowerFallback(Item item, ResourceLocation id) {
        String path = id.getPath();
        if (path.equals("wither_rose")) {
            return WITHER_ROSE_PRICE;
        }
        if (path.equals("flower_pot")) {
            return DEFAULT_FLOWER_POT_PRICE;
        }
        if (inTag(id, MINECRAFT_TALL_FLOWERS)) {
            return DEFAULT_TALL_FLOWER_PRICE;
        }
        if (inTag(id, MINECRAFT_SMALL_FLOWERS)) {
            return DEFAULT_SMALL_FLOWER_PRICE;
        }
        return DEFAULT_FLOWER_EXTRA_PRICE;
    }

    private static int[] flowerBand(Item item, ResourceLocation id) {
        int base = Math.max(1, flowerFallback(item, id));
        if (id.getPath().equals("wither_rose")) {
            return new int[]{12, 20};
        }
        if (id.getPath().equals("flower_pot")) {
            return new int[]{3, 6};
        }
        if (inTag(id, MINECRAFT_TALL_FLOWERS)) {
            return new int[]{8, 14};
        }
        if (inTag(id, MINECRAFT_SMALL_FLOWERS)) {
            return new int[]{4, 8};
        }
        return new int[]{Math.max(4, base - 2), Math.max(base + 2, base * 2)};
    }

    private static int oreFallback(String path) {
        return switch (path) {
            case "coal", "coal_ore", "deepslate_coal_ore" -> 12;
            case "iron_ingot" -> 32;
            case "raw_iron", "iron_ore", "deepslate_iron_ore" -> 21;
            case "gold_ingot" -> 115;
            case "raw_gold", "gold_ore", "deepslate_gold_ore" -> 82;
            case "redstone", "redstone_ore", "deepslate_redstone_ore" -> 22;
            case "lapis_lazuli", "lapis_ore", "deepslate_lapis_ore" -> 22;
            case "diamond", "diamond_ore", "deepslate_diamond_ore" -> 225;
            case "netherite_ingot" -> 6250;
            case "copper_ingot" -> 35;
            case "raw_copper", "copper_ore", "deepslate_copper_ore" -> 22;
            case "emerald", "emerald_ore", "deepslate_emerald_ore" -> 250;
            case "quartz", "nether_quartz_ore" -> 4;
            default -> 20;
        };
    }

    public static OptionalInt tryGetMarketPrice(MarketData market, Item item) {
        if (!isTradeable(new ItemStack(item))) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(market.getReferencePrice(item));
    }

    public static void invalidateTradeableCache() {
        tradeableCache = null;
        ShopOffers.invalidate();
    }

    public static void restoreTradeable(java.util.Collection<Item> items) {
        tradeableCache = Collections.unmodifiableList(new ArrayList<>(items));
    }

    public static List<Item> cachedTradeableOrEmpty() {
        return tradeableCache == null ? List.of() : tradeableCache;
    }

    public static void warmupTradeableItems() {
        collectTradeableItems();
    }

    public static List<Item> collectTradeableItems() {
        if (tradeableCache != null) {
            return tradeableCache;
        }
        ArrayList<Item> list = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (!isTradeable(new ItemStack(item))) {
                continue;
            }
            if (getBasePrice(item) > 0) {
                list.add(item);
                continue;
            }
            // No fallback: an item whose price cannot be derived never reaches any shelf.
            if (isSpecialtyShopExclusive(item)) {
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                if (id != null && skipped.size() < 40) {
                    skipped.add(id.toString());
                }
            }
        }
        if (!skipped.isEmpty()) {
            LOGGER.info("[{}] skipped {} specialty items with no resolvable price: {}",
                    NetShopMod.MOD_ID, skipped.size(), skipped);
        }
        tradeableCache = Collections.unmodifiableList(list);
        return tradeableCache;
    }

    public static int getBuyPrice(MarketData market, Item item) {
        double mul = 1.15D;
        try {
            mul = SellVegConfig.BUY_PRICE_MULTIPLIER.get();
        } catch (Exception ignored) {
        }
        int buy = (int) Math.round(market.getReferencePrice(item) * mul);
        return raiseToCraftFloor(item, buy);
    }

    private static boolean isSeed(ResourceLocation id) {
        if (id == null) {
            return false;
        }
        if (inTag(id, new ResourceLocation("forge", "seeds"))) {
            return true;
        }
        return "minecraft".equals(id.getNamespace()) && id.getPath().endsWith("_seeds");
    }

    private static boolean isFarmExtra(ResourceLocation id) {
        if (id == null) {
            return false;
        }
        String path = id.getPath();
        return path.equals("hay_block")
                || path.equals("dried_kelp_block")
                || path.equals("honeycomb")
                || path.equals("honey_block")
                || path.equals("brown_mushroom")
                || path.equals("red_mushroom")
                || path.equals("rabbit_hide")
                || path.equals("rabbit_foot")
                || path.equals("scute")
                || path.equals("rice_panicle")
                || path.equals("rice_bag")
                || path.equals("rice_bale")
                || path.equals("pods")
                || path.equals("pumpkin_slice")
                || path.equals("cantaloupe")
                || path.equals("cantaloupe_slice")
                || path.endsWith("_tea_leaves")
                || path.equals("tea_leaves")
                || path.endsWith("_crate")
                || path.endsWith("_bag");
    }

    private static boolean isAnimalDrop(ResourceLocation id) {
        if (id == null) {
            return false;
        }
        if (isWool(id) || isEggOrWool(id)) {
            return true;
        }
        return "minecraft".equals(id.getNamespace())
                && (id.getPath().equals("feather") || id.getPath().equals("leather"));
    }

    private static boolean isEggOrWool(ResourceLocation id) {
        return id != null && "minecraft".equals(id.getNamespace())
                && (id.getPath().equals("egg") || isWool(id));
    }

    private static boolean isWool(ResourceLocation id) {
        return id != null && "minecraft".equals(id.getNamespace())
                && (id.getPath().equals("wool") || id.getPath().endsWith("_wool"));
    }

    private static boolean inTag(ResourceLocation id, ResourceLocation tagId) {
        Item item = BuiltInRegistries.ITEM.get(id);
        if (item == null) {
            return false;
        }
        TagKey<Item> key = TagKey.create(ForgeRegistries.ITEMS.getRegistryKey(), tagId);
        return item.builtInRegistryHolder().is(key);
    }

    private static Map<ResourceLocation, Integer> loadBasePrices() {
        Map<ResourceLocation, Integer> map = new java.util.HashMap<>();
        map.put(new ResourceLocation("minecraft:wheat"), WHEAT_ANCHOR_PRICE);
        map.put(new ResourceLocation("minecraft:diamond"), 225);

        var stream = PriceCatalog.class.getResourceAsStream("/data/homestead_sell_veg/price_catalog/default.json");
        if (stream == null) {
            return map;
        }
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (root.has("items")) {
                for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("items").entrySet()) {
                    map.put(new ResourceLocation(entry.getKey()), entry.getValue().getAsInt());
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("[{}] Failed loading price catalog JSON: {}", NetShopMod.MOD_ID, ex.toString());
        }
        return map;
    }

    private static Map<ResourceLocation, int[]> listedBands() {
        Map<ResourceLocation, int[]> bands = new java.util.HashMap<>();
        bands.put(new ResourceLocation("minecraft:rabbit_hide"), new int[]{20, 30});
        bands.put(new ResourceLocation("minecraft:rabbit_foot"), new int[]{30, 40});
        bands.put(new ResourceLocation("farmersdelight:milk_bottle"), new int[]{8, 14});
        bands.put(new ResourceLocation("minecraft:bowl"), new int[]{3, 6});
        bands.put(new ResourceLocation("minecraft:glass_bottle"), new int[]{4, 8});
        bands.put(new ResourceLocation("minecraft:honeycomb"), new int[]{10, 20});
        bands.put(new ResourceLocation("minecraft:honey_bottle"), new int[]{12, 26});
        bands.put(new ResourceLocation("minecraft:honey_block"), new int[]{40, 75});
        bands.put(new ResourceLocation("minecraft:quartz"), new int[]{3, 5});
        bands.put(new ResourceLocation("minecraft:nether_quartz_ore"), new int[]{3, 5});
        bands.put(new ResourceLocation("minecraft:quartz_block"), new int[]{28, 45});
        bands.put(new ResourceLocation("minecraft:hay_block"), new int[]{70, 110});
        bands.put(new ResourceLocation("minecraft:melon"), new int[]{8, 14});
        bands.put(new ResourceLocation("minecraft:melon_slice"), new int[]{3, 6});
        bands.put(new ResourceLocation("delightful:cantaloupe"), new int[]{8, 14});
        bands.put(new ResourceLocation("delightful:cantaloupe_slice"), new int[]{3, 6});
        bands.put(new ResourceLocation("delightful:cantaloupe_seeds"), new int[]{SEED_MIN, SEED_MAX});
        bands.put(new ResourceLocation("farmersdelight:pumpkin_slice"), new int[]{2, 5});
        bands.put(new ResourceLocation("farmersdelight:tomato_seeds"), new int[]{SEED_MIN, SEED_MAX});
        bands.put(new ResourceLocation("farmersdelight:cabbage_seeds"), new int[]{SEED_MIN, SEED_MAX});
        bands.put(new ResourceLocation("youkaishomecoming:tea_seeds"), new int[]{SEED_MIN, SEED_MAX});
        bands.put(new ResourceLocation("youkaishomecoming:pods"), new int[]{5, 10});
        bands.put(new ResourceLocation("youkaishomecoming:soybean"), new int[]{5, 10});
        bands.put(new ResourceLocation("youkaishomecoming:tea_leaves"), new int[]{8, 14});
        bands.put(new ResourceLocation("youkaishomecoming:green_tea_leaves"), new int[]{10, 16});
        bands.put(new ResourceLocation("youkaishomecoming:black_tea_leaves"), new int[]{12, 20});
        bands.put(new ResourceLocation("youkaishomecoming:oolong_tea_leaves"), new int[]{12, 20});
        bands.put(new ResourceLocation("youkaishomecoming:white_tea_leaves"), new int[]{12, 20});
        bands.put(new ResourceLocation("youkaishomecoming:dark_tea_leaves"), new int[]{12, 20});
        bands.put(new ResourceLocation("youkaishomecoming:yellow_tea_leaves"), new int[]{12, 20});
        bands.put(new ResourceLocation("farmersdelight:rice_panicle"), new int[]{5, 10});
        bands.put(new ResourceLocation("farmersdelight:rice"), new int[]{5, 10});
        bands.put(new ResourceLocation("farmersdelight:tomato"), new int[]{5, 10});
        bands.put(new ResourceLocation("farmersdelight:onion"), new int[]{5, 10});
        bands.put(new ResourceLocation("farmersdelight:cabbage"), new int[]{5, 10});
        bands.put(new ResourceLocation("farmersdelight:potato_crate"), new int[]{55, 90});
        bands.put(new ResourceLocation("farmersdelight:carrot_crate"), new int[]{55, 90});
        bands.put(new ResourceLocation("farmersdelight:beetroot_crate"), new int[]{48, 80});
        bands.put(new ResourceLocation("farmersdelight:tomato_crate"), new int[]{55, 90});
        bands.put(new ResourceLocation("farmersdelight:onion_crate"), new int[]{55, 90});
        bands.put(new ResourceLocation("farmersdelight:cabbage_crate"), new int[]{55, 90});
        bands.put(new ResourceLocation("farmersdelight:rice_bag"), new int[]{55, 90});
        bands.put(new ResourceLocation("farmersdelight:rice_bale"), new int[]{55, 90});
        bands.put(new ResourceLocation("farmersdelight:straw"), new int[]{2, 4});
        bands.put(new ResourceLocation("youkaishomecoming:pod_crate"), new int[]{55, 90});
        bands.put(new ResourceLocation("youkaishomecoming:cucumber_crate"), new int[]{55, 90});
        bands.put(new ResourceLocation("youkaishomecoming:red_grape_crate"), new int[]{55, 90});
        bands.put(new ResourceLocation("youkaishomecoming:white_grape_crate"), new int[]{55, 90});
        bands.put(new ResourceLocation("youkaishomecoming:black_grape_crate"), new int[]{55, 90});
        bands.put(new ResourceLocation("youkaishomecoming:soybean_bag"), new int[]{55, 90});
        bands.put(new ResourceLocation("youkaishomecoming:redbean_bag"), new int[]{55, 90});
        bands.put(new ResourceLocation("youkaishomecoming:coffee_bean_bag"), new int[]{55, 90});
        bands.put(new ResourceLocation("youkaishomecoming:tea_leaf_bag"), new int[]{68, 113});
        bands.put(new ResourceLocation("youkaishomecoming:green_tea_bag"), new int[]{81, 135});
        bands.put(new ResourceLocation("youkaishomecoming:black_tea_bag"), new int[]{101, 169});
        bands.put(new ResourceLocation("youkaishomecoming:oolong_tea_bag"), new int[]{101, 169});
        bands.put(new ResourceLocation("youkaishomecoming:white_tea_bag"), new int[]{101, 169});
        bands.put(new ResourceLocation("youkaishomecoming:dark_tea_bag"), new int[]{101, 169});
        bands.put(new ResourceLocation("youkaishomecoming:yellow_tea_bag"), new int[]{101, 169});
        bands.put(new ResourceLocation("minecraft:dried_kelp_block"), new int[]{40, 70});
        bands.put(new ResourceLocation("minecraft:bone"), new int[]{1, 4});
        bands.put(new ResourceLocation("minecraft:bone_meal"), new int[]{3, 6});
        bands.put(new ResourceLocation("minecraft:brown_mushroom"), new int[]{5, 10});
        bands.put(new ResourceLocation("minecraft:red_mushroom"), new int[]{5, 10});
        bands.put(new ResourceLocation("minecraft:string"), new int[]{3, 6});
        bands.put(new ResourceLocation("minecraft:ink_sac"), new int[]{6, 12});
        bands.put(new ResourceLocation("minecraft:glow_ink_sac"), new int[]{8, 16});
        bands.put(new ResourceLocation("minecraft:scute"), new int[]{30, 70});
        bands.put(new ResourceLocation("minecraft:egg"), new int[]{3, 8});
        bands.put(new ResourceLocation("minecraft:feather"), new int[]{2, 8});
        bands.put(new ResourceLocation("minecraft:leather"), new int[]{8, 28});
        bands.put(new ResourceLocation("minecraft:gunpowder"), new int[]{15, 20});
        bands.put(new ResourceLocation("minecraft:paper"), new int[]{2, 5});
        bands.put(new ResourceLocation("minecraft:stone"), new int[]{CHEAP_BUILD_MIN, CHEAP_BUILD_MAX});
        bands.put(new ResourceLocation("minecraft:cobblestone"), new int[]{CHEAP_BUILD_MIN, CHEAP_BUILD_MAX});
        bands.put(new ResourceLocation("minecraft:diorite"), new int[]{CHEAP_BUILD_MIN, CHEAP_BUILD_MAX});
        bands.put(new ResourceLocation("minecraft:andesite"), new int[]{CHEAP_BUILD_MIN, CHEAP_BUILD_MAX});
        bands.put(new ResourceLocation("minecraft:granite"), new int[]{CHEAP_BUILD_MIN, CHEAP_BUILD_MAX});
        bands.put(new ResourceLocation("minecraft:calcite"), new int[]{CHEAP_BUILD_MIN, CHEAP_BUILD_MAX});
        bands.put(new ResourceLocation("minecraft:sand"), new int[]{CHEAP_BUILD_MIN, CHEAP_BUILD_MAX});
        bands.put(new ResourceLocation("minecraft:gravel"), new int[]{CHEAP_BUILD_MIN, CHEAP_BUILD_MAX});
        // Raw meats: explicit bands so the cheap food formula cannot price them at 1-2.
        bands.put(new ResourceLocation("minecraft:chicken"), new int[]{12, 14});
        bands.put(new ResourceLocation("minecraft:porkchop"), new int[]{12, 14});
        bands.put(new ResourceLocation("minecraft:beef"), new int[]{25, 30});
        bands.put(new ResourceLocation("minecraft:mutton"), new int[]{18, 22});
        bands.put(new ResourceLocation("minecraft:vine"), new int[]{DEFAULT_FLOWER_VINE_MIN, DEFAULT_FLOWER_VINE_MAX});
        for (ResourceLocation leaf : FLOWER_SHOP_LEAVES) {
            bands.put(leaf, new int[]{1, 1});
        }
        // Netherite gear stays craft-priced: raising the ingot band lifts every piece with it.
        putBuildShopBands(bands);
        return bands;
    }

    private static Set<ResourceLocation> buildShopExclusiveSet() {
        java.util.HashSet<ResourceLocation> set = new java.util.HashSet<>();
        set.add(new ResourceLocation("minecraft:stone"));
        set.add(new ResourceLocation("minecraft:deepslate"));
        set.add(new ResourceLocation("minecraft:cobbled_deepslate"));
        set.add(new ResourceLocation("minecraft:granite"));
        set.add(new ResourceLocation("minecraft:andesite"));
        set.add(new ResourceLocation("minecraft:diorite"));
        set.add(new ResourceLocation("minecraft:calcite"));
        set.add(new ResourceLocation("minecraft:mossy_stone_bricks"));
        set.add(new ResourceLocation("minecraft:dirt"));
        set.add(new ResourceLocation("minecraft:grass_block"));
        set.add(new ResourceLocation("minecraft:moss_block"));
        set.add(new ResourceLocation("minecraft:podzol"));
        set.add(new ResourceLocation("minecraft:mud"));
        set.add(new ResourceLocation("minecraft:packed_mud"));
        set.add(new ResourceLocation("minecraft:brown_mushroom_block"));
        set.add(new ResourceLocation("minecraft:red_mushroom_block"));
        set.add(new ResourceLocation("minecraft:mushroom_stem"));
        set.add(new ResourceLocation("minecraft:terracotta"));
        for (String color : DYE_COLOR_SUFFIXES) {
            set.add(new ResourceLocation("minecraft", color + "_terracotta"));
            set.add(new ResourceLocation("minecraft", color + "_concrete"));
            set.add(new ResourceLocation("minecraft", color + "_concrete_powder"));
            set.add(new ResourceLocation("minecraft", color + "_stained_glass"));
        }
        for (String log : BUILD_LOG_PATHS) {
            set.add(new ResourceLocation("minecraft", log));
        }
        for (String wood : BUILD_WOOD_PATHS) {
            set.add(new ResourceLocation("minecraft", wood));
        }
        for (String coral : BUILD_CORAL_BLOCK_PATHS) {
            set.add(new ResourceLocation("minecraft", coral));
        }
        for (String froglight : BUILD_FROGLIGHT_PATHS) {
            set.add(new ResourceLocation("minecraft", froglight));
        }
        set.add(new ResourceLocation("minecraft:end_rod"));
        set.add(new ResourceLocation("minecraft:shroomlight"));
        set.add(new ResourceLocation("minecraft:campfire"));
        set.add(new ResourceLocation("minecraft:lantern"));
        return Set.copyOf(set);
    }

    private static Set<ResourceLocation> flowerShopLeafSet() {
        java.util.HashSet<ResourceLocation> set = new java.util.HashSet<>();
        for (String leaf : FLOWER_LEAF_PATHS) {
            set.add(new ResourceLocation("minecraft", leaf));
        }
        return Set.copyOf(set);
    }

    private static Set<ResourceLocation> redstoneShopExclusiveSet() {
        return Set.of(
                new ResourceLocation("minecraft:redstone_torch"),
                new ResourceLocation("minecraft:repeater"),
                new ResourceLocation("minecraft:comparator"),
                new ResourceLocation("minecraft:piston"),
                new ResourceLocation("minecraft:sticky_piston"),
                new ResourceLocation("minecraft:observer"),
                new ResourceLocation("minecraft:hopper"),
                new ResourceLocation("minecraft:dropper"),
                new ResourceLocation("minecraft:dispenser"),
                new ResourceLocation("minecraft:daylight_detector"),
                new ResourceLocation("minecraft:redstone_lamp"),
                new ResourceLocation("minecraft:lever"),
                new ResourceLocation("minecraft:tripwire_hook"),
                new ResourceLocation("minecraft:target"),
                new ResourceLocation("minecraft:note_block"),
                new ResourceLocation("minecraft:jukebox"),
                new ResourceLocation("minecraft:rail"),
                new ResourceLocation("minecraft:powered_rail"),
                new ResourceLocation("minecraft:detector_rail"),
                new ResourceLocation("minecraft:activator_rail"),
                new ResourceLocation("minecraft:minecart"),
                new ResourceLocation("minecraft:chest_minecart"),
                new ResourceLocation("minecraft:furnace_minecart"),
                new ResourceLocation("minecraft:hopper_minecart"),
                new ResourceLocation("minecraft:tnt_minecart"),
                new ResourceLocation("minecraft:chain"),
                new ResourceLocation("minecraft:stone_button"),
                new ResourceLocation("minecraft:oak_button"),
                new ResourceLocation("minecraft:polished_blackstone_button"),
                new ResourceLocation("minecraft:stone_pressure_plate"),
                new ResourceLocation("minecraft:oak_pressure_plate"),
                new ResourceLocation("minecraft:polished_blackstone_pressure_plate"),
                new ResourceLocation("minecraft:light_weighted_pressure_plate"),
                new ResourceLocation("minecraft:heavy_weighted_pressure_plate")
        );
    }

    private static Set<ResourceLocation> weaponShopExclusiveSet() {
        return Set.of(
                new ResourceLocation("minecraft:wooden_sword"),
                new ResourceLocation("minecraft:stone_sword"),
                new ResourceLocation("minecraft:iron_sword"),
                new ResourceLocation("minecraft:golden_sword"),
                new ResourceLocation("minecraft:diamond_sword"),
                new ResourceLocation("minecraft:netherite_sword"),
                new ResourceLocation("minecraft:bow"),
                new ResourceLocation("minecraft:crossbow"),
                new ResourceLocation("minecraft:trident"),
                new ResourceLocation("minecraft:shield")
        );
    }

    private static Set<ResourceLocation> armorShopExclusiveSet() {
        java.util.HashSet<ResourceLocation> set = new java.util.HashSet<>();
        for (String mat : new String[]{"leather", "chainmail", "iron", "golden", "diamond", "netherite"}) {
            set.add(new ResourceLocation("minecraft", mat + "_helmet"));
            set.add(new ResourceLocation("minecraft", mat + "_chestplate"));
            set.add(new ResourceLocation("minecraft", mat + "_leggings"));
            set.add(new ResourceLocation("minecraft", mat + "_boots"));
        }
        set.add(new ResourceLocation("minecraft:leather_horse_armor"));
        set.add(new ResourceLocation("minecraft:iron_horse_armor"));
        set.add(new ResourceLocation("minecraft:golden_horse_armor"));
        set.add(new ResourceLocation("minecraft:diamond_horse_armor"));
        return Set.copyOf(set);
    }

    private static Set<ResourceLocation> farmToolShopExclusiveSet() {
        java.util.HashSet<ResourceLocation> set = new java.util.HashSet<>();
        for (String mat : new String[]{"wooden", "stone", "iron", "golden", "diamond", "netherite"}) {
            set.add(new ResourceLocation("minecraft", mat + "_hoe"));
            set.add(new ResourceLocation("minecraft", mat + "_shovel"));
        }
        set.add(new ResourceLocation("minecraft:shears"));
        set.add(new ResourceLocation("minecraft:lead"));
        set.add(new ResourceLocation("farmersdelight:organic_compost"));
        set.add(new ResourceLocation("farmersdelight:straw"));
        set.add(new ResourceLocation("farmersdelight:rich_soil"));
        return Set.copyOf(set);
    }

    private static Set<ResourceLocation> farmToolDualListedSet() {
        return Set.of(
                new ResourceLocation("minecraft:bone_meal"),
                new ResourceLocation("minecraft:composter")
        );
    }

    private static Set<ResourceLocation> furnitureVanillaWhitelist() {
        java.util.HashSet<ResourceLocation> set = new java.util.HashSet<>();
        String[] woods = {
                "oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
                "mangrove", "cherry", "bamboo", "crimson", "warped"
        };
        for (String wood : woods) {
            set.add(new ResourceLocation("minecraft", wood + "_door"));
            set.add(new ResourceLocation("minecraft", wood + "_trapdoor"));
            set.add(new ResourceLocation("minecraft", wood + "_fence_gate"));
            set.add(new ResourceLocation("minecraft", wood + "_sign"));
            set.add(new ResourceLocation("minecraft", wood + "_hanging_sign"));
        }
        for (String color : DYE_COLOR_SUFFIXES) {
            set.add(new ResourceLocation("minecraft", color + "_bed"));
            set.add(new ResourceLocation("minecraft", color + "_carpet"));
            set.add(new ResourceLocation("minecraft", color + "_candle"));
            set.add(new ResourceLocation("minecraft", color + "_candle_cake"));
        }
        set.add(new ResourceLocation("minecraft:bookshelf"));
        set.add(new ResourceLocation("minecraft:lectern"));
        set.add(new ResourceLocation("minecraft:chest"));
        set.add(new ResourceLocation("minecraft:barrel"));
        set.add(new ResourceLocation("minecraft:armor_stand"));
        set.add(new ResourceLocation("minecraft:painting"));
        set.add(new ResourceLocation("minecraft:item_frame"));
        set.add(new ResourceLocation("minecraft:glow_item_frame"));
        set.add(new ResourceLocation("minecraft:clock"));
        set.add(new ResourceLocation("minecraft:lantern"));
        set.add(new ResourceLocation("minecraft:soul_lantern"));
        set.add(new ResourceLocation("minecraft:campfire"));
        set.add(new ResourceLocation("minecraft:soul_campfire"));
        set.add(new ResourceLocation("minecraft:candle"));
        set.add(new ResourceLocation("minecraft:candle_cake"));
        set.add(new ResourceLocation("minecraft:ladder"));
        set.add(new ResourceLocation("minecraft:scaffolding"));
        set.add(new ResourceLocation("minecraft:composter"));
        set.add(new ResourceLocation("minecraft:smithing_table"));
        set.add(new ResourceLocation("minecraft:grindstone"));
        set.add(new ResourceLocation("minecraft:loom"));
        set.add(new ResourceLocation("minecraft:cartography_table"));
        set.add(new ResourceLocation("minecraft:note_block"));
        set.add(new ResourceLocation("minecraft:jukebox"));
        set.add(new ResourceLocation("minecraft:iron_door"));
        set.add(new ResourceLocation("minecraft:iron_trapdoor"));
        return Set.copyOf(set);
    }

    private static void putBuildShopBands(Map<ResourceLocation, int[]> bands) {
        for (ResourceLocation id : BUILD_SHOP_EXCLUSIVE) {
            String path = id.getPath();
            if (isBuildCraftPricedWood(id)) {
                // Recipe-priced wood / hyphae: live band from craft+1, not a fixed catalog band.
                continue;
            }
            if (path.equals("campfire")) {
                bands.put(id, new int[]{20, 32});
            } else if (path.equals("lantern")) {
                bands.put(id, new int[]{32, 48});
            } else if (path.endsWith("_concrete_powder") || path.endsWith("_concrete")) {
                bands.put(id, new int[]{13, 16});
            } else if (path.endsWith("_stained_glass")) {
                bands.put(id, new int[]{3, 5});
            } else if (path.equals("dirt") || path.equals("grass_block") || path.equals("moss_block")) {
                bands.put(id, new int[]{1, 1});
            } else if (path.equals("podzol")
                    || path.equals("brown_mushroom_block")
                    || path.equals("red_mushroom_block")
                    || path.equals("mushroom_stem")) {
                // mushroom_stem before endsWith("_stem") so it does not use log band 8–12
                bands.put(id, new int[]{2, 3});
            } else if (path.equals("mud")) {
                bands.put(id, new int[]{1, 2});
            } else if (path.equals("packed_mud")) {
                bands.put(id, new int[]{8, 10});
            } else if (path.equals("mossy_stone_bricks")) {
                bands.put(id, new int[]{4, 4});
            } else if (path.endsWith("_log") || path.endsWith("_stem")) {
                bands.put(id, new int[]{LOG_BUILD_MIN, LOG_BUILD_MAX});
            } else if (path.endsWith("_coral_block")) {
                bands.put(id, new int[]{CORAL_BUILD_MIN, CORAL_BUILD_MAX});
            } else if (path.endsWith("_froglight")) {
                bands.put(id, new int[]{FROGLIGHT_BUILD_MIN, FROGLIGHT_BUILD_MAX});
            } else if (path.equals("end_rod")) {
                bands.put(id, new int[]{END_ROD_BUILD_MIN, END_ROD_BUILD_MAX});
            } else if (path.equals("shroomlight")) {
                bands.put(id, new int[]{SHROOMLIGHT_BUILD_MIN, SHROOMLIGHT_BUILD_MAX});
            } else {
                // Stone + terracotta: keep intentionally cheap; do not follow clay craft cost.
                bands.put(id, new int[]{CHEAP_BUILD_MIN, CHEAP_BUILD_MAX});
            }
        }
    }
}
