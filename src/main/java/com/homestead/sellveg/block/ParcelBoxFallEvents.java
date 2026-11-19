package com.homestead.sellveg.block;

import com.homestead.sellveg.NetShopMod;
import com.homestead.sellveg.block.entity.ParcelBoxBE;
import com.homestead.sellveg.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Some fail paths discard the falling entity without {@code onBrokenAfterFall}.
 * Salvage cargo unless this entity just placed an empty box awaiting BE merge.
 */
@Mod.EventBusSubscriber(modid = NetShopMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ParcelBoxFallEvents {
    private ParcelBoxFallEvents() {
    }

    @SubscribeEvent
    public static void onFallingLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof FallingBlockEntity falling)) {
            return;
        }
        if (!ParcelBoxBlock.isOurFallingParcel(falling)) {
            return;
        }
        if (!ParcelBoxBlock.hasFallCargo(falling)) {
            return;
        }
        Level level = event.getLevel();
        BlockPos pos = falling.blockPosition();
        boolean parcelAtPos = level.getBlockState(pos).is(ModBlocks.PARCEL_BOX.get());
        boolean placedBeEmpty = false;
        if (parcelAtPos) {
            BlockEntity be = level.getBlockEntity(pos);
            placedBeEmpty = be instanceof ParcelBoxBE parcel && parcel.isEmpty();
        }
        if (ParcelBoxBlock.shouldDeferScatterForMerge(parcelAtPos, placedBeEmpty, true)) {
            // Real land: keep blockData for vanilla BE merge; onLand clears the mirror key.
            return;
        }
        ParcelBoxBlock.scatterFallCargo(level, pos, falling);
    }
}
