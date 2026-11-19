package com.homestead.sellveg.shop;

import java.util.List;
import java.util.Optional;

public final class PresetShops {
    public static final List<PresetShop> ALL = List.of(
            new PresetShop("pulpy_orange", ShopKind.FARM),
            new PresetShop("golden_wheat", ShopKind.FARM),
            new PresetShop("moonlight_market", ShopKind.FARM),
            new PresetShop("space_ore", ShopKind.ORE),
            new PresetShop("villager_mining", ShopKind.ORE),
            new PresetShop("deep_down", ShopKind.ORE),
            new PresetShop("torn_bread", ShopKind.FOOD),
            new PresetShop("oven_4am", ShopKind.FOOD),
            new PresetShop("pulpy_orange_bakery", ShopKind.FOOD),
            new PresetShop("night_sparrow", ShopKind.FOOD),
            new PresetShop("omni_mart", ShopKind.GENERAL),
            new PresetShop("beacon_flagship", ShopKind.GENERAL),
            new PresetShop("cola_two", ShopKind.GROCERY),
            new PresetShop("corner_store", ShopKind.GROCERY),
            new PresetShop("pulpy_orange_souvenir", ShopKind.GROCERY),
            new PresetShop("block_build_co", ShopKind.BUILD),
            new PresetShop("civil_bro_supply", ShopKind.BUILD),
            new PresetShop("forget_me_not", ShopKind.FLOWER),
            new PresetShop("fragrant_tale", ShopKind.FLOWER),
            new PresetShop("red_stone_shop", ShopKind.REDSTONE),
            new PresetShop("iron_bro_forge", ShopKind.WEAPON),
            new PresetShop("scaredy_armor", ShopKind.ARMOR),
            new PresetShop("cozy_furniture", ShopKind.FURNITURE),
            new PresetShop("pulpy_orange_tools", ShopKind.FARM_TOOL)
    );

    private PresetShops() {
    }

    public static Optional<PresetShop> byId(String id) {
        if (id == null || id.isEmpty()) {
            return Optional.empty();
        }
        for (PresetShop shop : ALL) {
            if (shop.id().equals(id)) {
                return Optional.of(shop);
            }
        }
        return Optional.empty();
    }

    /** Directory grouping only; shelf planning still keys off {@link ShopKind}. */
    public static List<PresetShop> inGroup(ShopGroup group) {
        return ALL.stream().filter(shop -> group.contains(shop.kind())).toList();
    }
}
