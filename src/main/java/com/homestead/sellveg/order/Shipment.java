package com.homestead.sellveg.order;

import com.homestead.sellveg.config.SellVegConfig;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public final class Shipment {
    private final UUID id;
    private final NonNullList<ItemStack> contents;
    private final Courier courier;
    private int remainingTicks;
    private boolean blocked;
    private boolean notifiedBlocked;

    public Shipment(UUID id, NonNullList<ItemStack> contents, int remainingTicks) {
        this(id, contents, remainingTicks, Courier.FALLBACK);
    }

    public Shipment(UUID id, NonNullList<ItemStack> contents, int remainingTicks, Courier courier) {
        this.id = id;
        this.contents = contents;
        this.remainingTicks = remainingTicks;
        this.courier = courier == null ? Courier.FALLBACK : courier;
    }

    public UUID getId() {
        return id;
    }

    public NonNullList<ItemStack> getContents() {
        return contents;
    }

    public Courier getCourier() {
        return courier;
    }

    public int getRemainingTicks() {
        return remainingTicks;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setRemainingTicks(int remainingTicks) {
        this.remainingTicks = remainingTicks;
    }

    public boolean tickDown() {
        if (remainingTicks > 0) {
            remainingTicks--;
        }
        return remainingTicks <= 0;
    }

    public void markBlocked() {
        blocked = true;
        remainingTicks = SellVegConfig.DELIVERY_RETRY_TICKS.get();
    }

    public void clearBlocked() {
        blocked = false;
        notifiedBlocked = false;
    }

    public boolean consumeBlockedNotify() {
        if (blocked && !notifiedBlocked) {
            notifiedBlocked = true;
            return true;
        }
        return false;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        tag.put("Contents", ParcelFiller.saveBox(contents));
        tag.putString("Courier", courier.id());
        tag.putInt("Remaining", remainingTicks);
        tag.putBoolean("Blocked", blocked);
        tag.putBoolean("NotifiedBlocked", notifiedBlocked);
        return tag;
    }

    public static Shipment load(CompoundTag tag) {
        int slots = 27;
        try {
            slots = SellVegConfig.BOX_SLOT_CAPACITY.get();
        } catch (Exception ignored) {
            // unit / early load
        }
        NonNullList<ItemStack> contents = ParcelFiller.loadBox(tag.getCompound("Contents"), slots);
        Courier courier = Courier.fromId(tag.getString("Courier"));
        Shipment shipment = new Shipment(tag.getUUID("Id"), contents, tag.getInt("Remaining"), courier);
        shipment.blocked = tag.getBoolean("Blocked");
        shipment.notifiedBlocked = tag.getBoolean("NotifiedBlocked");
        return shipment;
    }
}
