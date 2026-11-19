package com.homestead.sellveg.registry;

import com.homestead.sellveg.NetShopMod;
import com.homestead.sellveg.item.ParcelBoxItem;
import com.homestead.sellveg.item.QuickListerItem;
import com.homestead.sellveg.item.SourcePermitItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, NetShopMod.MOD_ID);

    public static final RegistryObject<Item> DROP_POINT = ITEMS.register("drop_point",
            () -> new BlockItem(ModBlocks.DROP_POINT.get(), new Item.Properties()));

    public static final RegistryObject<Item> PARCEL_BOX = ITEMS.register("parcel_box",
            () -> new ParcelBoxItem(ModBlocks.PARCEL_BOX.get(), new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> SOURCE_PERMIT = ITEMS.register("source_permit",
            () -> new SourcePermitItem(new Item.Properties()));

    public static final RegistryObject<Item> QUICK_LISTER = ITEMS.register("quick_lister",
            () -> new QuickListerItem(new Item.Properties()));

    private ModItems() {
    }
}
