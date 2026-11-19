package com.homestead.sellveg.market;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.nio.charset.StandardCharsets;

public final class PriceCacheSavedData extends SavedData {
    public static final String NAME = "homestead_sell_veg_price_cache";

    private PriceCacheSnapshot snapshot = PriceCacheSnapshot.empty();

    public static PriceCacheSavedData get(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            return new PriceCacheSavedData();
        }
        return overworld.getDataStorage().computeIfAbsent(PriceCacheSavedData::load, PriceCacheSavedData::new, NAME);
    }

    public static PriceCacheSavedData load(CompoundTag tag) {
        PriceCacheSavedData data = new PriceCacheSavedData();
        data.snapshot = PriceCacheSnapshot.decode(readBlob(tag));
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        // NBT strings cap at 65535 bytes and the snapshot is far larger, so store raw UTF-8 bytes.
        tag.putByteArray("Blob", snapshot.encode().getBytes(StandardCharsets.UTF_8));
        return tag;
    }

    /** Accepts the byte-array form plus legacy string saves, which silently failed to write. */
    private static String readBlob(CompoundTag tag) {
        if (tag.contains("Blob", Tag.TAG_BYTE_ARRAY)) {
            return new String(tag.getByteArray("Blob"), StandardCharsets.UTF_8);
        }
        return tag.getString("Blob");
    }

    public PriceCacheSnapshot snapshot() {
        return snapshot;
    }

    public void replace(PriceCacheSnapshot next) {
        this.snapshot = next;
        setDirty();
    }
}
