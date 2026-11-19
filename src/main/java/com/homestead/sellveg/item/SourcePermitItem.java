package com.homestead.sellveg.item;

import com.homestead.sellveg.stock.SourcePermitData;
import com.homestead.sellveg.stock.SourcePermitMark;
import com.homestead.sellveg.stock.SourcePermitMarks;
import com.homestead.sellveg.stock.StockIndex;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SourcePermitItem extends Item {
    public SourcePermitItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!isSelected || level.isClientSide() || level.getGameTime() % 40 != 0) {
            return;
        }
        List<SourcePermitMark> marks = SourcePermitData.read(stack);
        boolean changed = false;
        for (int i = marks.size() - 1; i >= 0; i--) {
            SourcePermitMark mark = marks.get(i);
            if (!SourcePermitData.matchesDimension(mark, level)) {
                continue;
            }
            BlockPos pos = SourcePermitData.toPos(mark);
            if (!level.isLoaded(pos)) {
                continue;
            }
            BlockEntity be = level.getBlockEntity(pos);
            if (be == null || !StockIndex.isMarkable(be)) {
                marks.remove(i);
                changed = true;
            }
        }
        if (changed) {
            SourcePermitData.write(stack, marks);
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || context.getHand() != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        ItemStack stack = context.getItemInHand();
        Level level = context.getLevel();
        if (player.isShiftKeyDown()) {
            return clearAll(level, player, stack);
        }
        BlockPos pos = context.getClickedPos();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null || !StockIndex.isMarkable(blockEntity)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        List<SourcePermitMark> marks = SourcePermitData.read(stack);
        SourcePermitMark mark = SourcePermitData.of(level, pos);
        SourcePermitMarks.ToggleResult result = SourcePermitMarks.toggle(marks, mark);
        SourcePermitData.write(stack, marks);
        playFeedback(level, player, pos);
        if (result == SourcePermitMarks.ToggleResult.ADDED) {
            player.displayClientMessage(Component.translatable("message.homestead_sell_veg.source_marked"), true);
        } else {
            player.displayClientMessage(Component.translatable("message.homestead_sell_veg.source_unmarked"), true);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }
        InteractionResult clear = clearAll(level, player, stack);
        if (clear.consumesAction()) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        return InteractionResultHolder.pass(stack);
    }

    private static InteractionResult clearAll(Level level, Player player, ItemStack stack) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        List<SourcePermitMark> marks = SourcePermitData.read(stack);
        SourcePermitMarks.clear(marks);
        SourcePermitData.write(stack, marks);
        level.playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.get(),
                SoundSource.PLAYERS, 0.4F, 1.2F);
        player.displayClientMessage(Component.translatable("message.homestead_sell_veg.source_cleared"), true);
        return InteractionResult.CONSUME;
    }

    private static void playFeedback(Level level, Player player, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.get(), SoundSource.PLAYERS, 0.5F, 1.1F);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.homestead_sell_veg.source_permit.lore"));
        tooltip.add(Component.translatable("item.homestead_sell_veg.source_permit.marks",
                SourcePermitData.read(stack).size()));
    }
}
