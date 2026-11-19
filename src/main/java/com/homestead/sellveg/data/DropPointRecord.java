package com.homestead.sellveg.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.UUID;

public final class DropPointRecord {
    public final UUID id;
    public final ResourceKey<Level> dimension;
    public final BlockPos pos;
    public String name;

    public DropPointRecord(UUID id, ResourceKey<Level> dimension, BlockPos pos, String name) {
        this.id = id;
        this.dimension = dimension;
        this.pos = pos.immutable();
        this.name = name;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        tag.putString("Dim", dimension.location().toString());
        tag.putInt("X", pos.getX());
        tag.putInt("Y", pos.getY());
        tag.putInt("Z", pos.getZ());
        tag.putString("Name", name == null ? "" : name);
        return tag;
    }

    public static DropPointRecord load(CompoundTag tag) {
        ResourceLocation dimId = new ResourceLocation(tag.getString("Dim"));
        ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, dimId);
        BlockPos pos = new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z"));
        return new DropPointRecord(tag.getUUID("Id"), dim, pos, tag.getString("Name"));
    }
}
