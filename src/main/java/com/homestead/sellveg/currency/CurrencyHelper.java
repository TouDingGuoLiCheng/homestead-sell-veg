package com.homestead.sellveg.currency;

import com.homestead.sellveg.data.PlayerNetShopData;
import net.minecraft.world.entity.player.Player;

import java.util.Locale;

public final class CurrencyHelper {
    private CurrencyHelper() {
    }

    /**
     * UI count text: ≤999 raw; then k / M / B
     * (1000→1k, 1_000_000→1M, 1_000_000_000→1B; one decimal when needed).
     */
    public static String formatCount(long count) {
        if (count < 0) {
            count = 0;
        }
        if (count < 1_000) {
            return Long.toString(count);
        }
        if (count < 1_000_000) {
            return formatScaled(count, 1_000, "k");
        }
        if (count < 1_000_000_000L) {
            return formatScaled(count, 1_000_000, "M");
        }
        return formatScaled(count, 1_000_000_000L, "B");
    }

    public static String formatCount(int count) {
        return formatCount((long) count);
    }

    private static String formatScaled(long count, long unit, String suffix) {
        if (count % unit == 0) {
            return (count / unit) + suffix;
        }
        String s = String.format(Locale.ROOT, "%.1f", count / (double) unit);
        if (s.endsWith(".0")) {
            s = s.substring(0, s.length() - 2);
        }
        return s + suffix;
    }

    public static long countCoins(Player player) {
        return PlayerNetShopData.get(player).wallet().balance();
    }

    public static boolean takeCoins(PlayerNetShopData data, long amount) {
        return data.wallet().take(amount);
    }

    public static void giveCoins(PlayerNetShopData data, long amount) {
        data.wallet().give(amount);
    }
}
