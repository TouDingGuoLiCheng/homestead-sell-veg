package com.homestead.sellveg.shop;

import java.util.List;

/**
 * First-level directory grouping of the preset shops, sized so every group fits one screen.
 * Display only: it never feeds shelf planning or pricing, which stay keyed on {@link ShopKind}.
 * Icons live in the client-side {@code ShopIcons} table so this class stays loadable by unit tests.
 */
public enum ShopGroup {
    FARM("farm", ShopKind.FARM, ShopKind.FARM_TOOL),
    FOOD("food", ShopKind.FOOD),
    DAILY("daily", ShopKind.GROCERY, ShopKind.GENERAL),
    ORE("ore", ShopKind.ORE),
    BUILD("build", ShopKind.BUILD, ShopKind.REDSTONE),
    GEAR("gear", ShopKind.WEAPON, ShopKind.ARMOR),
    HOME("home", ShopKind.FLOWER, ShopKind.FURNITURE);

    private final String id;
    private final List<ShopKind> kinds;

    ShopGroup(String id, ShopKind... kinds) {
        this.id = id;
        this.kinds = List.of(kinds);
    }

    public String id() {
        return id;
    }

    public String langKey() {
        return "gui.homestead_sell_veg.group." + id;
    }

    public boolean contains(ShopKind kind) {
        return kinds.contains(kind);
    }
}
