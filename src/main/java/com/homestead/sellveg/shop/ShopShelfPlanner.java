package com.homestead.sellveg.shop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ShopShelfPlanner {
    public record ItemRef(String id, ItemShelfKind kind, boolean specialtyOnly, boolean alsoInRedstoneShop,
                          boolean alsoInFurnitureShop, boolean alsoInFarmToolShop) {
        public ItemRef(String id, ItemShelfKind kind) {
            this(id, kind, false, false, false, false);
        }

        public ItemRef(String id, ItemShelfKind kind, boolean specialtyOnly) {
            this(id, kind, specialtyOnly, false, false, false);
        }

        public ItemRef(String id, ItemShelfKind kind, boolean specialtyOnly, boolean alsoInRedstoneShop) {
            this(id, kind, specialtyOnly, alsoInRedstoneShop, false, false);
        }

        public ItemRef(String id, ItemShelfKind kind, boolean specialtyOnly, boolean alsoInRedstoneShop,
                       boolean alsoInFurnitureShop) {
            this(id, kind, specialtyOnly, alsoInRedstoneShop, alsoInFurnitureShop, false);
        }
    }

    public static final class Assignment {
        private final Map<String, LinkedHashSet<String>> itemsByShop;
        private final Map<String, LinkedHashSet<String>> shopsByItem;

        Assignment(Map<String, LinkedHashSet<String>> itemsByShop, Map<String, LinkedHashSet<String>> shopsByItem) {
            this.itemsByShop = itemsByShop;
            this.shopsByItem = shopsByItem;
        }

        public Set<String> itemsOf(String shopId) {
            LinkedHashSet<String> set = itemsByShop.get(shopId);
            return set == null ? Set.of() : Collections.unmodifiableSet(set);
        }

        public Set<String> shopsFor(String itemId) {
            LinkedHashSet<String> set = shopsByItem.get(itemId);
            return set == null ? Set.of() : Collections.unmodifiableSet(set);
        }

        public boolean sells(String shopId, String itemId) {
            LinkedHashSet<String> set = itemsByShop.get(shopId);
            return set != null && set.contains(itemId);
        }
    }

    private ShopShelfPlanner() {
    }

    public static Assignment assign(List<PresetShop> shops, List<ItemRef> items) {
        Map<ShopKind, List<PresetShop>> byKind = new EnumMap<>(ShopKind.class);
        for (PresetShop shop : shops) {
            byKind.computeIfAbsent(shop.kind(), k -> new ArrayList<>()).add(shop);
        }
        Map<String, LinkedHashSet<String>> itemsByShop = new LinkedHashMap<>();
        for (PresetShop shop : shops) {
            itemsByShop.put(shop.id(), new LinkedHashSet<>());
        }
        Map<String, LinkedHashSet<String>> shopsByItem = new HashMap<>();
        List<PresetShop> general = byKind.getOrDefault(ShopKind.GENERAL, List.of());

        for (ItemRef item : items) {
            LinkedHashSet<String> owners = new LinkedHashSet<>();
            ShopKind match = switch (item.kind()) {
                case FARM -> ShopKind.FARM;
                case ORE -> ShopKind.ORE;
                case FOOD -> ShopKind.FOOD;
                case GROCERY -> ShopKind.GROCERY;
                case BUILD -> ShopKind.BUILD;
                case FLOWER -> ShopKind.FLOWER;
                case REDSTONE -> ShopKind.REDSTONE;
                case WEAPON -> ShopKind.WEAPON;
                case ARMOR -> ShopKind.ARMOR;
                case FURNITURE -> ShopKind.FURNITURE;
                case FARM_TOOL -> ShopKind.FARM_TOOL;
            };
            for (PresetShop shop : byKind.getOrDefault(match, List.of())) {
                owners.add(shop.id());
            }
            if (item.alsoInRedstoneShop()) {
                for (PresetShop shop : byKind.getOrDefault(ShopKind.REDSTONE, List.of())) {
                    owners.add(shop.id());
                }
            }
            if (item.alsoInFurnitureShop()) {
                for (PresetShop shop : byKind.getOrDefault(ShopKind.FURNITURE, List.of())) {
                    owners.add(shop.id());
                }
            }
            if (item.alsoInFarmToolShop()) {
                for (PresetShop shop : byKind.getOrDefault(ShopKind.FARM_TOOL, List.of())) {
                    owners.add(shop.id());
                }
            }
            if (!item.specialtyOnly()) {
                if (owners.isEmpty()) {
                    for (PresetShop shop : general) {
                        owners.add(shop.id());
                    }
                } else {
                    for (PresetShop shop : general) {
                        if (Math.floorMod(mix(item.id(), shop.id()), 2) == 0) {
                            owners.add(shop.id());
                        }
                    }
                }
                // Cross-theme spill only into general stores, never FARM/ORE/FOOD specialty shops.
                List<PresetShop> extras = new ArrayList<>();
                for (PresetShop shop : general) {
                    if (!owners.contains(shop.id())) {
                        extras.add(shop);
                    }
                }
                extras.sort((a, b) -> Integer.compare(mix(item.id(), a.id()), mix(item.id(), b.id())));
                int extraCount = Math.floorMod(mix(item.id(), "extra"), 3);
                for (int i = 0; i < extraCount && i < extras.size(); i++) {
                    owners.add(extras.get(i).id());
                }
            }
            if (owners.isEmpty() && !shops.isEmpty()) {
                owners.add(shops.get(0).id());
            }
            shopsByItem.put(item.id(), owners);
            for (String shopId : owners) {
                itemsByShop.computeIfAbsent(shopId, k -> new LinkedHashSet<>()).add(item.id());
            }
        }
        return new Assignment(itemsByShop, shopsByItem);
    }

    private static int mix(String a, String b) {
        return 31 * (a == null ? 0 : a.hashCode()) + (b == null ? 0 : b.hashCode());
    }

}
