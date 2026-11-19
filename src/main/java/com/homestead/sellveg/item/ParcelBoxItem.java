package com.homestead.sellveg.item;

import com.homestead.sellveg.block.entity.ParcelBoxBE;
import com.homestead.sellveg.registry.ModItems;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

public class ParcelBoxItem extends BlockItem {
    public ParcelBoxItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static ItemStack createPrefilledDemo() {
        ItemStack stack = new ItemStack(ModItems.PARCEL_BOX.get());
        NonNullList<ItemStack> items = NonNullList.withSize(ParcelBoxBE.SLOT_CAPACITY, ItemStack.EMPTY);
        items.set(0, new ItemStack(Items.WHEAT, 64));
        items.set(1, new ItemStack(Items.CARROT, 32));
        items.set(2, new ItemStack(Items.POTATO, 16));
        CompoundTag beTag = new CompoundTag();
        net.minecraft.world.ContainerHelper.saveAllItems(beTag, items);
        BlockItem.setBlockEntityData(stack, com.homestead.sellveg.registry.ModBlockEntities.PARCEL_BOX.get(), beTag);
        return stack;
    }
}
