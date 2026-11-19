package com.homestead.sellveg.stock;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * Where items were taken from when listing: player inventory or a marked source chest.
 */
public record StockDraw(boolean fromPlayer, String dimension, int x, int y, int z, int count) {
    private static final String KEY_PLAYER = "Player";
    private static final String KEY_DIM = "Dim";
    private static final String KEY_X = "X";
    private static final String KEY_Y = "Y";
    private static final String KEY_Z = "Z";
    private static final String KEY_COUNT = "Count";

    public static StockDraw player(int count) {
        return new StockDraw(true, "", 0, 0, 0, Math.max(0, count));
    }

    public static StockDraw chest(SourcePermitMark mark, int count) {
        return new StockDraw(false, mark.dimension(), mark.x(), mark.y(), mark.z(), Math.max(0, count));
    }

    public SourcePermitMark mark() {
        return new SourcePermitMark(dimension, x, y, z);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(KEY_PLAYER, fromPlayer);
        if (!fromPlayer) {
            tag.putString(KEY_DIM, dimension == null ? "" : dimension);
            tag.putInt(KEY_X, x);
            tag.putInt(KEY_Y, y);
            tag.putInt(KEY_Z, z);
        }
        tag.putInt(KEY_COUNT, count);
        return tag;
    }

    public static StockDraw load(CompoundTag tag) {
        int count = Math.max(0, tag.getInt(KEY_COUNT));
        if (tag.getBoolean(KEY_PLAYER)) {
            return player(count);
        }
        return new StockDraw(false, tag.getString(KEY_DIM), tag.getInt(KEY_X), tag.getInt(KEY_Y), tag.getInt(KEY_Z), count);
    }

    public static ListTag saveList(List<StockDraw> draws) {
        ListTag list = new ListTag();
        if (draws == null) {
            return list;
        }
        for (StockDraw draw : draws) {
            if (draw != null && draw.count() > 0) {
                list.add(draw.save());
            }
        }
        return list;
    }

    public static List<StockDraw> loadList(ListTag list) {
        List<StockDraw> draws = new ArrayList<>();
        if (list == null) {
            return draws;
        }
        for (int i = 0; i < list.size(); i++) {
            StockDraw draw = load(list.getCompound(i));
            if (draw.count() > 0) {
                draws.add(draw);
            }
        }
        return draws;
    }

    public static List<StockDraw> loadList(CompoundTag parent, String key) {
        if (parent == null || !parent.contains(key, Tag.TAG_LIST)) {
            return List.of();
        }
        return loadList(parent.getList(key, Tag.TAG_COMPOUND));
    }
}
