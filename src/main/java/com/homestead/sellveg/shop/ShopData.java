package com.homestead.sellveg.shop;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Shop level, cumulative sell revenue, and active listings.
 */
public final class ShopData {
    private static final String KEY_LEVEL = "Level";
    private static final String KEY_SOLD_TOTAL = "SoldTotal";
    private static final String KEY_LISTINGS = "Listings";

    private int level;
    private long soldTotal;
    private final List<Listing> listings = new ArrayList<>();

    public int getLevel() {
        return level;
    }

    public long getSoldTotal() {
        return soldTotal;
    }

    public int getListingCapacity() {
        return ShopProgression.listingCapacity(level);
    }

    public List<Listing> listings() {
        return listings;
    }

    public boolean canAddListing() {
        listings.removeIf(listing -> listing.getStack().isEmpty());
        return listings.size() < getListingCapacity();
    }

    public void addListing(Listing listing) {
        if (listing == null || listing.getStack().isEmpty()) {
            return;
        }
        listings.add(listing);
    }

    public Optional<Listing> removeListing(UUID id) {
        for (int i = 0; i < listings.size(); i++) {
            if (listings.get(i).getId().equals(id)) {
                return Optional.of(listings.remove(i));
            }
        }
        return Optional.empty();
    }

    public void addSoldTotal(long amount) {
        if (amount <= 0) {
            return;
        }
        soldTotal += amount;
        refreshLevel();
    }

    public void refreshLevel() {
        level = ShopProgression.levelForSoldTotal(soldTotal, ShopProgression.thresholds());
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(KEY_LEVEL, level);
        tag.putLong(KEY_SOLD_TOTAL, soldTotal);
        ListTag list = new ListTag();
        for (Listing listing : listings) {
            list.add(listing.save());
        }
        tag.put(KEY_LISTINGS, list);
        return tag;
    }

    public void load(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return;
        }
        level = tag.getInt(KEY_LEVEL);
        soldTotal = tag.getLong(KEY_SOLD_TOTAL);
        listings.clear();
        ListTag list = tag.getList(KEY_LISTINGS, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            Listing listing = Listing.load(list.getCompound(i));
            if (!listing.getStack().isEmpty()) {
                listings.add(listing);
            }
        }
        refreshLevel();
    }
}
