package com.homestead.sellveg.shop;

import com.homestead.sellveg.data.PlayerNetShopData;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Live directory of online players whose net shop another player may browse.
 * Deliberately not persisted: an offline owner is simply absent from the list.
 */
public final class PeerShopIndex {
    public record Entry(UUID ownerId, String name, int listingCount) {
    }

    private PeerShopIndex() {
    }

    public static List<Entry> visibleTo(ServerPlayer viewer) {
        List<Entry> entries = new ArrayList<>();
        if (viewer == null || viewer.getServer() == null) {
            return entries;
        }
        for (ServerPlayer other : viewer.getServer().getPlayerList().getPlayers()) {
            if (other.getUUID().equals(viewer.getUUID())) {
                continue;
            }
            int count = 0;
            for (Listing listing : PlayerNetShopData.get(other).shop().listings()) {
                if (!listing.getStack().isEmpty()) {
                    count++;
                }
            }
            entries.add(new Entry(other.getUUID(), other.getGameProfile().getName(), count));
        }
        entries.sort(Comparator.comparing(Entry::name, String.CASE_INSENSITIVE_ORDER));
        return entries;
    }
}
