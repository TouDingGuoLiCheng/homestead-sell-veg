package com.homestead.sellveg.registry;

import com.homestead.sellveg.NetShopMod;
import com.homestead.sellveg.block.DropPointBlock;
import com.homestead.sellveg.block.DropPointMining;
import com.homestead.sellveg.block.ParcelBoxBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, NetShopMod.MOD_ID);

    public static final RegistryObject<Block> DROP_POINT = BLOCKS.register("drop_point",
            () -> new DropPointBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GREEN)
                    .strength(DropPointMining.DESTROY_TIME)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    public static final RegistryObject<Block> PARCEL_BOX = BLOCKS.register("parcel_box",
            () -> new ParcelBoxBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN)
                    .strength(0.5F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    private ModBlocks() {
    }
}
