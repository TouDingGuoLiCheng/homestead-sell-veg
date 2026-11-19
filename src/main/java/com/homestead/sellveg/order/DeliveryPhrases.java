package com.homestead.sellveg.order;

import java.util.UUID;

public final class DeliveryPhrases {
    public static final int COUNT = 2;

    private DeliveryPhrases() {
    }

    public static int index(UUID shipmentId) {
        long bits = shipmentId == null ? 0L : shipmentId.getMostSignificantBits() ^ shipmentId.getLeastSignificantBits();
        return Math.floorMod(Long.hashCode(bits), COUNT);
    }

    public static String langKey(UUID shipmentId) {
        return "message.homestead_sell_veg.delivery_arrived." + index(shipmentId);
    }
}
