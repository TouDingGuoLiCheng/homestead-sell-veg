package com.homestead.sellveg.block;

import com.homestead.sellveg.block.entity.DropPointBE;
import com.homestead.sellveg.data.DropPointIndex;
import com.homestead.sellveg.network.ModNetwork;
import com.homestead.sellveg.network.OpenDropPointNameScreenPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

public class DropPointBlock extends BaseEntityBlock {
    public DropPointBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DropPointBE(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide || !(placer instanceof ServerPlayer player)) {
            return;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof DropPointBE dropPoint) {
            dropPoint.setOwnerId(player.getUUID());
            dropPoint.setDropPointName(player.getGameProfile().getName() + "'s Point");
            dropPoint.getOrCreatePointId();
            DropPointIndex.register(player, dropPoint);
            ModNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new OpenDropPointNameScreenPacket(pos, dropPoint.getDropPointName())
            );
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof DropPointBE dropPoint && dropPoint.getPointId() != null) {
                ServerPlayer owner = dropPoint.getOwnerId() == null
                        ? null
                        : serverLevel.getServer().getPlayerList().getPlayer(dropPoint.getOwnerId());
                DropPointIndex.unregister(owner, dropPoint.getPointId());
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
