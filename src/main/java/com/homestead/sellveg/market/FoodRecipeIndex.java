package com.homestead.sellveg.market;

import com.homestead.sellveg.shop.ShopOffers;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds food material sums from the server recipe manager.
 */
public final class FoodRecipeIndex {
    private static final Logger LOGGER = LoggerFactory.getLogger(FoodRecipeIndex.class);
    private static RecipeManager recipes;
    private static RegistryAccess access;
    private static final Map<Item, List<Recipe<?>>> byResult = new IdentityHashMap<>();
    private static final Map<String, List<Recipe<?>>> byOutputFluid = new HashMap<>();
    private static final Map<Item, Integer> priceCache = new IdentityHashMap<>();
    private static final Map<Item, Integer> craftCostCache = new IdentityHashMap<>();
    private static final Map<Item, Map<Item, Boolean>> luxuryCraftCache = new IdentityHashMap<>();
    private static final Map<String, Double> fluidCostPerMb = new HashMap<>();
    private static final Map<Item, BottleFill> bottleFills = new IdentityHashMap<>();
    /** Catalog prices already established; reused as ingredient costs by later recipes. */
    private static final Map<Item, Integer> establishedPriceCache = new IdentityHashMap<>();
    /** Guards against re-entering price establishment for the same item. */
    private static final Set<Item> establishingPrice =
            java.util.Collections.newSetFromMap(new IdentityHashMap<>());

    private FoodRecipeIndex() {
    }

    /** Drop recipe manager refs so integrated/reload servers can GC. */
    public static void clear() {
        recipes = null;
        access = null;
        byResult.clear();
        byOutputFluid.clear();
        priceCache.clear();
        craftCostCache.clear();
        luxuryCraftCache.clear();
        fluidCostPerMb.clear();
        bottleFills.clear();
        establishedPriceCache.clear();
        establishingPrice.clear();
        PriceCatalog.invalidateTradeableCache();
    }

    public static void rebuild(MinecraftServer server) {
        recipes = server.getRecipeManager();
        access = server.registryAccess();
        byResult.clear();
        byOutputFluid.clear();
        priceCache.clear();
        craftCostCache.clear();
        luxuryCraftCache.clear();
        fluidCostPerMb.clear();
        bottleFills.clear();
        establishedPriceCache.clear();
        establishingPrice.clear();
        if (recipes == null || access == null) {
            PriceCatalog.invalidateTradeableCache();
            return;
        }
        for (Recipe<?> recipe : recipes.getRecipes()) {
            try {
                ItemStack result = recipe.getResultItem(access);
                if (!result.isEmpty()) {
                    byResult.computeIfAbsent(result.getItem(), key -> new ArrayList<>()).add(recipe);
                } else {
                    for (ItemStack extra : RecipeFieldBags.extraOutputs(recipe)) {
                        if (!extra.isEmpty() && PriceMath.indexExtraOutput(true, false)) {
                            byResult.computeIfAbsent(extra.getItem(), key -> new ArrayList<>()).add(recipe);
                        }
                    }
                }
                for (FluidStack fluid : RecipeFieldBags.outputFluids(recipe)) {
                    String id = fluidId(fluid);
                    if (id != null) {
                        byOutputFluid.computeIfAbsent(id, key -> new ArrayList<>()).add(recipe);
                    }
                }
            } catch (Exception ignored) {
            }
        }
        linkFluidBottles();
        PriceCatalog.invalidateTradeableCache();
        ShopOffers.invalidate();
        String fingerprint = currentFingerprint();
        PriceCacheSavedData store = PriceCacheSavedData.get(server);
        boolean cacheHit = store.snapshot().matches(fingerprint) && !store.snapshot().tradeable().isEmpty();
        long started = System.nanoTime();
        if (cacheHit) {
            applySnapshot(store.snapshot());
            ensureCatalogWarm();
            store.replace(dumpSnapshot(fingerprint));
            LOGGER.info("Loaded homestead sell veg price cache ({} prices, {} crafts, {} tradeable) in {} ms",
                    priceCache.size(), craftCostCache.size(), PriceCatalog.cachedTradeableOrEmpty().size(),
                    (System.nanoTime() - started) / 1_000_000L);
            return;
        }
        ensureCatalogWarm();
        store.replace(dumpSnapshot(fingerprint));
        LOGGER.info("Rebuilt homestead sell veg price cache ({} prices, {} crafts, {} tradeable) in {} ms",
                priceCache.size(), craftCostCache.size(), PriceCatalog.cachedTradeableOrEmpty().size(),
                (System.nanoTime() - started) / 1_000_000L);
    }

    private static void ensureCatalogWarm() {
        PriceCatalog.warmupTradeableItems();
        warmupCraftCosts();
        ShopOffers.warmup();
    }


    private static String currentFingerprint() {
        List<String> mods = new ArrayList<>();
        ModList.get().getMods().forEach(mod -> mods.add(mod.getModId() + "@" + mod.getVersion()));
        Collections.sort(mods);
        List<String> recipeIds = new ArrayList<>();
        for (Recipe<?> recipe : recipes.getRecipes()) {
            recipeIds.add(recipe.getId().toString());
        }
        Collections.sort(recipeIds);
        return PriceCacheSnapshot.fingerprint(mods, recipeIds.size(), PriceCacheSnapshot.recipeHash(recipeIds));
    }

    private static void applySnapshot(PriceCacheSnapshot snapshot) {
        priceCache.clear();
        for (Map.Entry<String, Integer> entry : snapshot.prices().entrySet()) {
            Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(entry.getKey()));
            if (item != Items.AIR) {
                priceCache.put(item, entry.getValue());
            }
        }
        List<Item> tradeable = new ArrayList<>();
        for (String id : snapshot.tradeable()) {
            Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(id));
            if (item != Items.AIR) {
                tradeable.add(item);
            }
        }
        PriceCatalog.restoreTradeable(tradeable);
        for (Map.Entry<String, Integer> entry : snapshot.crafts().entrySet()) {
            Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(entry.getKey()));
            if (item != Items.AIR) {
                craftCostCache.put(item, entry.getValue());
            }
        }
    }

    private static void warmupCraftCosts() {
        for (Item item : PriceCatalog.cachedTradeableOrEmpty()) {
            cheapestCraftCostPerResult(item);
        }
    }

    private static PriceCacheSnapshot dumpSnapshot(String fingerprint) {
        Map<String, Integer> prices = new LinkedHashMap<>();
        for (Map.Entry<Item, Integer> entry : priceCache.entrySet()) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(entry.getKey());
            if (id != null) {
                prices.put(id.toString(), entry.getValue());
            }
        }
        List<String> tradeable = new ArrayList<>();
        for (Item item : PriceCatalog.cachedTradeableOrEmpty()) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (id != null) {
                tradeable.add(id.toString());
            }
        }
        Map<String, Integer> crafts = new LinkedHashMap<>();
        for (Map.Entry<Item, Integer> entry : craftCostCache.entrySet()) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(entry.getKey());
            if (id != null) {
                crafts.put(id.toString(), entry.getValue());
            }
        }
        return new PriceCacheSnapshot(fingerprint, prices, tradeable, crafts);
    }

    public static int cachedFoodPriceOrZero(Item item) {
        Integer cached = priceCache.get(item);
        return cached == null ? 0 : cached;
    }

    public static int priceFor(Item item) {
        Integer cached = priceCache.get(item);
        if (cached != null) {
            return cached;
        }
        int priced = priceFor(item, new HashSet<>(), new HashSet<>());
        priceCache.put(item, priced);
        return priced;
    }

    /** Nested pricing keeps the same visit stack so cyclic recipes cannot overflow. */
    static int priceFor(Item item, Set<Item> visiting, Set<String> visitingFluids) {
        Integer cached = priceCache.get(item);
        if (cached != null) {
            return cached;
        }
        if (visiting.size() >= PriceMath.MAX_CRAFT_NESTING) {
            return 0;
        }
        if (!visiting.add(item)) {
            return 0;
        }
        FoodProperties food = item.getFoodProperties();
        if (food == null) {
            int material = PriceCatalog.materialBasePrice(item, visiting, visitingFluids);
            visiting.remove(item);
            priceCache.put(item, material);
            return material;
        }
        double hunger = food.getNutrition();
        double saturation = food.getSaturationModifier() * food.getNutrition();
        int buffs = 0;
        int debuffs = 0;
        for (var pair : food.getEffects()) {
            MobEffectInstance inst = pair.getFirst();
            if (inst == null) {
                continue;
            }
            MobEffect effect = inst.getEffect();
            if (effect.isBeneficial()) {
                buffs++;
            } else {
                debuffs++;
            }
        }

        List<Recipe<?>> matches = recipesFor(item);
        int best = Integer.MAX_VALUE;
        for (Recipe<?> recipe : matches) {
            if (skipRecipeForPricing(recipe, item)) {
                continue;
            }
            MaterialSum sum = sumIngredients(recipe, item, visiting, visitingFluids);
            if (sum.invalid) {
                continue;
            }
            ItemStack vanilla = ItemStack.EMPTY;
            try {
                vanilla = recipe.getResultItem(access == null ? RegistryAccess.EMPTY : access);
            } catch (Exception ignored) {
            }
            int count = RecipeFieldBags.outputCount(recipe, vanilla, new ItemStack(item));
            double perItem = sum.cost / count;
            int priced = FoodPriceMath.compute(perItem, sum.ingredientCount, hunger, saturation, buffs, debuffs);
            best = Math.min(best, priced);
        }
        int bottled = priceFromBottleFill(item, hunger, saturation, buffs, debuffs, visiting, visitingFluids);
        if (bottled > 0) {
            best = Math.min(best, bottled);
        }
        if (best == Integer.MAX_VALUE) {
            best = FoodPriceMath.compute(0, 0, hunger, saturation, buffs, debuffs);
        }
        visiting.remove(item);
        priceCache.put(item, best);
        return best;
    }

    /**
     * Cheapest crafting/cooking ingredient sum per output item, or 0 if none.
     */
    public static int cheapestCraftCostPerResult(Item item) {
        if (recipes == null || access == null || item == null) {
            return 0;
        }
        Integer cached = craftCostCache.get(item);
        if (cached != null) {
            return cached;
        }
        int priced = cheapestCraftCostPerResult(item, new HashSet<>(), new HashSet<>());
        craftCostCache.put(item, priced);
        return priced;
    }

    /** Nested craft costing keeps the same visit stack so cyclic recipes cannot overflow. */
    static int cheapestCraftCostPerResult(Item item, Set<Item> visiting, Set<String> visitingFluids) {
        Integer cached = craftCostCache.get(item);
        if (cached != null) {
            return cached;
        }
        if (visiting.size() >= PriceMath.MAX_CRAFT_NESTING) {
            return 0;
        }
        if (!visiting.add(item)) {
            return 0;
        }
        List<Recipe<?>> matches = recipesFor(item);
        int best = Integer.MAX_VALUE;
        for (Recipe<?> recipe : matches) {
            if (skipRecipeForPricing(recipe, item)) {
                continue;
            }
            MaterialSum sum = sumIngredients(recipe, item, visiting, visitingFluids);
            if (sum.invalid || sum.cost <= 0.0D) {
                continue;
            }
            int count = RecipeFieldBags.outputCount(recipe, recipe.getResultItem(access), new ItemStack(item));
            int perItem = (int) Math.ceil(sum.cost / count);
            best = Math.min(best, perItem);
        }
        visiting.remove(item);
        int priced = best == Integer.MAX_VALUE ? 0 : best;
        craftCostCache.put(item, priced);
        return priced;
    }

    private static List<Recipe<?>> recipesFor(Item item) {
        List<Recipe<?>> list = byResult.get(item);
        return list == null ? List.of() : list;
    }

    private static boolean skipRecipeForPricing(Recipe<?> recipe, Item output) {
        String type = recipeTypeId(recipe);
        boolean edible = output != null && output.getFoodProperties() != null;
        return PriceMath.skipRecipeForPricing(type, edible, ingredientContains(recipe, output));
    }

    private static String recipeTypeId(Recipe<?> recipe) {
        ResourceLocation id = BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType());
        return id != null ? id.toString() : recipe.getClass().getName();
    }

    private static boolean ingredientContains(Recipe<?> recipe, Item output) {
        if (output == null) {
            return false;
        }
        List<Ingredient> ingredients;
        try {
            ingredients = RecipeFieldBags.ingredients(recipe);
        } catch (Exception ignored) {
            return false;
        }
        for (Ingredient ingredient : ingredients) {
            if (ingredient.isEmpty()) {
                continue;
            }
            ItemStack[] options;
            try {
                options = ingredient.getItems();
            } catch (Exception ignored) {
                continue;
            }
            for (ItemStack option : options) {
                if (!option.isEmpty() && option.getItem() == output) {
                    return true;
                }
            }
        }
        return false;
    }

    private static MaterialSum sumIngredients(Recipe<?> recipe, Item output, Set<Item> visiting, Set<String> visitingFluids) {
        MaterialSum sum = new MaterialSum();
        List<Ingredient> ingredients;
        try {
            ingredients = RecipeFieldBags.ingredients(recipe);
        } catch (Exception ignored) {
            sum.invalid = true;
            return sum;
        }
        List<SlotQuote> slots = new ArrayList<>();
        for (Ingredient ingredient : ingredients) {
            if (ingredient.isEmpty()) {
                continue;
            }
            ItemStack[] options;
            try {
                options = ingredient.getItems();
            } catch (Exception ignored) {
                options = new ItemStack[0];
            }
            if (options.length == 0) {
                continue;
            }
            SlotQuote quote = quoteSlot(options, output, visiting, visitingFluids);
            if (quote.cost > 0) {
                slots.add(quote);
            }
        }
        for (int i = 0; i < slots.size(); i++) {
            SlotQuote quote = slots.get(i);
            if (PriceMath.allowLuxurySlotSkip(output != null && output.getFoodProperties() != null)
                    && isLuxurySubstituteSlot(quote, slots, i)) {
                sum.luxurySkipped++;
                continue;
            }
            sum.cost += quote.cost;
            sum.ingredientCount++;
        }
        for (FluidStack fluid : RecipeFieldBags.inputFluids(recipe)) {
            String id = fluidId(fluid);
            if (isFreeFluid(id)) {
                continue;
            }
            double cost = fluidCost(fluid, visiting, visitingFluids);
            if (FluidShareMath.incompleteFluid(fluid.getAmount(), cost)) {
                sum.invalid = true;
                return sum;
            }
            if (cost <= 0.0D) {
                continue;
            }
            sum.cost += cost;
            sum.ingredientCount++;
        }
        if (sum.ingredientCount == 0) {
            sum.invalid = true;
        }
        return sum;
    }

    private static double fluidCost(FluidStack fluid, Set<Item> visiting, Set<String> visitingFluids) {
        String id = fluidId(fluid);
        if (id == null || fluid.getAmount() <= 0) {
            return 0.0D;
        }
        return fluidCostPerMb(id, visiting, visitingFluids) * fluid.getAmount();
    }

    private static double fluidCostPerMb(String fluidId, Set<Item> visiting, Set<String> visitingFluids) {
        Double cached = fluidCostPerMb.get(fluidId);
        if (cached != null) {
            return cached;
        }
        if (visitingFluids.size() >= PriceMath.MAX_CRAFT_NESTING) {
            return 0.0D;
        }
        if (!visitingFluids.add(fluidId)) {
            return 0.0D;
        }
        List<Recipe<?>> matches = byOutputFluid.getOrDefault(fluidId, List.of());
        double best = Double.POSITIVE_INFINITY;
        for (Recipe<?> recipe : matches) {
            MaterialSum sum = sumIngredients(recipe, null, visiting, visitingFluids);
            if (sum.invalid || sum.cost <= 0.0D) {
                continue;
            }
            int produced = 0;
            for (FluidStack out : RecipeFieldBags.outputFluids(recipe)) {
                if (fluidId.equals(fluidId(out))) {
                    produced += Math.max(0, out.getAmount());
                }
            }
            if (produced <= 0) {
                continue;
            }
            best = Math.min(best, sum.cost / produced);
        }
        visitingFluids.remove(fluidId);
        double priced = Double.isInfinite(best) ? 0.0D : best;
        fluidCostPerMb.put(fluidId, priced);
        return priced;
    }

    private static String fluidId(FluidStack fluid) {
        if (fluid == null || fluid.isEmpty()) {
            return null;
        }
        ResourceLocation id = ForgeRegistries.FLUIDS.getKey(fluid.getFluid());
        if (id == null || "empty".equals(id.getPath())) {
            return null;
        }
        return id.toString();
    }

    private static boolean isFreeFluid(String id) {
        return id == null
                || id.equals("minecraft:water")
                || id.equals("minecraft:lava")
                || id.endsWith(":empty");
    }

    private static int nestedPrice(Item item, Set<Item> visiting, Set<String> visitingFluids) {
        if (item.getFoodProperties() != null) {
            return priceFor(item, visiting, visitingFluids);
        }
        // Reuse a price this item already established instead of re-deriving it from its recipe.
        Integer established = establishedPriceCache.get(item);
        if (established != null) {
            return established;
        }
        if (!visiting.contains(item) && !establishingPrice.contains(item)) {
            establishingPrice.add(item);
            int catalogPrice;
            try {
                catalogPrice = PriceCatalog.getBasePrice(item);
            } finally {
                establishingPrice.remove(item);
            }
            if (catalogPrice > 0) {
                establishedPriceCache.put(item, catalogPrice);
                return catalogPrice;
            }
        }
        int catalog = PriceCatalog.materialBasePrice(item, visiting, visitingFluids);
        int craft = catalog > 0 ? 0 : cheapestCraftCostPerResult(item, visiting, visitingFluids);
        return PriceMath.nestedMaterialOrCraft(catalog, craft);
    }

    private static SlotQuote quoteSlot(ItemStack[] options, Item output, Set<Item> visiting, Set<String> visitingFluids) {
        List<Item> items = new ArrayList<>();
        List<ItemStack> unresolved = new ArrayList<>();
        List<Integer> knownCosts = new ArrayList<>();
        int bestKnown = Integer.MAX_VALUE;
        Item bestKnownItem = null;
        for (ItemStack option : options) {
            if (option.isEmpty() || option.getItem() == output) {
                continue;
            }
            Item item = option.getItem();
            items.add(item);
            int count = Math.max(1, option.getCount());
            int listed = PriceCatalog.listedBaseOnly(item);
            if (listed > 0) {
                int cost = listed * count;
                knownCosts.add(cost);
                if (cost < bestKnown) {
                    bestKnown = cost;
                    bestKnownItem = item;
                }
            } else {
                unresolved.add(option);
            }
        }
        if (!knownCosts.isEmpty()) {
            int[] known = new int[knownCosts.size()];
            for (int i = 0; i < knownCosts.size(); i++) {
                known[i] = knownCosts.get(i);
            }
            return new SlotQuote(PriceMath.cheapestSlotPreferKnown(known, new int[0]), bestKnownItem, items);
        }

        List<ItemStack> expand = prioritizeTagExpand(unresolved);
        List<Integer> resolvedCosts = new ArrayList<>();
        List<Integer> unknownCosts = new ArrayList<>();
        int bestResolved = Integer.MAX_VALUE;
        int bestUnknown = Integer.MAX_VALUE;
        Item bestResolvedItem = null;
        Item bestUnknownItem = null;
        for (ItemStack option : expand) {
            Item item = option.getItem();
            int count = Math.max(1, option.getCount());
            int catalog = nestedPrice(item, visiting, visitingFluids);
            if (catalog > 0) {
                int cost = catalog * count;
                resolvedCosts.add(cost);
                if (cost < bestResolved) {
                    bestResolved = cost;
                    bestResolvedItem = item;
                }
            } else {
                int cost = PriceMath.craftMaterialUnit(0) * count;
                unknownCosts.add(cost);
                if (cost < bestUnknown) {
                    bestUnknown = cost;
                    bestUnknownItem = item;
                }
            }
        }
        int[] known = new int[resolvedCosts.size()];
        for (int i = 0; i < resolvedCosts.size(); i++) {
            known[i] = resolvedCosts.get(i);
        }
        int[] unknown = new int[unknownCosts.size()];
        for (int i = 0; i < unknownCosts.size(); i++) {
            unknown[i] = unknownCosts.get(i);
        }
        int slotCost = PriceMath.cheapestSlotPreferKnown(known, unknown);
        Item bestItem = bestResolvedItem != null ? bestResolvedItem : bestUnknownItem;
        return new SlotQuote(slotCost, bestItem, items);
    }

    /** Prefer vanilla ids, then cap how many unpriced tag members get full craft expansion. */
    private static List<ItemStack> prioritizeTagExpand(List<ItemStack> unresolved) {
        if (unresolved.size() <= PriceMath.TAG_CRAFT_EXPAND_LIMIT) {
            return unresolved;
        }
        List<ItemStack> vanilla = new ArrayList<>();
        List<ItemStack> other = new ArrayList<>();
        for (ItemStack stack : unresolved) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (id != null && "minecraft".equals(id.getNamespace())) {
                vanilla.add(stack);
            } else {
                other.add(stack);
            }
        }
        List<ItemStack> picked = new ArrayList<>(PriceMath.TAG_CRAFT_EXPAND_LIMIT);
        for (ItemStack stack : vanilla) {
            if (picked.size() >= PriceMath.TAG_CRAFT_EXPAND_LIMIT) {
                break;
            }
            picked.add(stack);
        }
        for (ItemStack stack : other) {
            if (picked.size() >= PriceMath.TAG_CRAFT_EXPAND_LIMIT) {
                break;
            }
            picked.add(stack);
        }
        return picked;
    }

    private static boolean isLuxurySubstituteSlot(SlotQuote quote, List<SlotQuote> slots, int index) {
        if (quote.cheapestItem == null) {
            return false;
        }
        for (int i = 0; i < slots.size(); i++) {
            if (i == index) {
                continue;
            }
            boolean hasBase = false;
            for (Item base : slots.get(i).options) {
                if (base == quote.cheapestItem) {
                    continue;
                }
                if (isLuxuryCraftOf(quote.cheapestItem, base)) {
                    hasBase = true;
                    break;
                }
            }
            if (LuxurySubstituteMath.skipLuxurySlot(hasBase, hasBase)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isLuxuryCraftOf(Item expensive, Item base) {
        if (expensive == null || base == null || expensive == base || recipes == null) {
            return false;
        }
        Map<Item, Boolean> byBase = luxuryCraftCache.get(expensive);
        if (byBase != null && byBase.containsKey(base)) {
            return byBase.get(base);
        }
        for (Recipe<?> recipe : recipesFor(expensive)) {
            List<Ingredient> ings;
            try {
                ings = RecipeFieldBags.ingredients(recipe);
            } catch (Exception ignored) {
                continue;
            }
            boolean sawBase = false;
            boolean extrasPreciousOnly = true;
            for (Ingredient ingredient : ings) {
                if (ingredient.isEmpty()) {
                    continue;
                }
                ItemStack[] options;
                try {
                    options = ingredient.getItems();
                } catch (Exception ignored) {
                    options = new ItemStack[0];
                }
                boolean slotBase = false;
                boolean slotPrecious = false;
                for (ItemStack option : options) {
                    if (option.isEmpty()) {
                        continue;
                    }
                    if (option.getItem() == base) {
                        slotBase = true;
                    }
                    ResourceLocation id = BuiltInRegistries.ITEM.getKey(option.getItem());
                    if (id != null && LuxurySubstituteMath.isPreciousMaterialPath(id.getPath())) {
                        slotPrecious = true;
                    }
                }
                if (slotBase) {
                    sawBase = true;
                } else if (!slotPrecious) {
                    extrasPreciousOnly = false;
                    break;
                }
            }
            if (sawBase && extrasPreciousOnly) {
                rememberLuxury(expensive, base, true);
                return true;
            }
        }
        rememberLuxury(expensive, base, false);
        return false;
    }

    private static void rememberLuxury(Item expensive, Item base, boolean value) {
        luxuryCraftCache.computeIfAbsent(expensive, key -> new IdentityHashMap<>()).put(base, value);
    }

    private static void linkFluidBottles() {
        Item sake = BuiltInRegistries.ITEM.get(new ResourceLocation("youkaishomecoming", "sake_bottle"));
        if (sake == Items.AIR) {
            sake = Items.GLASS_BOTTLE;
        }
        for (String fluidId : byOutputFluid.keySet()) {
            ResourceLocation fid = new ResourceLocation(fluidId);
            Item pour = BuiltInRegistries.ITEM.get(fid);
            if (pour != Items.AIR) {
                bottleFills.put(pour, new BottleFill(fluidId, 250, Items.GLASS_BOTTLE));
            }
            Item flask = BuiltInRegistries.ITEM.get(new ResourceLocation(fid.getNamespace(), fid.getPath() + "_flask"));
            if (flask != Items.AIR) {
                bottleFills.put(flask, new BottleFill(fluidId, 1000, sake));
            }
            Item magnum = BuiltInRegistries.ITEM.get(new ResourceLocation(fid.getNamespace(), fid.getPath() + "_bottle"));
            if (magnum != Items.AIR) {
                boolean hasPour = pour != Items.AIR;
                bottleFills.put(magnum, new BottleFill(fluidId, hasPour ? 1000 : 250, hasPour ? sake : Items.GLASS_BOTTLE));
            }
        }
    }

    private static int priceFromBottleFill(
            Item item,
            double hunger,
            double saturation,
            int buffs,
            int debuffs,
            Set<Item> visiting,
            Set<String> visitingFluids
    ) {
        BottleFill fill = bottleFills.get(item);
        if (fill == null) {
            return 0;
        }
        double fluid = fluidCostPerMb(fill.fluidId, visiting, visitingFluids) * fill.mb;
        if (fluid <= 0.0D) {
            return 0;
        }
        int container = PriceMath.craftMaterialUnit(nestedPrice(fill.container, visiting, visitingFluids));
        double cost = fluid + container;
        return FoodPriceMath.compute(cost, 2, hunger, saturation, buffs, debuffs);
    }

    private static final class BottleFill {
        private final String fluidId;
        private final int mb;
        private final Item container;

        private BottleFill(String fluidId, int mb, Item container) {
            this.fluidId = fluidId;
            this.mb = mb;
            this.container = container;
        }
    }

    private static final class SlotQuote {
        private final int cost;
        private final Item cheapestItem;
        private final List<Item> options;

        private SlotQuote(int cost, Item cheapestItem, List<Item> options) {
            this.cost = cost;
            this.cheapestItem = cheapestItem;
            this.options = options;
        }
    }

    private static final class MaterialSum {
        private double cost;
        private int ingredientCount;
        private boolean invalid;
        private int luxurySkipped;
    }
}
