package com.homestead.sellveg.order;

import com.homestead.sellveg.shop.GearOffers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class CartData {
    public static final class Line {
        public final String shopId;
        /** Count-1 template; may carry enchantments for gear offers. */
        public final ItemStack stack;
        public int count;
        public int unitPrice;

        public Line(String shopId, ItemStack stack, int count, int unitPrice) {
            this.shopId = shopId == null ? "" : shopId;
            ItemStack copy = stack == null ? ItemStack.EMPTY : stack.copy();
            if (!copy.isEmpty()) {
                copy.setCount(1);
            }
            this.stack = copy;
            this.count = count;
            this.unitPrice = Math.max(1, unitPrice);
        }

        public Item item() {
            return stack.getItem();
        }
    }

    private final List<Line> lines = new ArrayList<>();

    public List<Line> lines() {
        return lines;
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    public void clear() {
        lines.clear();
    }

    public void add(String shopId, Item item, int count, int unitPrice) {
        add(shopId, new ItemStack(item), count, unitPrice);
    }

    public void add(String shopId, ItemStack template, int count, int unitPrice) {
        if (count < 1 || template == null || template.isEmpty()) {
            return;
        }
        String id = shopId == null ? "" : shopId;
        for (Line line : lines) {
            if (line.shopId.equals(id) && GearOffers.sameOffer(line.stack, template)) {
                line.count += count;
                return;
            }
        }
        lines.add(new Line(id, template, count, unitPrice));
    }

    public boolean remove(String shopId, Item item) {
        return remove(shopId, new ItemStack(item));
    }

    public boolean remove(String shopId, ItemStack template) {
        String id = shopId == null ? "" : shopId;
        return lines.removeIf(line -> line.shopId.equals(id) && GearOffers.sameOffer(line.stack, template));
    }

    public void setCount(String shopId, Item item, int count) {
        setCount(shopId, new ItemStack(item), count);
    }

    public void setCount(String shopId, ItemStack template, int count) {
        String id = shopId == null ? "" : shopId;
        if (count < 1) {
            remove(id, template);
            return;
        }
        for (Line line : lines) {
            if (line.shopId.equals(id) && GearOffers.sameOffer(line.stack, template)) {
                line.count = count;
                return;
            }
        }
    }

    public List<ItemStack> asStacks() {
        List<ItemStack> stacks = new ArrayList<>();
        for (Line line : lines) {
            int left = line.count;
            int max = Math.max(1, line.stack.getMaxStackSize());
            while (left > 0) {
                int n = Math.min(max, left);
                ItemStack piece = line.stack.copy();
                piece.setCount(n);
                stacks.add(piece);
                left -= n;
            }
        }
        return stacks;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (Line line : lines) {
            if (line.stack.isEmpty()) {
                continue;
            }
            CompoundTag entry = new CompoundTag();
            entry.putString("Shop", line.shopId);
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(line.stack.getItem());
            entry.putString("Id", id == null ? "minecraft:air" : id.toString());
            entry.putInt("Count", line.count);
            entry.putInt("Unit", line.unitPrice);
            if (line.stack.hasTag()) {
                entry.put("StackTag", line.stack.getTag().copy());
            }
            list.add(entry);
        }
        tag.put("Lines", list);
        return tag;
    }

    public void load(CompoundTag tag) {
        lines.clear();
        if (tag == null) {
            return;
        }
        ListTag list = tag.getList("Lines", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(entry.getString("Id")));
            int count = entry.getInt("Count");
            int unit = entry.contains("Unit") ? entry.getInt("Unit") : 1;
            String shop = entry.getString("Shop");
            if (item == null || count < 1) {
                continue;
            }
            ItemStack stack = new ItemStack(item);
            if (entry.contains("StackTag", Tag.TAG_COMPOUND)) {
                stack.setTag(entry.getCompound("StackTag").copy());
            }
            lines.add(new Line(shop, stack, count, unit));
        }
    }
}
