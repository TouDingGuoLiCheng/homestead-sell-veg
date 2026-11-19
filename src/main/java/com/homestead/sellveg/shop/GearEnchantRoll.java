package com.homestead.sellveg.shop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Pure helpers for daily enchanted mall SKUs (count / sampling). */
public final class GearEnchantRoll {
    /** Weights for enchant counts 1..5 (more enchants rarer). */
    private static final int[] COUNT_WEIGHTS = {16, 8, 4, 2, 1};

    private GearEnchantRoll() {
    }

    public static int pickDailyCount(Random random) {
        return 3 + random.nextInt(5); // 3..7
    }

    public static int pickEnchantCount(Random random) {
        int total = 0;
        for (int w : COUNT_WEIGHTS) {
            total += w;
        }
        int roll = random.nextInt(total);
        int acc = 0;
        for (int i = 0; i < COUNT_WEIGHTS.length; i++) {
            acc += COUNT_WEIGHTS[i];
            if (roll < acc) {
                return i + 1;
            }
        }
        return 1;
    }

    public static List<String> pickItemKeys(List<String> candidates, int target, Random random) {
        if (candidates == null || candidates.isEmpty() || target < 1) {
            return List.of();
        }
        ArrayList<String> copy = new ArrayList<>(candidates);
        Collections.shuffle(copy, random);
        int n = Math.min(target, copy.size());
        return List.copyOf(copy.subList(0, n));
    }
}
