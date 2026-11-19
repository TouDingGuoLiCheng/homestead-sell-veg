package com.homestead.sellveg.market;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Disk-friendly price catalog snapshot (no Minecraft types).
 */
public final class PriceCacheSnapshot {
    private final String fingerprint;
    private final Map<String, Integer> prices;
    private final List<String> tradeable;
    private final Map<String, Integer> crafts;

    public PriceCacheSnapshot(String fingerprint, Map<String, Integer> prices, List<String> tradeable) {
        this(fingerprint, prices, tradeable, Map.of());
    }

    public PriceCacheSnapshot(String fingerprint, Map<String, Integer> prices, List<String> tradeable,
                              Map<String, Integer> crafts) {
        this.fingerprint = fingerprint == null ? "" : fingerprint;
        this.prices = Collections.unmodifiableMap(new LinkedHashMap<>(prices));
        this.tradeable = List.copyOf(tradeable);
        this.crafts = Collections.unmodifiableMap(new LinkedHashMap<>(crafts == null ? Map.of() : crafts));
    }

    public static PriceCacheSnapshot empty() {
        return new PriceCacheSnapshot("", Map.of(), List.of(), Map.of());
    }

    public static final String FORMULA_VERSION = "assemble-not-salvage-v1|flower-extra-dripleaf-2|build-cobbled-flower-leaf-vine-4|build-mud-5|furniture-chest-barrel-6|build-mossy-stone-bricks-7|build-concrete-powder-8|no-fallback-9";

    public static String fingerprint(List<String> modsSorted, int recipeCount, long recipeHash) {
        return FORMULA_VERSION + "|" + String.join(",", modsSorted) + "|" + recipeCount + "|" + recipeHash;
    }

    public static long recipeHash(Iterable<String> recipeIds) {
        long hash = 1125899906842597L;
        for (String id : recipeIds) {
            hash = 31 * hash + id.hashCode();
        }
        return hash;
    }

    public boolean matches(String expected) {
        return !fingerprint.isEmpty() && fingerprint.equals(expected);
    }

    public Map<String, Integer> prices() {
        return prices;
    }

    public List<String> tradeable() {
        return tradeable;
    }

    public Map<String, Integer> crafts() {
        return crafts;
    }

    public String encode() {
        StringBuilder out = new StringBuilder();
        out.append("v2\n").append(fingerprint).append('\n');
        appendMap(out, prices);
        out.append(tradeable.size()).append('\n');
        for (String id : tradeable) {
            out.append(id).append('\n');
        }
        appendMap(out, crafts);
        return out.toString();
    }

    private static void appendMap(StringBuilder out, Map<String, Integer> map) {
        TreeMap<String, Integer> ordered = new TreeMap<>(map);
        out.append(ordered.size()).append('\n');
        for (Map.Entry<String, Integer> entry : ordered.entrySet()) {
            out.append(entry.getKey()).append('=').append(entry.getValue()).append('\n');
        }
    }

    public static PriceCacheSnapshot decode(String blob) {
        if (blob == null || blob.isBlank()) {
            return empty();
        }
        String[] lines = blob.split("\n", -1);
        if (lines.length < 4 || (!"v1".equals(lines[0]) && !"v2".equals(lines[0]))) {
            return empty();
        }
        boolean v2 = "v2".equals(lines[0]);
        String fp = lines[1];
        int cursor = 2;
        Map<String, Integer> prices = readMap(lines, cursor);
        if (prices == null) {
            return empty();
        }
        cursor = skipMap(lines, cursor);
        if (cursor >= lines.length) {
            return empty();
        }
        int tradeCount;
        try {
            tradeCount = Integer.parseInt(lines[cursor++].trim());
        } catch (NumberFormatException ex) {
            return empty();
        }
        List<String> tradeable = new ArrayList<>();
        for (int i = 0; i < tradeCount && cursor < lines.length; i++, cursor++) {
            if (!lines[cursor].isBlank()) {
                tradeable.add(lines[cursor]);
            }
        }
        Map<String, Integer> crafts = Map.of();
        if (v2 && cursor < lines.length) {
            Map<String, Integer> parsed = readMap(lines, cursor);
            if (parsed != null) {
                crafts = parsed;
            }
        }
        return new PriceCacheSnapshot(fp, prices, tradeable, crafts);
    }

    private static Map<String, Integer> readMap(String[] lines, int cursor) {
        if (cursor >= lines.length) {
            return null;
        }
        int count;
        try {
            count = Integer.parseInt(lines[cursor].trim());
        } catch (NumberFormatException ex) {
            return null;
        }
        Map<String, Integer> map = new LinkedHashMap<>();
        cursor++;
        for (int i = 0; i < count && cursor < lines.length; i++, cursor++) {
            String line = lines[cursor];
            int eq = line.lastIndexOf('=');
            if (eq < 1) {
                continue;
            }
            try {
                map.put(line.substring(0, eq), Integer.parseInt(line.substring(eq + 1)));
            } catch (NumberFormatException ignored) {
            }
        }
        return map;
    }

    private static int skipMap(String[] lines, int cursor) {
        if (cursor >= lines.length) {
            return cursor;
        }
        int count;
        try {
            count = Integer.parseInt(lines[cursor].trim());
        } catch (NumberFormatException ex) {
            return cursor;
        }
        return cursor + 1 + count;
    }
}
