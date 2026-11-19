package com.homestead.sellveg.client;

import com.homestead.sellveg.network.QuickListPriceSyncPacket;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class QuickListClientPrices {
    private static final Map<ResourceLocation, Integer> MIDS = new HashMap<>();

    private QuickListClientPrices() {
    }

    public static void apply(QuickListPriceSyncPacket packet) {
        MIDS.clear();
        MIDS.putAll(packet.mids);
    }

    public static void clear() {
        MIDS.clear();
    }

    public static int suggestMid(Item item) {
        if (item == null) {
            return 0;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) {
            return 0;
        }
        return MIDS.getOrDefault(id, 0);
    }
}
