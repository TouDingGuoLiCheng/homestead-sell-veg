package com.homestead.sellveg.market;

import com.homestead.sellveg.config.SellVegConfig;
import com.homestead.sellveg.shop.ShopOfferPricing;
import com.homestead.sellveg.shop.ShopOffers;
import com.homestead.sellveg.shop.ShopShelfPlanner;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.homestead.sellveg.shop.GearOffers;
import com.homestead.sellveg.shop.PresetShop;
import com.homestead.sellveg.shop.PresetShops;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Per-player live price bands (shifted by volume) and a daily sample inside the band.
 */
public final class MarketData {
    /** Bump when catalog craft / band rules change so old player bands are rebuilt. */
    public static final int PRICING_RULES_VERSION = 11;

    private static final String KEY_DAY = "DayIndex";
    private static final String KEY_SETTLED = "SettledWorldDay";
    private static final String KEY_INIT = "Init";
    private static final String KEY_PRICING_RULES = "PricingRules";
    private static final String KEY_PRICES = "Prices";
    private static final String KEY_SOLD = "SoldToday";
    private static final String KEY_BOUGHT = "BoughtToday";
    private static final String KEY_SALE_BOUGHT = "SaleBoughtToday";
    private static final String KEY_BAND_LO = "BandLo";
    private static final String KEY_BAND_HI = "BandHi";

    private long dayIndex;
    /** Last overworld day index for which end-of-day adjustments already ran. */
    private long lastSettledWorldDay = -1L;
    private int pricingRulesVersion;
    private final Map<Item, Integer> referencePrices = new HashMap<>();
    private final Map<Item, Integer> bandLo = new HashMap<>();
    private final Map<Item, Integer> bandHi = new HashMap<>();
    private final Map<Item, Integer> soldToday = new HashMap<>();
    private final Map<Item, Integer> boughtToday = new HashMap<>();
    private int saleBoughtToday;
    private final Set<Item> hotItems = new HashSet<>();
    /** item → (shopId → percent off 10–99) */
    private final Map<Item, Map<String, Integer>> saleShops = new HashMap<>();
    /** Daily enchanted gear SKUs for weapon/armor shops. */
    private final List<GearOffers.Offer> gearEnchantOffers = new ArrayList<>();
    /** Old saves omitted Percent and were filled with a flat 20 — force a fresh roll once. */
    private boolean forceSaleReroll;
    /** False until the first {@link #rollNewDay}; day index 0 is a valid world day. */
    private boolean initialized;

    public long getDayIndex() {
        return dayIndex;
    }

    public long getLastSettledWorldDay() {
        return lastSettledWorldDay;
    }

    public void markSettledWorldDay(long worldDay) {
        this.lastSettledWorldDay = worldDay;
    }

    public boolean isInitialized() {
        return initialized;
    }

    /** Live band, or {@code null} when the item has no derivable price (no fallback bands). */
    public int[] getLiveBand(Item item) {
        int[] orig = PriceCatalog.marketBand(item);
        if (orig == null) {
            return null;
        }
        int lo = bandLo.getOrDefault(item, orig[0]);
        int hi = bandHi.getOrDefault(item, orig[1]);
        int craft = liveBandCraftFloor(item);
        if (PriceMath.needsCatalogBandRebase(lo, hi, orig[0], orig[1], craft)) {
            return PriceMath.resolveLiveBand(orig[0], PriceMath.bandHiWithCraftFloor(orig[1], craft, false));
        }
        return PriceMath.resolveLiveBand(lo, hi);
    }

    /**
     * After catalog craft rules change, old per-player bands can sit below the new floor.
     * Returns true when any band/reference was rewritten (caller should save).
     */
    public boolean syncPricingRules(RandomSource random) {
        boolean changed = false;
        if (pricingRulesVersion != PRICING_RULES_VERSION) {
            pricingRulesVersion = PRICING_RULES_VERSION;
            resetBandsToCatalog(random);
            // Daily enchanted gear SKUs carry a rolled price; rebuild them under the new bands.
            gearEnchantOffers.clear();
            return true;
        }
        for (Item item : PriceCatalog.collectTradeableItems()) {
            int[] orig = PriceCatalog.marketBand(item);
            if (orig == null) {
                continue;
            }
            int craft = liveBandCraftFloor(item);
            int lo = bandLo.getOrDefault(item, orig[0]);
            int hi = bandHi.getOrDefault(item, orig[1]);
            if (!PriceMath.needsCatalogBandRebase(lo, hi, orig[0], orig[1], craft)) {
                continue;
            }
            int nextHi = PriceMath.bandHiWithCraftFloor(orig[1], craft, false);
            bandLo.put(item, orig[0]);
            bandHi.put(item, nextHi);
            int mid = (orig[0] + nextHi) / 2;
            referencePrices.put(item, craft > 0 ? Math.max(mid, craft + 1) : mid);
            changed = true;
        }
        return changed;
    }

    private void resetBandsToCatalog(RandomSource random) {
        for (Item item : PriceCatalog.collectTradeableItems()) {
            int[] orig = PriceCatalog.marketBand(item);
            if (orig == null) {
                continue;
            }
            int craft = liveBandCraftFloor(item);
            int hi = PriceMath.bandHiWithCraftFloor(orig[1], craft, false);
            bandLo.put(item, orig[0]);
            bandHi.put(item, hi);
            int span = Math.max(1, hi - orig[0] + 1);
            int price = orig[0] + random.nextInt(span);
            if (craft > 0) {
                price = Math.max(price, craft + 1);
            }
            referencePrices.put(item, price);
        }
    }

    /** Zero for catalog cheap goods so clay/seed recipes cannot inflate 1–3 bands. */
    private static int liveBandCraftFloor(Item item) {
        return PriceMath.craftFloorForLiveBand(
                FoodRecipeIndex.cheapestCraftCostPerResult(item),
                PriceCatalog.ignoresCraftFloor(item));
    }

    public int getReferencePrice(Item item) {
        int[] band = getLiveBand(item);
        if (band == null) {
            return 0;
        }
        int fallback = (band[0] + band[1]) / 2;
        int price = referencePrices.getOrDefault(item, fallback);
        return PriceCatalog.raiseToCraftFloor(item, PriceMath.clampRange(price, band[0], band[1]));
    }

    /** Midpoint of the live suggest band (Quick Lister display + list price). */
    public int getSuggestMid(Item item) {
        int[] band = getLiveBand(item);
        if (band == null) {
            return 0;
        }
        return com.homestead.sellveg.shop.QuickListMath.suggestMid(band[0], band[1]);
    }

    public int getBuyPrice(Item item) {
        String shop = cheapestShop(item);
        if (shop == null || shop.isEmpty()) {
            int buy = PriceCatalog.getBuyPrice(this, item);
            return PriceCatalog.raiseToCraftFloor(item, buy);
        }
        return getShopBuyPrice(shop, item);
    }

    public int getShopBaseBuyPrice(String shopId, Item item) {
        int[] band = getLiveBand(item);
        if (band == null) {
            return 0;
        }
        double mul = 1.15D;
        try {
            mul = SellVegConfig.BUY_PRICE_MULTIPLIER.get();
        } catch (Exception ignored) {
        }
        int pick = ShopOfferPricing.bandPick(band[0], band[1], dayIndex, shopId, ShopOffers.itemKey(item));
        int buy = ShopOfferPricing.buyFromPick(pick, mul, 0);
        return PriceCatalog.raiseToCraftFloor(item, buy);
    }

    public int getShopBuyPrice(String shopId, Item item) {
        int[] band = getLiveBand(item);
        if (band == null) {
            return 0;
        }
        double mul = 1.15D;
        try {
            mul = SellVegConfig.BUY_PRICE_MULTIPLIER.get();
        } catch (Exception ignored) {
        }
        int pick = ShopOfferPricing.bandPick(band[0], band[1], dayIndex, shopId, ShopOffers.itemKey(item));
        int buy = ShopOfferPricing.buyFromPick(pick, mul, getSalePercent(shopId, item));
        return PriceCatalog.raiseToCraftFloor(item, buy);
    }

    public boolean shopSells(String shopId, Item item) {
        return ShopOffers.current().sells(shopId, ShopOffers.itemKey(item));
    }

    public String cheapestShop(Item item) {
        String best = "";
        int bestPrice = Integer.MAX_VALUE;
        for (String shopId : ShopOffers.current().shopsFor(ShopOffers.itemKey(item))) {
            int price = getShopBuyPrice(shopId, item);
            if (price < bestPrice || (price == bestPrice && shopId.compareTo(best) < 0)) {
                best = shopId;
                bestPrice = price;
            }
        }
        return best;
    }

    public boolean isHot(Item item) {
        return hotItems.contains(item);
    }

    public boolean isSale(Item item) {
        Map<String, Integer> shops = saleShops.get(item);
        return shops != null && !shops.isEmpty();
    }

    public boolean isSale(String shopId, Item item) {
        return getSalePercent(shopId, item) > 0;
    }

    public int getSalePercent(String shopId, Item item) {
        Map<String, Integer> shops = saleShops.get(item);
        if (shops == null || shopId == null) {
            return 0;
        }
        return Math.max(0, shops.getOrDefault(shopId, 0));
    }

    public Set<Item> hotItems() {
        return hotItems;
    }

    public Set<Item> saleItems() {
        return saleShops.keySet();
    }

    public int getSaleBoughtToday() {
        return saleBoughtToday;
    }

    public void recordSaleBought(int count) {
        if (count > 0) {
            saleBoughtToday += count;
        }
    }

    public boolean saleSpreadInvalid() {
        if (forceSaleReroll) {
            return true;
        }
        if (saleShops.isEmpty()) {
            return false;
        }
        for (Map<String, Integer> shops : saleShops.values()) {
            if (shops == null || shops.isEmpty() || shops.size() > SaleRollMath.MAX_SHOPS_PER_ITEM) {
                return true;
            }
            for (int percent : shops.values()) {
                if (percent < 1 || percent > 99) {
                    return true;
                }
            }
        }
        return false;
    }

    public void recordSold(Item item, int count) {
        soldToday.merge(item, count, Integer::sum);
    }

    public void recordBought(Item item, int count) {
        boughtToday.merge(item, count, Integer::sum);
    }

    public void rollNewDay(long newDayIndex, RandomSource random) {
        this.dayIndex = newDayIndex;
        this.initialized = true;
        soldToday.clear();
        boughtToday.clear();
        saleBoughtToday = 0;

        for (Item item : PriceCatalog.collectTradeableItems()) {
            int[] live = getLiveBand(item);
            if (live == null) {
                continue;
            }
            int craft = liveBandCraftFloor(item);
            bandLo.put(item, live[0]);
            bandHi.put(item, live[1]);
            Integer existing = referencePrices.get(item);
            int price = existing == null
                    ? live[0] + random.nextInt(Math.max(1, live[1] - live[0] + 1))
                    : PriceMath.clampRange(existing, live[0], live[1]);
            if (craft > 0) {
                price = Math.max(price, Math.min(live[1], craft + 1));
            }
            referencePrices.put(item, price);
        }
        rollActivities(random);
        rollGearEnchantOffers(random);
    }

    public List<GearOffers.Offer> gearEnchantOffers() {
        return Collections.unmodifiableList(gearEnchantOffers);
    }

    public GearOffers.Offer findGearEnchantOffer(String shopId, ItemStack stack) {
        return GearOffers.findOffer(gearEnchantOffers, shopId, stack);
    }

    public void rollGearEnchantOffers(RandomSource random) {
        gearEnchantOffers.clear();
        Random javaRandom = new Random(random.nextLong());
        List<Item> all = PriceCatalog.collectTradeableItems();
        for (PresetShop shop : PresetShops.ALL) {
            if (!GearOffers.isGearShop(shop.id())) {
                continue;
            }
            List<Item> catalog = new ArrayList<>();
            for (Item item : all) {
                if (shopSells(shop.id(), item)) {
                    catalog.add(item);
                }
            }
            gearEnchantOffers.addAll(GearOffers.rollForShop(
                    shop.id(),
                    catalog,
                    item -> Math.max(1, getShopBaseBuyPrice(shop.id(), item)),
                    javaRandom));
        }
    }

    public void rollActivities(RandomSource random) {
        hotItems.clear();
        saleShops.clear();
        forceSaleReroll = false;
        List<Item> pool = PriceCatalog.collectTradeableItems();
        if (pool.isEmpty()) {
            return;
        }
        int hotCount = 1 + random.nextInt(Math.min(3, pool.size()));
        pickUnique(pool, hotCount, hotItems, random);
        ShopShelfPlanner.Assignment shelves = ShopOffers.current();
        List<String> keys = new java.util.ArrayList<>();
        for (Item item : pool) {
            keys.add(ShopOffers.itemKey(item));
        }
        int distinct = Math.min(pool.size(), 5 + random.nextInt(4));
        List<SaleRollMath.Listing> listings = SaleRollMath.roll(
                keys,
                key -> new java.util.ArrayList<>(shelves.shopsFor(key)),
                (key, shopId) -> {
                    Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(key));
                    if (item == null || item == Items.AIR) {
                        return 1;
                    }
                    return getShopBaseBuyPrice(shopId, item);
                },
                distinct,
                n -> n <= 0 ? 0 : random.nextInt(n)
        );
        for (SaleRollMath.Listing listing : listings) {
            Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(listing.itemKey()));
            if (item == null || item == Items.AIR) {
                continue;
            }
            putSale(item, listing.shopId(), listing.percentOff());
        }
        sanitizeSaleShops();
    }

    private void putSale(Item item, String shopId, int percentOff) {
        if (shopId == null || shopId.isEmpty()) {
            return;
        }
        int percent = Math.max(10, Math.min(99, percentOff));
        Map<String, Integer> shops = saleShops.computeIfAbsent(item, key -> new HashMap<>());
        if (shops.size() >= SaleRollMath.MAX_SHOPS_PER_ITEM && !shops.containsKey(shopId)) {
            return;
        }
        shops.put(shopId, percent);
    }

    /** Trim corrupt/legacy saves where one item was listed in more than three shops. */
    private void sanitizeSaleShops() {
        for (Map.Entry<Item, Map<String, Integer>> entry : saleShops.entrySet()) {
            Map<String, Integer> shops = entry.getValue();
            if (shops == null || shops.size() <= SaleRollMath.MAX_SHOPS_PER_ITEM) {
                continue;
            }
            java.util.ArrayList<Map.Entry<String, Integer>> ranked =
                    new java.util.ArrayList<>(shops.entrySet());
            ranked.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
            shops.clear();
            for (int i = 0; i < SaleRollMath.MAX_SHOPS_PER_ITEM && i < ranked.size(); i++) {
                Map.Entry<String, Integer> pick = ranked.get(i);
                shops.put(pick.getKey(), pick.getValue());
            }
        }
    }

    private static void pickUnique(List<Item> pool, int count, Set<Item> target, RandomSource random) {
        java.util.ArrayList<Item> copy = new java.util.ArrayList<>(pool);
        for (int i = 0; i < count && !copy.isEmpty(); i++) {
            target.add(copy.remove(random.nextInt(copy.size())));
        }
    }

    public void applyEndOfDayAdjustments() {
        int volumeUnit = 256;
        double maxShift = 0.5D;
        double priceAnchor = 128.0D;
        double minWeight = 0.2D;
        double recover = 0.15D;
        double minRatio = 0.5D;
        double maxRatio = 2.0D;
        try {
            volumeUnit = SellVegConfig.MARKET_VOLUME_UNIT.get();
            maxShift = SellVegConfig.MARKET_MAX_SHIFT_RATIO.get();
            priceAnchor = SellVegConfig.MARKET_VOLUME_PRICE_ANCHOR.get();
            minWeight = SellVegConfig.MARKET_VOLUME_WEIGHT_MIN.get();
            recover = SellVegConfig.MARKET_IDLE_RECOVER.get();
            minRatio = SellVegConfig.MARKET_MIN_RATIO.get();
            maxRatio = SellVegConfig.MARKET_MAX_RATIO.get();
        } catch (Exception ignored) {
        }

        for (Item item : PriceCatalog.collectTradeableItems()) {
            int[] orig = PriceCatalog.marketBand(item);
            int[] live = getLiveBand(item);
            if (orig == null || live == null) {
                continue;
            }
            int sold = soldToday.getOrDefault(item, 0);
            int bought = boughtToday.getOrDefault(item, 0);
            int[] next = sold == bought
                    ? PriceMath.recoverBand(live[0], live[1], orig[0], orig[1], recover)
                    : PriceMath.shiftBand(live[0], live[1], orig[0], orig[1], sold, bought,
                    volumeUnit, maxShift, priceAnchor, minWeight);
            next = PriceMath.clampBandToOrigRatios(next[0], next[1], orig[0], orig[1], minRatio, maxRatio);
            int fallback = (live[0] + live[1]) / 2;
            int ref = referencePrices.getOrDefault(item, fallback);
            referencePrices.put(item, PriceMath.slideReference(ref, live[0], live[1], next[0], next[1]));
            bandLo.put(item, next[0]);
            bandHi.put(item, next[1]);
        }
        soldToday.clear();
        boughtToday.clear();
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong(KEY_DAY, dayIndex);
        tag.putLong(KEY_SETTLED, lastSettledWorldDay);
        tag.putBoolean(KEY_INIT, initialized);
        tag.putInt(KEY_PRICING_RULES, pricingRulesVersion);
        tag.put(KEY_PRICES, saveItemIntMap(referencePrices));
        tag.put(KEY_BAND_LO, saveItemIntMap(bandLo));
        tag.put(KEY_BAND_HI, saveItemIntMap(bandHi));
        tag.put(KEY_SOLD, saveItemIntMap(soldToday));
        tag.put(KEY_BOUGHT, saveItemIntMap(boughtToday));
        tag.putInt(KEY_SALE_BOUGHT, saleBoughtToday);
        tag.put("Hot", saveItemSet(hotItems));
        tag.put("Sale", saveSaleListings());
        tag.put("GearEnchants", GearOffers.saveOffers(gearEnchantOffers));
        return tag;
    }

    public void load(CompoundTag tag) {
        dayIndex = tag.getLong(KEY_DAY);
        lastSettledWorldDay = tag.contains(KEY_SETTLED) ? tag.getLong(KEY_SETTLED) : -1L;
        initialized = tag.getBoolean(KEY_INIT);
        pricingRulesVersion = tag.contains(KEY_PRICING_RULES) ? tag.getInt(KEY_PRICING_RULES) : 0;
        referencePrices.clear();
        bandLo.clear();
        bandHi.clear();
        soldToday.clear();
        boughtToday.clear();
        saleBoughtToday = 0;
        loadItemIntMap(tag.getList(KEY_PRICES, Tag.TAG_COMPOUND), referencePrices);
        loadItemIntMap(tag.getList(KEY_BAND_LO, Tag.TAG_COMPOUND), bandLo);
        loadItemIntMap(tag.getList(KEY_BAND_HI, Tag.TAG_COMPOUND), bandHi);
        loadItemIntMap(tag.getList(KEY_SOLD, Tag.TAG_COMPOUND), soldToday);
        loadItemIntMap(tag.getList(KEY_BOUGHT, Tag.TAG_COMPOUND), boughtToday);
        saleBoughtToday = tag.contains(KEY_SALE_BOUGHT) ? tag.getInt(KEY_SALE_BOUGHT) : 0;
        hotItems.clear();
        saleShops.clear();
        gearEnchantOffers.clear();
        forceSaleReroll = false;
        loadItemSet(tag.getList("Hot", Tag.TAG_COMPOUND), hotItems);
        loadSaleListings(tag.getList("Sale", Tag.TAG_COMPOUND));
        if (tag.contains("GearEnchants", Tag.TAG_LIST)) {
            gearEnchantOffers.addAll(GearOffers.loadOffers(tag.getList("GearEnchants", Tag.TAG_COMPOUND)));
        }
        if (!initialized) {
            initialized = !referencePrices.isEmpty() || !hotItems.isEmpty() || !saleShops.isEmpty();
        }
        sanitizeSaleShops();
    }

    private static ListTag saveItemIntMap(Map<Item, Integer> map) {
        ListTag list = new ListTag();
        for (Map.Entry<Item, Integer> entry : map.entrySet()) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(entry.getKey());
            if (id == null) {
                continue;
            }
            CompoundTag itemTag = new CompoundTag();
            itemTag.putString("Id", id.toString());
            itemTag.putInt("Value", entry.getValue());
            list.add(itemTag);
        }
        return list;
    }

    private static void loadItemIntMap(ListTag list, Map<Item, Integer> target) {
        if (list == null) {
            return;
        }
        for (Tag raw : list) {
            CompoundTag entry = (CompoundTag) raw;
            ResourceLocation id = new ResourceLocation(entry.getString("Id"));
            Item item = BuiltInRegistries.ITEM.get(id);
            if (item != null) {
                target.put(item, entry.getInt("Value"));
            }
        }
    }

    private ListTag saveSaleListings() {
        ListTag list = new ListTag();
        for (Map.Entry<Item, Map<String, Integer>> entry : saleShops.entrySet()) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(entry.getKey());
            if (id == null || entry.getValue() == null) {
                continue;
            }
            for (Map.Entry<String, Integer> shop : entry.getValue().entrySet()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putString("Id", id.toString());
                itemTag.putString("Shop", shop.getKey());
                itemTag.putInt("Percent", shop.getValue());
                list.add(itemTag);
            }
        }
        return list;
    }

    private void loadSaleListings(ListTag list) {
        if (list == null) {
            return;
        }
        for (Tag raw : list) {
            CompoundTag entry = (CompoundTag) raw;
            Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(entry.getString("Id")));
            if (item == null || item == Items.AIR) {
                continue;
            }
            String shop = entry.getString("Shop");
            boolean hasPercent = entry.contains("Percent");
            if (!hasPercent) {
                forceSaleReroll = true;
            }
            int percent = hasPercent ? entry.getInt("Percent") : 20;
            percent = Math.max(1, Math.min(99, percent));
            if (shop.isEmpty()) {
                forceSaleReroll = true;
                continue;
            }
            putSale(item, shop, percent);
        }
        sanitizeSaleShops();
    }

    private static ListTag saveItemSet(Set<Item> items) {
        ListTag list = new ListTag();
        for (Item item : items) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (id == null) {
                continue;
            }
            CompoundTag itemTag = new CompoundTag();
            itemTag.putString("Id", id.toString());
            list.add(itemTag);
        }
        return list;
    }

    private static void loadItemSet(ListTag list, Set<Item> target) {
        if (list == null) {
            return;
        }
        for (Tag raw : list) {
            CompoundTag entry = (CompoundTag) raw;
            Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(entry.getString("Id")));
            if (item != null) {
                target.add(item);
            }
        }
    }
}
