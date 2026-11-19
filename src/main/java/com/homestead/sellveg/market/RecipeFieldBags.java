package com.homestead.sellveg.market;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.fluids.FluidStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Custom machine recipes keep ingredients/results on public fields
 * ({@code ingredients}, {@code input}, {@code first}/{@code second}) instead of
 * {@link Recipe#getIngredients()} / {@link Recipe#getResultItem}.
 */
public final class RecipeFieldBags {
    private static final Set<String> INGREDIENT_FIELDS = Set.of("ingredients", "input", "first", "second");
    /** Smithing recipes keep template/base/addition as Ingredient fields and may expose no getIngredients(). */
    private static final String SMITHING_CLASS_TOKEN = "Smithing";
    private static final Set<String> ITEM_RESULT_FIELDS = Set.of("results", "result", "defaultbottle");

    private RecipeFieldBags() {
    }

    public static List<Ingredient> ingredients(Recipe<?> recipe) {
        List<Ingredient> listed = safeGetIngredients(recipe);
        if (!listed.isEmpty()) {
            return listed;
        }
        LinkedHashSet<Ingredient> unique = new LinkedHashSet<>();
        invokeIngredientList(recipe, "getInput", unique);
        invokeIngredientList(recipe, "getCustomIngredients", unique);
        collectIngredientFields(recipe, unique);
        collectSmithingIngredientFields(recipe, unique);
        return new ArrayList<>(unique);
    }

    public static List<String> ingredientBagNames(Object recipe) {
        List<String> names = new ArrayList<>();
        Class<?> cursor = recipe.getClass();
        while (cursor != null && cursor != Object.class) {
            for (Field field : cursor.getDeclaredFields()) {
                if (INGREDIENT_FIELDS.contains(field.getName())) {
                    names.add(field.getName());
                }
            }
            cursor = cursor.getSuperclass();
        }
        return names;
    }

    public static List<ItemStack> extraOutputs(Object recipe) {
        List<ItemStack> out = new ArrayList<>();
        collectNamedItemStacks(recipe, out);
        collectDeepItemStacks(recipe, out, 0);
        addStacks(invokeNoArg(recipe, "getRollableResults"), out);
        addStacks(invokeNoArg(recipe, "getResults"), out);
        return out;
    }

    public static String debugShape(Object recipe) {
        StringBuilder sb = new StringBuilder();
        sb.append("cls=").append(recipe.getClass().getName());
        Class<?> cursor = recipe.getClass();
        while (cursor != null && cursor != Object.class) {
            for (Field field : cursor.getDeclaredFields()) {
                Object value = read(field, recipe);
                sb.append('|').append(field.getName()).append(':');
                if (value == null) {
                    sb.append("null");
                } else if (value instanceof Collection<?> collection) {
                    sb.append("col").append(collection.size());
                    int n = 0;
                    for (Object entry : collection) {
                        if (entry != null && n++ < 2) {
                            sb.append('/').append(entry.getClass().getSimpleName());
                        }
                    }
                } else {
                    sb.append(value.getClass().getSimpleName());
                }
            }
            cursor = cursor.getSuperclass();
        }
        return sb.length() > 400 ? sb.substring(0, 400) : sb.toString();
    }

    public static int outputCount(Recipe<?> recipe, ItemStack vanillaResult, ItemStack priced) {
        Item target = priced == null || priced.isEmpty()
                ? (vanillaResult == null ? null : vanillaResult.getItem())
                : priced.getItem();
        int vanillaCount = 0;
        if (vanillaResult != null && !vanillaResult.isEmpty()
                && (target == null || vanillaResult.getItem() == target)) {
            vanillaCount = Math.max(1, vanillaResult.getCount());
        }
        int extra = 0;
        for (ItemStack stack : extraOutputs(recipe)) {
            if (target != null && stack.getItem() != target) {
                continue;
            }
            extra += Math.max(1, stack.getCount());
        }
        return PriceMath.resolvingOutputCount(vanillaCount, extra);
    }

    public static List<FluidStack> inputFluids(Object recipe) {
        List<FluidStack> in = new ArrayList<>();
        List<FluidStack> out = new ArrayList<>();
        collectFluids(recipe, in, out);
        collectFluidIngredients(recipe, in);
        return in;
    }

    public static List<FluidStack> outputFluids(Object recipe) {
        List<FluidStack> in = new ArrayList<>();
        List<FluidStack> out = new ArrayList<>();
        collectFluids(recipe, in, out);
        return out;
    }

    private static List<Ingredient> safeGetIngredients(Recipe<?> recipe) {
        try {
            List<Ingredient> listed = recipe.getIngredients();
            return listed == null ? List.of() : listed;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private static void invokeIngredientList(Object recipe, String method, Set<Ingredient> unique) {
        try {
            Method m = recipe.getClass().getMethod(method);
            addIngredients(m.invoke(recipe), unique);
        } catch (Exception ignored) {
        }
    }

    private static void collectIngredientFields(Object recipe, Set<Ingredient> unique) {
        Class<?> cursor = recipe.getClass();
        while (cursor != null && cursor != Object.class) {
            for (Field field : cursor.getDeclaredFields()) {
                if (!INGREDIENT_FIELDS.contains(field.getName())) {
                    continue;
                }
                addIngredients(read(field, recipe), unique);
            }
            cursor = cursor.getSuperclass();
        }
    }

    /**
     * 1.20.1 smithing recipes report no ingredients through {@code getIngredients()}; their
     * template/base/addition live in Ingredient-typed fields. Reading them lets netherite gear
     * (and any smithing-only equipment) resolve a craft cost instead of falling back.
     */
    private static void collectSmithingIngredientFields(Object recipe, Set<Ingredient> unique) {
        if (recipe == null || !recipe.getClass().getName().contains(SMITHING_CLASS_TOKEN)) {
            return;
        }
        Class<?> cursor = recipe.getClass();
        while (cursor != null && cursor != Object.class) {
            for (Field field : cursor.getDeclaredFields()) {
                if (field.getType() != Ingredient.class) {
                    continue;
                }
                Object value = read(field, recipe);
                if (value instanceof Ingredient ingredient) {
                    unique.add(ingredient);
                }
            }
            cursor = cursor.getSuperclass();
        }
    }

    private static void collectNamedItemStacks(Object recipe, List<ItemStack> out) {
        Class<?> cursor = recipe.getClass();
        while (cursor != null && cursor != Object.class) {
            for (Field field : cursor.getDeclaredFields()) {
                if (!ITEM_RESULT_FIELDS.contains(field.getName().toLowerCase(Locale.ROOT))) {
                    continue;
                }
                addStacks(read(field, recipe), out);
            }
            cursor = cursor.getSuperclass();
        }
    }

    private static void collectFluids(Object recipe, List<FluidStack> in, List<FluidStack> out) {
        Class<?> cursor = recipe.getClass();
        while (cursor != null && cursor != Object.class) {
            for (Field field : cursor.getDeclaredFields()) {
                String name = field.getName().toLowerCase(Locale.ROOT);
                Object value = read(field, recipe);
                boolean outputSide = name.contains("output") || name.equals("result") || name.equals("results");
                if (value instanceof FluidStack fluid && !fluid.isEmpty()) {
                    (outputSide ? out : in).add(fluid);
                } else if (value instanceof Collection<?> collection) {
                    for (Object entry : collection) {
                        if (entry instanceof FluidStack fluid && !fluid.isEmpty()) {
                            (outputSide ? out : in).add(fluid);
                        } else {
                            Object nested = invokeNoArg(entry, "getFluid");
                            if (nested instanceof FluidStack fluid && !fluid.isEmpty()) {
                                (outputSide ? out : in).add(fluid);
                            }
                        }
                    }
                }
            }
            cursor = cursor.getSuperclass();
        }
    }

    private static void collectFluidIngredients(Object recipe, List<FluidStack> in) {
        if (recipe instanceof Recipe<?> rec) {
            for (Ingredient ingredient : safeGetIngredients(rec)) {
                ItemStack[] items;
                try {
                    items = ingredient.getItems();
                } catch (Exception ignored) {
                    items = new ItemStack[0];
                }
                if (items.length == 0) {
                    addFluidIngredient(ingredient, in);
                }
            }
        }
        Class<?> cursor = recipe.getClass();
        while (cursor != null && cursor != Object.class) {
            for (Field field : cursor.getDeclaredFields()) {
                String name = field.getName();
                if (!"fluidIngredients".equals(name) && !"fluidIngredient".equals(name)) {
                    continue;
                }
                addFluidIngredient(read(field, recipe), in);
            }
            cursor = cursor.getSuperclass();
        }
    }

    private static void addFluidIngredient(Object value, List<FluidStack> in) {
        if (value == null) {
            return;
        }
        if (value instanceof FluidStack fluid && !fluid.isEmpty()) {
            in.add(fluid);
            return;
        }
        if (value instanceof Collection<?> collection) {
            for (Object entry : collection) {
                addFluidIngredient(entry, in);
            }
            return;
        }
        Object stacks = invokeNoArg(value, "getMatchingFluidStacks");
        if (stacks instanceof Collection<?> collection) {
            for (Object entry : collection) {
                if (entry instanceof FluidStack fluid && !fluid.isEmpty()) {
                    in.add(fluid);
                }
            }
        }
        Object single = invokeNoArg(value, "getFluid");
        if (single instanceof FluidStack fluid && !fluid.isEmpty()) {
            in.add(fluid);
        }
    }

    private static Object invokeNoArg(Object target, String name) {
        try {
            Method method = target.getClass().getMethod(name);
            return method.invoke(target);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void addIngredients(Object value, Set<Ingredient> extra) {
        if (value instanceof Ingredient ingredient && !ingredient.isEmpty()) {
            extra.add(ingredient);
        } else if (value instanceof Collection<?> collection) {
            for (Object entry : collection) {
                if (entry instanceof Ingredient ingredient && !ingredient.isEmpty()) {
                    extra.add(ingredient);
                }
            }
        } else if (value instanceof Ingredient[] array) {
            for (Ingredient ingredient : array) {
                if (ingredient != null && !ingredient.isEmpty()) {
                    extra.add(ingredient);
                }
            }
        }
    }

    private static void addStacks(Object value, List<ItemStack> out) {
        addStacks(value, out, 0);
    }

    private static void addStacks(Object value, List<ItemStack> out, int depth) {
        if (value == null || depth > 3) {
            return;
        }
        if (value instanceof ItemStack stack && !stack.isEmpty()) {
            out.add(stack);
            return;
        }
        if (value instanceof Collection<?> collection) {
            for (Object entry : collection) {
                addStacks(entry, out, depth + 1);
            }
            return;
        }
        Object stacked = invokeNoArg(value, "getStack");
        if (stacked instanceof ItemStack stack && !stack.isEmpty()) {
            out.add(stack);
            return;
        }
        if (depth >= 2) {
            return;
        }
        Class<?> cursor = value.getClass();
        if (cursor.getName().startsWith("net.minecraft") || cursor.getName().startsWith("java.")) {
            return;
        }
        while (cursor != null && cursor != Object.class) {
            for (Field field : cursor.getDeclaredFields()) {
                Object nested = read(field, value);
                if (nested instanceof ItemStack || nested instanceof Collection<?>) {
                    addStacks(nested, out, depth + 1);
                }
            }
            cursor = cursor.getSuperclass();
        }
    }

    private static void collectDeepItemStacks(Object recipe, List<ItemStack> out, int depth) {
        Class<?> cursor = recipe.getClass();
        while (cursor != null && cursor != Object.class) {
            for (Field field : cursor.getDeclaredFields()) {
                String name = field.getName().toLowerCase(Locale.ROOT);
                if (!(name.contains("result") || name.contains("output") || name.equals("stack")
                        || name.contains("bottle"))) {
                    continue;
                }
                addStacks(read(field, recipe), out, depth);
            }
            cursor = cursor.getSuperclass();
        }
    }

    private static Object read(Field field, Object recipe) {
        try {
            field.setAccessible(true);
            return field.get(recipe);
        } catch (Exception ignored) {
            return null;
        }
    }
}
