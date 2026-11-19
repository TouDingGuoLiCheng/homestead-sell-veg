package com.homestead.sellveg.registry;

import com.homestead.sellveg.NetShopMod;
import com.homestead.sellveg.block.entity.DropPointBE;
import com.homestead.sellveg.block.entity.ParcelBoxBE;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, NetShopMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<DropPointBE>> DROP_POINT =
            BLOCK_ENTITIES.register("drop_point", () ->
                    BlockEntityType.Builder.of(DropPointBE::new, ModBlocks.DROP_POINT.get()).build(null));

    public static final RegistryObject<BlockEntityType<ParcelBoxBE>> PARCEL_BOX =
            BLOCK_ENTITIES.register("parcel_box", () ->
                    BlockEntityType.Builder.of(ParcelBoxBE::new, ModBlocks.PARCEL_BOX.get()).build(null));

    private ModBlockEntities() {
    }
}
