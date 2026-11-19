package com.homestead.sellveg.shop;

public final class ProductReviewPicker {
    public static final int NICK_COUNT = 28;
    public static final int GENERIC_COUNT = 36;
    public static final int FARM_COUNT = 32;
    public static final int ORE_COUNT = 28;
    public static final int FOOD_COUNT = 32;
    public static final int GROCERY_COUNT = 24;
    public static final int FURNITURE_COUNT = 24;
    public static final int FARM_TOOL_COUNT = 24;
    public static final int SPECIAL_COUNT = 8;
    public static final int BAD_COUNT = 20;

    public record LinePick(int nickIndex, String pool, int textIndex, float stars) {
        public String nickKey() {
            return "review.homestead_sell_veg.nick." + nickIndex;
        }

        public String textKey() {
            return "review.homestead_sell_veg." + pool + "." + textIndex;
        }
    }

    public record Pick(float rating, LinePick line0, LinePick line1) {
    }

    private ProductReviewPicker() {
    }

    public static Pick pick(String shopId, String itemId, ItemShelfKind kind) {
        int h = hash(shopId, itemId);
        String pool0 = poolFor(itemId, kind);
        String pool1 = "generic";
        if (Math.floorMod(h, 5) == 0) {
            pool0 = "bad";
        }
        if (Math.floorMod(h, 6) == 1) {
            pool1 = "bad";
        }
        int t0 = Math.floorMod(h, countFor(pool0));
        int t1 = Math.floorMod(h / 7 + 3, countFor(pool1));
        int n0 = Math.floorMod(h / 11, NICK_COUNT);
        int n1 = Math.floorMod(h / 13 + 5, NICK_COUNT);
        if (n1 == n0) {
            n1 = (n0 + 1) % NICK_COUNT;
        }
        float s0 = starsFor(pool0, h);
        float s1 = starsFor(pool1, h / 5 + 2);
        float rating = Math.round((s0 + s1)) / 2.0F;
        rating = Math.max(1.0F, Math.min(5.0F, Math.round(rating * 2.0F) / 2.0F));
        return new Pick(rating, new LinePick(n0, pool0, t0, s0), new LinePick(n1, pool1, t1, s1));
    }

    static String poolFor(String itemId, ItemShelfKind kind) {
        String path = itemId == null ? "" : itemId.substring(itemId.indexOf(':') + 1);
        if (isSpecial(path)) {
            return path;
        }
        return switch (kind) {
            case FARM -> "farm";
            case ORE -> "ore";
            case FOOD -> "food";
            case GROCERY -> "grocery";
            case FURNITURE -> "furniture";
            case FARM_TOOL -> "farm_tool";
            case BUILD, FLOWER, REDSTONE, WEAPON, ARMOR -> "generic";
        };
    }

    private static boolean isSpecial(String path) {
        return switch (path) {
            case "wheat", "apple", "carrot", "potato", "bread", "cookie", "diamond", "coal" -> true;
            default -> false;
        };
    }

    private static int countFor(String pool) {
        return switch (pool) {
            case "farm" -> FARM_COUNT;
            case "ore" -> ORE_COUNT;
            case "food" -> FOOD_COUNT;
            case "grocery" -> GROCERY_COUNT;
            case "furniture" -> FURNITURE_COUNT;
            case "farm_tool" -> FARM_TOOL_COUNT;
            case "generic" -> GENERIC_COUNT;
            case "bad" -> BAD_COUNT;
            default -> SPECIAL_COUNT;
        };
    }

    private static float starsFor(String pool, int h) {
        if ("bad".equals(pool)) {
            return 1.0F + Math.floorMod(h, 4) * 0.5F;
        }
        return 3.5F + Math.floorMod(h, 4) * 0.5F;
    }

    private static int hash(String shopId, String itemId) {
        int h = shopId == null ? 0 : shopId.hashCode();
        return 31 * h + (itemId == null ? 0 : itemId.hashCode());
    }
}
