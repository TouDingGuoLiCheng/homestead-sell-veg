package com.homestead.sellveg.stock;

import java.util.ArrayList;
import java.util.List;

public final class StockPacker {
    public static final int SLOT_CAP = 10_000;
    public static final int LIST_MAX = 64;

    private StockPacker() {
    }

    public static List<Integer> split(int total, int cap) {
        List<Integer> slots = new ArrayList<>();
        if (total < 1 || cap < 1) {
            return slots;
        }
        int left = total;
        while (left > 0) {
            int chunk = Math.min(cap, left);
            slots.add(chunk);
            left -= chunk;
        }
        return slots;
    }

    public static int clampListCount(int count, int max) {
        if (count < 1 || max < 1) {
            return 0;
        }
        return Math.min(count, max);
    }

    /** Listing qty cap: never above stock on hand or {@link #LIST_MAX}. */
    public static int listQtyCap(int available) {
        if (available < 1) {
            return 0;
        }
        return Math.min(LIST_MAX, available);
    }

    /** Typing in a qty box: junk becomes empty, overflow becomes {@code max}. */
    public static String sanitizeQtyTyping(String raw, int max) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        for (int i = 0; i < raw.length(); i++) {
            if (!Character.isDigit(raw.charAt(i))) {
                return "";
            }
        }
        if (max < 1) {
            return "";
        }
        try {
            long n = Long.parseLong(raw);
            if (n > max) {
                return String.valueOf(max);
            }
            return raw;
        } catch (NumberFormatException ex) {
            return String.valueOf(max);
        }
    }

    public static int bumpQty(String current, int delta, int max) {
        int n = 0;
        String cleaned = sanitizeQtyTyping(current == null ? "" : current, max);
        if (!cleaned.isEmpty()) {
            n = Integer.parseInt(cleaned);
        }
        return Math.min(max, Math.max(1, n + delta));
    }

    /**
     * Deduct {@code want} from pools in order. Mutates {@code pools}. Returns leftover demand.
     */
    public static int takeWaterfall(int[] pools, int want) {
        if (pools == null || want < 1) {
            return Math.max(0, want);
        }
        int remaining = want;
        for (int i = 0; i < pools.length && remaining > 0; i++) {
            int take = Math.min(pools[i], remaining);
            pools[i] -= take;
            remaining -= take;
        }
        return remaining;
    }
}
