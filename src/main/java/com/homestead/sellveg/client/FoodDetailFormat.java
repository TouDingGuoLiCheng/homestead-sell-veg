package com.homestead.sellveg.client;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Food detail helpers that need Minecraft types (effect lines, stack food lookup).
 */
public final class FoodDetailFormat {
    private FoodDetailFormat() {
    }

    public static FoodProperties foodOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return stack.getItem().getFoodProperties(stack, null);
    }

    public static boolean isFood(ItemStack stack) {
        return foodOf(stack) != null;
    }

    public static double saturationRestored(FoodProperties food) {
        if (food == null) {
            return 0.0D;
        }
        return FoodDetailMath.saturationRestored(food.getNutrition(), food.getSaturationModifier());
    }

    public static List<Component> effectLines(FoodProperties food) {
        List<Component> lines = new ArrayList<>();
        if (food == null || food.getEffects().isEmpty()) {
            lines.add(Component.translatable("gui.homestead_sell_veg.no_effects"));
            return lines;
        }
        for (var pair : food.getEffects()) {
            MobEffectInstance inst = pair.getFirst();
            if (inst == null) {
                continue;
            }
            float chance = pair.getSecond() == null ? 1.0F : pair.getSecond();
            lines.add(formatEffectLine(inst, chance));
        }
        if (lines.isEmpty()) {
            lines.add(Component.translatable("gui.homestead_sell_veg.no_effects"));
        }
        return lines;
    }

    public static Component formatEffectLine(MobEffectInstance inst, float chance) {
        MobEffect effect = inst.getEffect();
        MutableComponent name = Component.translatable(effect.getDescriptionId());
        if (inst.getAmplifier() > 0) {
            name = Component.translatable(
                    "potion.withAmplifier",
                    name,
                    Component.translatable("potion.potency." + inst.getAmplifier()));
        }
        MutableComponent line;
        if (effect.isInstantenous() || inst.getDuration() <= 0) {
            line = name;
        } else {
            line = Component.translatable(
                    "potion.withDuration",
                    name,
                    FoodDetailMath.formatDurationTicks(inst.getDuration()));
        }
        String suffix = FoodDetailMath.formatChanceSuffix(chance);
        if (!suffix.isEmpty()) {
            line = line.copy().append(suffix);
        }
        return line.withStyle(effect.isBeneficial() ? ChatFormatting.BLUE : ChatFormatting.RED);
    }
}
