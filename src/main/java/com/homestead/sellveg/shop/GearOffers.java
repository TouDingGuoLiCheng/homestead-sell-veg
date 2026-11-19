package com.homestead.sellveg.shop;

import com.homestead.sellveg.market.PriceCatalog;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Daily enchanted mall SKUs for weapon/armor shops, plus stack pricing helpers.
 */
public final class GearOffers {
    private static final Set<ResourceLocation> ADVANCED = Set.of(
            new ResourceLocation("minecraft:fire_aspect"),
            new ResourceLocation("minecraft:fire_protection"),
            new ResourceLocation("minecraft:thorns"),
            new ResourceLocation("minecraft:piercing"),
            new ResourceLocation("minecraft:multishot"),
            new ResourceLocation("minecraft:quick_charge"),
            new ResourceLocation("minecraft:punch"),
            new ResourceLocation("minecraft:infinity"),
            new ResourceLocation("minecraft:respiration"),
            new ResourceLocation("minecraft:depth_strider"),
            new ResourceLocation("minecraft:lure"),
            new ResourceLocation("minecraft:luck_of_the_sea")
    );

    public record Offer(String shopId, ItemStack stack, int unitPrice) {
    }

    private GearOffers() {
    }

    public static boolean isGearShop(String shopId) {
        return "iron_bro_forge".equals(shopId) || "scaredy_armor".equals(shopId);
    }

    public static boolean isGearItem(Item item) {
        return PriceCatalog.isWeaponShopExclusive(item) || PriceCatalog.isArmorShopExclusive(item);
    }

    public static double durabilityRatio(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.isDamageableItem()) {
            return 1.0D;
        }
        int max = stack.getMaxDamage();
        if (max <= 0) {
            return 1.0D;
        }
        return Math.max(0.0D, (max - stack.getDamageValue()) / (double) max);
    }

    public static List<GearPricing.EnchantLine> pricingLines(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return List.of();
        }
        Map<Enchantment, Integer> map = EnchantmentHelper.getEnchantments(stack);
        List<GearPricing.EnchantLine> lines = new ArrayList<>();
        for (Map.Entry<Enchantment, Integer> entry : map.entrySet()) {
            Enchantment ench = entry.getKey();
            if (ench == null) {
                continue;
            }
            lines.add(new GearPricing.EnchantLine(tierOf(ench), Math.max(1, entry.getValue()), ench.isCurse()));
        }
        return lines;
    }

    public static int suggestUnitPrice(int blankBase, ItemStack stack) {
        return GearPricing.finalPrice(Math.max(0, blankBase), pricingLines(stack), durabilityRatio(stack));
    }

    public static GearPricing.Tier tierOf(Enchantment ench) {
        if (ench.isCurse()) {
            return GearPricing.Tier.ADVANCED;
        }
        ResourceLocation id = BuiltInRegistries.ENCHANTMENT.getKey(ench);
        if (ench.isTreasureOnly()) {
            return GearPricing.Tier.TREASURE;
        }
        if (id != null && ADVANCED.contains(id)) {
            return GearPricing.Tier.ADVANCED;
        }
        return GearPricing.Tier.COMMON;
    }

    public static List<Offer> rollForShop(String shopId, List<Item> catalog,
            java.util.function.ToIntFunction<Item> blankBase, Random random) {
        if (!isGearShop(shopId) || catalog == null || catalog.isEmpty()) {
            return List.of();
        }
        List<String> keys = new ArrayList<>();
        Map<String, Item> byKey = new HashMap<>();
        for (Item item : catalog) {
            if (item == null || !canReceiveShopEnchant(item)) {
                continue;
            }
            String key = ShopOffers.itemKey(item);
            keys.add(key);
            byKey.put(key, item);
        }
        int target = GearEnchantRoll.pickDailyCount(random);
        List<String> picked = GearEnchantRoll.pickItemKeys(keys, target, random);
        List<Offer> offers = new ArrayList<>();
        for (String key : picked) {
            Item item = byKey.get(key);
            ItemStack stack = rollEnchantedStack(item, random);
            if (stack.isEmpty() || !stack.isEnchanted()) {
                continue;
            }
            int price = suggestUnitPrice(blankBase.applyAsInt(item), stack);
            offers.add(new Offer(shopId, stack, price));
        }
        return offers;
    }

    public static boolean canReceiveShopEnchant(Item item) {
        if (!isGearItem(item)) {
            return false;
        }
        ItemStack probe = new ItemStack(item);
        for (Enchantment ench : BuiltInRegistries.ENCHANTMENT) {
            if (ench != null && !ench.isCurse() && ench.canEnchant(probe)) {
                return true;
            }
        }
        return false;
    }

    public static ItemStack rollEnchantedStack(Item item, Random random) {
        ItemStack stack = new ItemStack(item);
        List<Enchantment> pool = eligiblePool(stack);
        if (pool.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int want = GearEnchantRoll.pickEnchantCount(random);
        Collections.shuffle(pool, random);
        Map<Enchantment, Integer> chosen = new HashMap<>();
        for (Enchantment ench : pool) {
            if (chosen.size() >= want) {
                break;
            }
            if (!compatibleWithAll(ench, chosen.keySet())) {
                continue;
            }
            int max = Math.max(1, ench.getMaxLevel());
            int level = 1 + random.nextInt(max);
            chosen.put(ench, level);
        }
        if (chosen.isEmpty()) {
            return ItemStack.EMPTY;
        }
        EnchantmentHelper.setEnchantments(chosen, stack);
        return stack;
    }

    private static List<Enchantment> eligiblePool(ItemStack stack) {
        List<Enchantment> pool = new ArrayList<>();
        for (Enchantment ench : BuiltInRegistries.ENCHANTMENT) {
            if (ench == null || ench.isCurse()) {
                continue;
            }
            if (ench.canEnchant(stack)) {
                pool.add(ench);
            }
        }
        return pool;
    }

    private static boolean compatibleWithAll(Enchantment candidate, Set<Enchantment> existing) {
        for (Enchantment other : existing) {
            if (!candidate.isCompatibleWith(other)) {
                return false;
            }
        }
        return true;
    }

    public static String offerSignature(String shopId, ItemStack stack) {
        StringBuilder sb = new StringBuilder(shopId == null ? "" : shopId);
        sb.append('|').append(ShopOffers.itemKey(stack.getItem()));
        Map<Enchantment, Integer> map = EnchantmentHelper.getEnchantments(stack);
        List<String> parts = new ArrayList<>();
        for (Map.Entry<Enchantment, Integer> e : map.entrySet()) {
            ResourceLocation id = BuiltInRegistries.ENCHANTMENT.getKey(e.getKey());
            if (id != null) {
                parts.add(id + "#" + e.getValue());
            }
        }
        Collections.sort(parts);
        for (String part : parts) {
            sb.append('|').append(part);
        }
        return sb.toString();
    }

    public static boolean sameOffer(ItemStack a, ItemStack b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) {
            return false;
        }
        if (a.getItem() != b.getItem()) {
            return false;
        }
        return EnchantmentHelper.getEnchantments(a).equals(EnchantmentHelper.getEnchantments(b));
    }

    public static CompoundTag saveOfferStack(ItemStack stack) {
        CompoundTag tag = new CompoundTag();
        stack.copy().save(tag);
        return tag;
    }

    public static ItemStack loadOfferStack(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return ItemStack.of(tag);
    }

    public static ListTag saveOffers(List<Offer> offers) {
        ListTag list = new ListTag();
        if (offers == null) {
            return list;
        }
        for (Offer offer : offers) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Shop", offer.shopId());
            entry.put("Stack", saveOfferStack(offer.stack()));
            entry.putInt("Unit", offer.unitPrice());
            list.add(entry);
        }
        return list;
    }

    public static List<Offer> loadOffers(ListTag list) {
        List<Offer> offers = new ArrayList<>();
        if (list == null) {
            return offers;
        }
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            ItemStack stack = loadOfferStack(entry.getCompound("Stack"));
            if (stack.isEmpty()) {
                continue;
            }
            offers.add(new Offer(entry.getString("Shop"), stack, Math.max(1, entry.getInt("Unit"))));
        }
        return offers;
    }

    public static Offer findOffer(List<Offer> offers, String shopId, ItemStack stack) {
        if (offers == null || stack == null || stack.isEmpty()) {
            return null;
        }
        // Prefer registry-id signature so NBT round-trips still match even if
        // Enchantment map key identity differs across encode/decode.
        String want = offerSignature(shopId, stack);
        for (Offer offer : offers) {
            if (want.equals(offerSignature(offer.shopId(), offer.stack()))) {
                return offer;
            }
        }
        return null;
    }
}
