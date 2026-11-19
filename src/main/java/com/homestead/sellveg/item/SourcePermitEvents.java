package com.homestead.sellveg.item;

import com.homestead.sellveg.NetShopMod;
import com.homestead.sellveg.stock.StockIndex;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = NetShopMod.MOD_ID)
public final class SourcePermitEvents {
    private SourcePermitEvents() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (!(event.getItemStack().getItem() instanceof SourcePermitItem)) {
            return;
        }
        BlockEntity blockEntity = event.getLevel().getBlockEntity(event.getPos());
        if (blockEntity == null || !StockIndex.isMarkable(blockEntity)) {
            return;
        }
        event.setUseBlock(Event.Result.DENY);
    }
}
