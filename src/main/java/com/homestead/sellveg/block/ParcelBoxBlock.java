package com.homestead.sellveg.block;

import com.homestead.sellveg.block.entity.ParcelBoxBE;
import com.homestead.sellveg.registry.ModBlocks;
import com.homestead.sellveg.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class ParcelBoxBlock extends FallingBlock implements EntityBlock {
    static final String FALL_CARGO_KEY = "HomesteadParcelCargo";

    private static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 14.0D, 15.0D);

    public ParcelBoxBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /**
     * Visual carton is 14/16 tall; collision must be a full cube or stacked falling
     * boxes land inside the block below and fail to place (they break from the 2nd above).
     */
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.block();
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ParcelBoxBE(pos, state);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!isFree(level.getBlockState(pos.below())) || pos.getY() < level.getMinBuildHeight()) {
            return;
        }
        CompoundTag data = null;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ParcelBoxBE parcel) {
            data = parcel.saveWithoutMetadata();
        }
        FallingBlockEntity falling = FallingBlockEntity.fall(level, pos, state);
        if (data != null && !data.isEmpty()) {
            falling.blockData = data;
            // Mirror cargo: vanilla may clear blockData on some break paths before our hook runs.
            falling.getPersistentData().put(FALL_CARGO_KEY, data.copy());
        }
        this.falling(falling);
    }

    /**
     * Do not let vanilla drop an empty parcel-box item. Failed lands scatter cargo only.
     */
    @Override
    protected void falling(FallingBlockEntity entity) {
        entity.dropItem = false;
    }

    @Override
    public void onLand(Level level, BlockPos pos, BlockState state, BlockState replaced, FallingBlockEntity falling) {
        // Successful place: BE merge uses blockData; clear the mirror so leave-hooks do not scatter.
        falling.getPersistentData().remove(FALL_CARGO_KEY);
    }

    /**
     * Torch / non-solid landings break the fall (like sand). Scatter goods, never a box item.
     */
    @Override
    public void onBrokenAfterFall(Level level, BlockPos pos, FallingBlockEntity falling) {
        scatterFallCargo(level, pos, falling);
    }

    /**
     * @return true if any cargo was dropped
     */
    static boolean scatterFallCargo(Level level, BlockPos pos, FallingBlockEntity falling) {
        if (level.isClientSide) {
            return false;
        }
        CompoundTag data = takeFallCargo(falling);
        if (data == null || data.isEmpty()) {
            return false;
        }
        NonNullList<ItemStack> items = ParcelBoxBE.readItemsFromTag(data);
        boolean any = false;
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                popItem(level, pos, stack.copy());
                any = true;
            }
        }
        return any;
    }

    @Nullable
    private static CompoundTag takeFallCargo(FallingBlockEntity falling) {
        CompoundTag data = falling.blockData;
        if (data == null || data.isEmpty()) {
            CompoundTag mirrored = falling.getPersistentData().getCompound(FALL_CARGO_KEY);
            data = mirrored.isEmpty() ? null : mirrored;
        }
        falling.blockData = null;
        falling.getPersistentData().remove(FALL_CARGO_KEY);
        return data;
    }

    static boolean isOurFallingParcel(FallingBlockEntity falling) {
        return falling.getBlockState().is(ModBlocks.PARCEL_BOX.get());
    }

    /**
     * After {@code setBlock}, discard runs before BE merge. Skip scatter only for that empty
     * waiting box — not when overlapping another filled (or foreign) parcel.
     */
    static boolean shouldDeferScatterForMerge(boolean parcelAtPos, boolean placedBeEmpty, boolean hasFallCargo) {
        return ParcelFallMergeMath.shouldDeferScatterForMerge(parcelAtPos, placedBeEmpty, hasFallCargo);
    }

    static boolean hasFallCargo(FallingBlockEntity falling) {
        if (falling.blockData != null && !falling.blockData.isEmpty()) {
            return true;
        }
        return falling.getPersistentData().contains(FALL_CARGO_KEY);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        openAndScatter(level, pos);
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player,
                                       boolean willHarvest, FluidState fluid) {
        if (!level.isClientSide) {
            scatterContents(level, pos);
            level.playSound(null, pos, ModSounds.PARCEL_BOX_OPEN.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }

    public static void openAndScatter(Level level, BlockPos pos) {
        scatterContents(level, pos);
        level.playSound(null, pos, ModSounds.PARCEL_BOX_OPEN.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }

    private static void scatterContents(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ParcelBoxBE parcel)) {
            return;
        }
        for (ItemStack stack : parcel.getItems()) {
            if (!stack.isEmpty()) {
                popItem(level, pos, stack.copy());
            }
        }
        parcel.clearContents();
    }

    private static void popItem(Level level, BlockPos pos, ItemStack stack) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        ItemEntity entity = new ItemEntity(level, x, y, z, stack);
        entity.setDefaultPickUpDelay();
        entity.setDeltaMovement(
                level.random.nextGaussian() * 0.05,
                0.2,
                level.random.nextGaussian() * 0.05
        );
        level.addFreshEntity(entity);
    }
}
