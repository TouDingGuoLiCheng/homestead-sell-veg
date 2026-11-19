package com.homestead.sellveg.shop;

/**
 * Pure hover/click rules for the Quick Listing Scroll. No Minecraft GUI types.
 */
public final class QuickListHints {
    private QuickListHints() {
    }

    public enum Kind {
        NONE,
        PRICE,
        PENDING
    }

    public record Hover(Kind kind, int price) {
        public static Hover none() {
            return new Hover(Kind.NONE, 0);
        }

        public static Hover price(int mid) {
            return new Hover(Kind.PRICE, mid);
        }

        public static Hover pending() {
            return new Hover(Kind.PENDING, 0);
        }
    }

    public static Hover hover(
            boolean scrollReady,
            boolean hoveringScroll,
            boolean emptySlot,
            boolean tradeable,
            int syncedMid
    ) {
        if (!scrollReady || hoveringScroll || emptySlot) {
            return Hover.none();
        }
        if (syncedMid >= 1) {
            return Hover.price(syncedMid);
        }
        if (tradeable) {
            return Hover.pending();
        }
        return Hover.none();
    }

    public static boolean shouldInterceptClick(boolean scrollReady, boolean hoveringScroll, boolean emptySlot) {
        return scrollReady && !hoveringScroll && !emptySlot;
    }
}
