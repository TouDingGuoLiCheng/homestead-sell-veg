package com.homestead.sellveg.order;

/**
 * Mall couriers. Delay is the configured parcel window scaled by permille (1000 = 1.0×).
 */
public enum Courier {
    ORANGE_PULP("orange_pulp", 550),
    DOORSTEP("doorstep", 1000),
    CART("cart", 1450);

    public static final Courier FALLBACK = DOORSTEP;

    private final String id;
    private final int scalePermille;

    Courier(String id, int scalePermille) {
        this.id = id;
        this.scalePermille = scalePermille;
    }

    public String id() {
        return id;
    }

    public String langKey() {
        return "gui.homestead_sell_veg.courier." + id;
    }

    public int scaleDelay(int baseTicks) {
        long scaled = (long) Math.max(1, baseTicks) * scalePermille / 1000L;
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, scaled));
    }

    public static Courier pick(int roll) {
        Courier[] all = values();
        int i = roll % all.length;
        if (i < 0) {
            i += all.length;
        }
        return all[i];
    }

    public static Courier fromId(String id) {
        if (id == null || id.isEmpty()) {
            return FALLBACK;
        }
        for (Courier courier : values()) {
            if (courier.id.equals(id)) {
                return courier;
            }
        }
        return FALLBACK;
    }
}
