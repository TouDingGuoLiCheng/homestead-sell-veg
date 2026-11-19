package com.homestead.sellveg.shop;

import com.homestead.sellveg.market.PriceCatalog;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;

public final class ShopOffers {
    private static ShopShelfPlanner.Assignment cached;

    private ShopOffers() {
    }

    public static void invalidate() {
        cached = null;
    }

    public static void warmup() {
        current();
    }

    public static ShopShelfPlanner.Assignment current() {
        if (cached != null) {
            return cached;
        }
        List<ShopShelfPlanner.ItemRef> refs = new ArrayList<>();
        for (Item item : PriceCatalog.collectTradeableItems()) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (id == null) {
                continue;
            }
            refs.add(new ShopShelfPlanner.ItemRef(
                    id.toString(),
                    PriceCatalog.shelfKind(item),
                    PriceCatalog.isSpecialtyShopExclusive(item),
                    PriceCatalog.isRedstoneDualListed(item),
                    PriceCatalog.isFurnitureDualListed(item),
                    PriceCatalog.isFarmToolDualListed(item)));
        }
        cached = ShopShelfPlanner.assign(PresetShops.ALL, refs);
        return cached;
    }

    public static String itemKey(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return id == null ? "minecraft:air" : id.toString();
    }
}
