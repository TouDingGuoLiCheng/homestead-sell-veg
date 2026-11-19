package com.homestead.sellveg.order;

/**
 * Pending parcel queue cap (boxes waiting for delivery).
 */
public final class ShipmentCapacity {
    private ShipmentCapacity() {
    }

    public static boolean canAdd(int pending, int adding, int cap) {
        if (adding <= 0) {
            return true;
        }
        if (cap < 0) {
            return false;
        }
        return pending <= cap - adding;
    }
}
