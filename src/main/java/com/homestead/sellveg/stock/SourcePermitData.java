package com.homestead.sellveg.stock;

import com.homestead.sellveg.item.SourcePermitItem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class SourcePermitData {
    private static final String ROOT_TAG = "SourcePermit";
    private static final String MARKS_TAG = "Marks";
    private static final String DIM_TAG = "Dim";
    private static final String X_TAG = "X";
    private static final String Y_TAG = "Y";
    private static final String Z_TAG = "Z";

    private SourcePermitData() {
    }

    public static List<SourcePermitMark> read(ItemStack stack) {
        List<SourcePermitMark> marks = new ArrayList<>();
        CompoundTag root = stack.getTagElement(ROOT_TAG);
        if (root == null) {
            return marks;
        }
        ListTag list = root.getList(MARKS_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            String dim = entry.getString(DIM_TAG);
            if (dim.isEmpty()) {
                continue;
            }
            marks.add(new SourcePermitMark(dim, entry.getInt(X_TAG), entry.getInt(Y_TAG), entry.getInt(Z_TAG)));
        }
        return marks;
    }

    public static void write(ItemStack stack, List<SourcePermitMark> marks) {
        CompoundTag root = stack.getOrCreateTagElement(ROOT_TAG);
        ListTag list = new ListTag();
        for (SourcePermitMark mark : marks) {
            CompoundTag entry = new CompoundTag();
            entry.putString(DIM_TAG, mark.dimension());
            entry.putInt(X_TAG, mark.x());
            entry.putInt(Y_TAG, mark.y());
            entry.putInt(Z_TAG, mark.z());
            list.add(entry);
        }
        root.put(MARKS_TAG, list);
    }

    public static SourcePermitMark of(Level level, BlockPos pos) {
        ResourceKey<Level> key = level.dimension();
        ResourceLocation id = key.location();
        return new SourcePermitMark(id.toString(), pos.getX(), pos.getY(), pos.getZ());
    }

    public static boolean matchesDimension(SourcePermitMark mark, Level level) {
        return mark.dimension().equals(level.dimension().location().toString());
    }

    public static BlockPos toPos(SourcePermitMark mark) {
        return new BlockPos(mark.x(), mark.y(), mark.z());
    }

    public static List<SourcePermitMark> collectFromPlayer(Player player) {
        Set<SourcePermitMark> unique = new LinkedHashSet<>();
        for (ItemStack stack : player.getInventory().items) {
            addMarks(stack, unique);
        }
        for (ItemStack stack : player.getInventory().offhand) {
            addMarks(stack, unique);
        }
        return new ArrayList<>(unique);
    }

    private static void addMarks(ItemStack stack, Set<SourcePermitMark> unique) {
        if (stack.isEmpty() || !(stack.getItem() instanceof SourcePermitItem)) {
            return;
        }
        unique.addAll(read(stack));
    }
}
