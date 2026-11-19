package com.homestead.sellveg.market;

/** Keep Eat Properly milestone token foods out of the mall without touching Minecraft types. */
public final class EatProperlyTradeFilter {
    public static final String MOD_ID = "touhou_maid_eat_properly";

    private EatProperlyTradeFilter() {
    }

    public static boolean isTokenFood(String namespace, boolean edible) {
        return edible && MOD_ID.equals(namespace);
    }
}
