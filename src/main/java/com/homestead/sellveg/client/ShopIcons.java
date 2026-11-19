package com.homestead.sellveg.client;

import com.homestead.sellveg.shop.ShopGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Map;

/**
 * Client-side icon table for the shop directory. Deliberately kept out of {@code PresetShops}:
 * the data classes are loaded by unit tests, where {@code Items} cannot be initialized.
 */
@OnlyIn(Dist.CLIENT)
public final class ShopIcons {
    private static final Map<String, Item> BY_SHOP = Map.ofEntries(
            Map.entry("pulpy_orange", Items.APPLE),
            Map.entry("golden_wheat", Items.WHEAT),
            Map.entry("moonlight_market", Items.CARROT),
            Map.entry("space_ore", Items.AMETHYST_SHARD),
            Map.entry("villager_mining", Items.IRON_PICKAXE),
            Map.entry("deep_down", Items.DEEPSLATE),
            Map.entry("torn_bread", Items.BREAD),
            Map.entry("oven_4am", Items.CAMPFIRE),
            Map.entry("pulpy_orange_bakery", Items.CAKE),
            Map.entry("night_sparrow", Items.COOKED_CHICKEN),
            Map.entry("omni_mart", Items.EMERALD),
            Map.entry("beacon_flagship", Items.BEACON),
            Map.entry("cola_two", Items.HONEY_BOTTLE),
            Map.entry("corner_store", Items.MILK_BUCKET),
            Map.entry("pulpy_orange_souvenir", Items.PAPER),
            Map.entry("block_build_co", Items.BRICKS),
            Map.entry("civil_bro_supply", Items.STONE),
            Map.entry("forget_me_not", Items.CORNFLOWER),
            Map.entry("fragrant_tale", Items.PEONY),
            Map.entry("red_stone_shop", Items.REDSTONE),
            Map.entry("iron_bro_forge", Items.ANVIL),
            Map.entry("scaredy_armor", Items.IRON_CHESTPLATE),
            Map.entry("cozy_furniture", Items.RED_BED),
            Map.entry("pulpy_orange_tools", Items.IRON_HOE)
    );

    private static final Map<ShopGroup, Item> BY_GROUP = Map.of(
            ShopGroup.FARM, Items.WHEAT,
            ShopGroup.FOOD, Items.BREAD,
            ShopGroup.DAILY, Items.MILK_BUCKET,
            ShopGroup.ORE, Items.IRON_ORE,
            ShopGroup.BUILD, Items.BRICKS,
            ShopGroup.GEAR, Items.IRON_SWORD,
            ShopGroup.HOME, Items.PEONY
    );

    private ShopIcons() {
    }

    public static ItemStack ofShop(String shopId) {
        Item item = shopId == null ? null : BY_SHOP.get(shopId);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    public static ItemStack ofGroup(ShopGroup group) {
        Item item = group == null ? null : BY_GROUP.get(group);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }
}
