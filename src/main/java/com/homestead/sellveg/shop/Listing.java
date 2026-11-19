package com.homestead.sellveg.shop;

import com.homestead.sellveg.stock.StockDraw;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

public final class Listing {
    private static final String KEY_ID = "Id";
    private static final String KEY_STACK = "Stack";
    private static final String KEY_UNIT_PRICE = "UnitPrice";
    private static final String KEY_REMAINING = "RemainingTicks";
    private static final String KEY_SOURCES = "Sources";

    private UUID id;
    private ItemStack stack;
    private int unitPrice;
    private int remainingTicks;
    private List<StockDraw> sources;

    public Listing(UUID id, ItemStack stack, int unitPrice, int remainingTicks) {
        this(id, stack, unitPrice, remainingTicks, List.of());
    }

    public Listing(UUID id, ItemStack stack, int unitPrice, int remainingTicks, List<StockDraw> sources) {
        this.id = id;
        this.stack = stack.copy();
        this.unitPrice = unitPrice;
        this.remainingTicks = remainingTicks;
        this.sources = sources == null ? List.of() : List.copyOf(sources);
    }

    public UUID getId() {
        return id;
    }

    public ItemStack getStack() {
        return stack;
    }

    public int getUnitPrice() {
        return unitPrice;
    }

    public int getRemainingTicks() {
        return remainingTicks;
    }

    public List<StockDraw> getSources() {
        return sources;
    }

    public long getPayout() {
        return (long) unitPrice * stack.getCount();
    }

    public boolean tickDown() {
        if (remainingTicks < 0) {
            return false;
        }
        if (remainingTicks == 0) {
            return true;
        }
        remainingTicks--;
        return remainingTicks <= 0;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID(KEY_ID, id);
        tag.put(KEY_STACK, stack.save(new CompoundTag()));
        tag.putInt(KEY_UNIT_PRICE, unitPrice);
        tag.putInt(KEY_REMAINING, remainingTicks);
        if (!sources.isEmpty()) {
            tag.put(KEY_SOURCES, StockDraw.saveList(sources));
        }
        return tag;
    }

    public static Listing load(CompoundTag tag) {
        UUID id = tag.hasUUID(KEY_ID) ? tag.getUUID(KEY_ID) : UUID.randomUUID();
        ItemStack stack = ItemStack.of(tag.getCompound(KEY_STACK));
        int unitPrice = Math.max(1, tag.getInt(KEY_UNIT_PRICE));
        int remaining = tag.getInt(KEY_REMAINING);
        List<StockDraw> sources = StockDraw.loadList(tag, KEY_SOURCES);
        return new Listing(id, stack, unitPrice, remaining, sources);
    }
}
