package com.homestead.sellveg.stock;

import com.homestead.sellveg.market.PriceCatalog;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class StockIndex {
    public record Snapshot(List<StockSlot> slots, int unloadedSources) {
    }

    public record StockSlot(ItemStack template, int count) {
    }

    private StockIndex() {
    }

    public static Snapshot scan(ServerPlayer player) {
        Map<String, Merged> merged = new LinkedHashMap<>();
        Inventory inv = player.getInventory();
        for (ItemStack stack : inv.items) {
            add(merged, stack);
        }
        for (ItemStack stack : inv.offhand) {
            add(merged, stack);
        }

        int unloaded = 0;
        ServerLevel level = player.serverLevel();
        for (SourcePermitMark mark : SourcePermitData.collectFromPlayer(player)) {
            if (!SourcePermitData.matchesDimension(mark, level)) {
                unloaded++;
                continue;
            }
            BlockPos pos = SourcePermitData.toPos(mark);
            if (!level.isLoaded(pos)) {
                unloaded++;
                continue;
            }
            BlockEntity be = level.getBlockEntity(pos);
            if (be == null || !isMarkable(be)) {
                continue;
            }
            addContainer(merged, be);
        }

        List<Merged> ordered = new ArrayList<>(merged.values());
        ordered.sort((a, b) -> Integer.compare(b.count, a.count));

        List<StockSlot> slots = new ArrayList<>();
        for (Merged row : ordered) {
            for (int chunk : StockPacker.split(row.count, slotCap())) {
                ItemStack template = row.template.copy();
                template.setCount(1);
                slots.add(new StockSlot(template, chunk));
            }
        }
        return new Snapshot(slots, unloaded);
    }

    public record TakeOutcome(boolean ok, List<StockDraw> draws) {
        public TakeOutcome {
            draws = draws == null ? List.of() : List.copyOf(draws);
        }
    }

    public static boolean take(ServerPlayer player, ItemStack template, int count) {
        return takeTracking(player, template, count).ok();
    }

    public static int available(ServerPlayer player, ItemStack template) {
        int total = 0;
        for (StockSlot slot : scan(player).slots()) {
            if (ItemStack.isSameItemSameTags(slot.template(), template)) {
                total += slot.count();
            }
        }
        return total;
    }

    /**
     * Take items and record how many came from the player vs each marked chest.
     */
    public static TakeOutcome takeTracking(ServerPlayer player, ItemStack template, int count) {
        if (count < 1 || template == null || template.isEmpty()) {
            return new TakeOutcome(false, List.of());
        }
        List<StockDraw> draws = new ArrayList<>();
        int remaining = count;
        int before = remaining;
        remaining = takeFromPlayer(player, template, remaining);
        int fromPlayer = before - remaining;
        if (fromPlayer > 0) {
            draws.add(StockDraw.player(fromPlayer));
        }
        if (remaining > 0) {
            remaining = takeFromMarkedTracking(player, template, remaining, draws);
        }
        if (remaining != 0) {
            // Should not happen after available() check; leave world as-is if partial (caller gates).
            return new TakeOutcome(false, draws);
        }
        return new TakeOutcome(true, draws);
    }

    /**
     * Put listed items back: chest portions into the same marks when possible, else player inventory.
     */
    public static void restore(ServerPlayer player, ItemStack template, List<StockDraw> draws) {
        if (template == null || template.isEmpty()) {
            return;
        }
        if (draws == null || draws.isEmpty()) {
            ItemStack all = template.copy();
            com.homestead.sellveg.inventory.PlayerItemHelper.giveOrDrop(player, all);
            return;
        }
        for (StockDraw draw : draws) {
            if (draw == null || draw.count() < 1) {
                continue;
            }
            ItemStack portion = template.copy();
            portion.setCount(draw.count());
            if (draw.fromPlayer()) {
                com.homestead.sellveg.inventory.PlayerItemHelper.giveOrDrop(player, portion);
                continue;
            }
            int left = insertIntoMark(player, draw.mark(), portion);
            if (left > 0) {
                portion.setCount(left);
                com.homestead.sellveg.inventory.PlayerItemHelper.giveOrDrop(player, portion);
            }
        }
    }

    private static int takeFromMarkedTracking(
            ServerPlayer player, ItemStack template, int remaining, List<StockDraw> draws) {
        ServerLevel level = player.serverLevel();
        for (SourcePermitMark mark : SourcePermitData.collectFromPlayer(player)) {
            if (remaining <= 0) {
                break;
            }
            if (!SourcePermitData.matchesDimension(mark, level)) {
                continue;
            }
            BlockPos pos = SourcePermitData.toPos(mark);
            if (!level.isLoaded(pos)) {
                continue;
            }
            BlockEntity be = level.getBlockEntity(pos);
            if (be == null) {
                continue;
            }
            int before = remaining;
            remaining = drainBlockEntity(be, template, remaining);
            int taken = before - remaining;
            if (taken > 0) {
                draws.add(StockDraw.chest(mark, taken));
            }
        }
        return remaining;
    }

    private static int insertIntoMark(ServerPlayer player, SourcePermitMark mark, ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        ServerLevel level = player.serverLevel();
        if (!SourcePermitData.matchesDimension(mark, level)) {
            return stack.getCount();
        }
        BlockPos pos = SourcePermitData.toPos(mark);
        if (!level.isLoaded(pos)) {
            return stack.getCount();
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null || !isMarkable(be)) {
            return stack.getCount();
        }
        return insertIntoBlockEntity(be, stack);
    }

    private static int insertIntoBlockEntity(BlockEntity be, ItemStack stack) {
        IItemHandler handler = be.getCapability(ForgeCapabilities.ITEM_HANDLER, null).orElse(null);
        if (handler != null) {
            ItemStack left = stack.copy();
            for (int i = 0; i < handler.getSlots() && !left.isEmpty(); i++) {
                left = handler.insertItem(i, left, false);
            }
            return left.getCount();
        }
        if (be instanceof Container container) {
            ItemStack left = stack.copy();
            for (int i = 0; i < container.getContainerSize() && !left.isEmpty(); i++) {
                ItemStack in = container.getItem(i);
                if (in.isEmpty()) {
                    int put = Math.min(left.getCount(), left.getMaxStackSize());
                    container.setItem(i, left.split(put));
                    continue;
                }
                if (!ItemStack.isSameItemSameTags(in, left)) {
                    continue;
                }
                int space = Math.min(in.getMaxStackSize() - in.getCount(), left.getCount());
                if (space <= 0) {
                    continue;
                }
                in.grow(space);
                left.shrink(space);
                container.setChanged();
            }
            return left.getCount();
        }
        return stack.getCount();
    }

    private static int takeFromPlayer(ServerPlayer player, ItemStack template, int remaining) {
        Inventory inv = player.getInventory();
        remaining = drainList(inv.items, template, remaining);
        if (remaining > 0) {
            remaining = drainList(inv.offhand, template, remaining);
        }
        return remaining;
    }

    private static int drainList(net.minecraft.core.NonNullList<ItemStack> list, ItemStack template, int remaining) {
        for (int i = 0; i < list.size() && remaining > 0; i++) {
            ItemStack stack = list.get(i);
            if (!ItemStack.isSameItemSameTags(stack, template)) {
                continue;
            }
            int take = Math.min(remaining, stack.getCount());
            stack.shrink(take);
            remaining -= take;
        }
        return remaining;
    }

    private static int drainBlockEntity(BlockEntity be, ItemStack template, int remaining) {
        IItemHandler handler = be.getCapability(ForgeCapabilities.ITEM_HANDLER, null).orElse(null);
        if (handler != null) {
            for (int i = 0; i < handler.getSlots() && remaining > 0; i++) {
                ItemStack in = handler.getStackInSlot(i);
                if (!ItemStack.isSameItemSameTags(in, template)) {
                    continue;
                }
                ItemStack extracted = handler.extractItem(i, remaining, false);
                remaining -= extracted.getCount();
            }
            return remaining;
        }
        if (be instanceof Container container) {
            for (int i = 0; i < container.getContainerSize() && remaining > 0; i++) {
                ItemStack in = container.getItem(i);
                if (!ItemStack.isSameItemSameTags(in, template)) {
                    continue;
                }
                int take = Math.min(remaining, in.getCount());
                in.shrink(take);
                remaining -= take;
                container.setChanged();
            }
        }
        return remaining;
    }

    private static void addContainer(Map<String, Merged> merged, BlockEntity be) {
        IItemHandler handler = be.getCapability(ForgeCapabilities.ITEM_HANDLER, null).orElse(null);
        if (handler != null) {
            for (int i = 0; i < handler.getSlots(); i++) {
                add(merged, handler.getStackInSlot(i));
            }
            return;
        }
        if (be instanceof Container container) {
            for (int i = 0; i < container.getContainerSize(); i++) {
                add(merged, container.getItem(i));
            }
        }
    }

    private static void add(Map<String, Merged> merged, ItemStack stack) {
        if (stack.isEmpty() || !PriceCatalog.isTradeable(stack)) {
            return;
        }
        String key = key(stack);
        Merged row = merged.get(key);
        if (row == null) {
            ItemStack template = stack.copy();
            template.setCount(1);
            merged.put(key, new Merged(template, stack.getCount()));
        } else {
            row.count += stack.getCount();
        }
    }

    private static String key(ItemStack stack) {
        return stack.getItem().toString() + "|" + String.valueOf(stack.getTag());
    }

    public static boolean isMarkable(BlockEntity be) {
        if (be instanceof Container) {
            return true;
        }
        return be.getCapability(ForgeCapabilities.ITEM_HANDLER, null).isPresent();
    }

    private static int slotCap() {
        try {
            return com.homestead.sellveg.config.SellVegConfig.BACKEND_SLOT_CAP.get();
        } catch (Exception ignored) {
            return StockPacker.SLOT_CAP;
        }
    }

    private static final class Merged {
        private final ItemStack template;
        private int count;

        private Merged(ItemStack template, int count) {
            this.template = template;
            this.count = count;
        }
    }
}
