package com.homestead.sellveg.block.entity;

import com.homestead.sellveg.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ParcelBoxBE extends BlockEntity {
    public static final int SLOT_CAPACITY = 27;

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOT_CAPACITY, ItemStack.EMPTY);

    public ParcelBoxBE(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PARCEL_BOX.get(), pos, state);
    }

    public NonNullList<ItemStack> getItems() {
        return items;
    }

    public void setItems(NonNullList<ItemStack> items) {
        this.items = items;
        setChanged();
    }

    public void clearContents() {
        items.clear();
        setChanged();
    }

    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items = NonNullList.withSize(SLOT_CAPACITY, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);
    }

    public static NonNullList<ItemStack> readItemsFromTag(CompoundTag tag) {
        NonNullList<ItemStack> list = NonNullList.withSize(SLOT_CAPACITY, ItemStack.EMPTY);
        if (tag.contains("Items", Tag.TAG_LIST)) {
            ContainerHelper.loadAllItems(tag, list);
        }
        return list;
    }

    public CompoundTag saveItemsOnly() {
        CompoundTag tag = new CompoundTag();
        ContainerHelper.saveAllItems(tag, items);
        return tag;
    }
}
