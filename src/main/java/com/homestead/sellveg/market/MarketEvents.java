package com.homestead.sellveg.market;

import com.homestead.sellveg.data.DropPointIndex;
import com.homestead.sellveg.data.PlayerNetShopData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.SleepFinishedTimeEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Market day rolls on overworld calendar dawn and on sleep-to-morning.
 * {@link MarketData#getLastSettledWorldDay()} tracks the last world day that was
 * settled; it is never allowed to stay above the current world day (daytick / legacy
 * NBT can push market {@code dayIndex} ahead — that must not block dawn settlement).
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MarketEvents {
    /** Last overworld day index observed; {@code Long.MIN_VALUE} = unset this server run. */
    private static long lastSeenOverworldDay = Long.MIN_VALUE;

    private MarketEvents() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        FoodRecipeIndex.rebuild(event.getServer());
        lastSeenOverworldDay = Long.MIN_VALUE;
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) {
            return;
        }
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            FoodRecipeIndex.rebuild(server);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide()) {
            return;
        }
        if (event.level.dimension() != Level.OVERWORLD) {
            return;
        }
        ServerLevel overworld = (ServerLevel) event.level;
        long day = overworldDayIndex(overworld);
        if (day == lastSeenOverworldDay) {
            return;
        }
        if (lastSeenOverworldDay != Long.MIN_VALUE && day > lastSeenOverworldDay) {
            onWorldDayAdvanced(overworld.getServer(), day);
        }
        lastSeenOverworldDay = day;
    }

    /**
     * Sleep skip: always roll the market once, even if a pack keeps the same calendar
     * day index, or market day was pushed ahead by {@code /sellveg daytick}.
     */
    @SubscribeEvent
    public static void onSleepFinished(SleepFinishedTimeEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.isClientSide()) {
            return;
        }
        if (level.dimension() != Level.OVERWORLD) {
            return;
        }
        long newDay = event.getNewTime() / 24000L;
        MinecraftServer server = level.getServer();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            rollForSleep(player, newDay);
        }
        if (lastSeenOverworldDay == Long.MIN_VALUE || newDay > lastSeenOverworldDay) {
            lastSeenOverworldDay = newDay;
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ensureMarketInitialized(player);
        DropPointIndex.pruneMissing(player);
        com.homestead.sellveg.network.QuickListPriceSyncPacket.send(player);
    }

    public static void forceNewDay(ServerLevel level) {
        MinecraftServer server = level.getServer();
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        long worldDay = overworldDayIndex(overworld != null ? overworld : level);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PlayerNetShopData data = PlayerNetShopData.get(player);
            long next = Math.max(1L, data.market().getDayIndex() + 1L);
            data.market().applyEndOfDayAdjustments();
            data.market().rollNewDay(next, player.getRandom());
            // Keep settled at real world day so the next dawn/sleep can still settle.
            data.market().markSettledWorldDay(worldDay);
            PlayerNetShopData.save(player, data);
        }
    }

    private static void onWorldDayAdvanced(MinecraftServer server, long day) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            settleToWorldDay(player, day);
        }
    }

    public static void ensureMarketInitialized(ServerPlayer player) {
        PlayerNetShopData data = PlayerNetShopData.get(player);
        long day = overworldDayIndex(player);
        healSettledAheadOfWorld(data, day);
        if (data.market().syncPricingRules(player.getRandom())) {
            PlayerNetShopData.save(player, data);
        }
        if (!data.market().isInitialized()) {
            data.market().rollNewDay(day, player.getRandom());
            data.market().markSettledWorldDay(day);
            PlayerNetShopData.save(player, data);
            return;
        }
        if (data.market().getLastSettledWorldDay() < day) {
            settleToWorldDay(player, day);
            data = PlayerNetShopData.get(player);
        }
        if ((data.market().hotItems().isEmpty() && data.market().saleItems().isEmpty())
                || data.market().saleSpreadInvalid()) {
            data.market().rollActivities(player.getRandom());
            PlayerNetShopData.save(player, data);
        }
        if (data.market().gearEnchantOffers().isEmpty()) {
            data.market().rollGearEnchantOffers(player.getRandom());
            PlayerNetShopData.save(player, data);
        }
    }

    /**
     * Calendar dawn: settle when world day is ahead of last settled world day.
     */
    public static void settleToWorldDay(ServerPlayer player, long worldDay) {
        PlayerNetShopData data = PlayerNetShopData.get(player);
        healSettledAheadOfWorld(data, worldDay);
        if (!data.market().isInitialized()) {
            data.market().rollNewDay(worldDay, player.getRandom());
            data.market().markSettledWorldDay(worldDay);
            PlayerNetShopData.save(player, data);
            com.homestead.sellveg.network.QuickListPriceSyncPacket.send(player);
            return;
        }
        if (data.market().getLastSettledWorldDay() >= worldDay) {
            return;
        }
        data.market().applyEndOfDayAdjustments();
        data.market().rollNewDay(worldDay, player.getRandom());
        data.market().markSettledWorldDay(worldDay);
        PlayerNetShopData.save(player, data);
        com.homestead.sellveg.network.QuickListPriceSyncPacket.send(player);
    }

    /**
     * Sleep morning: always apply today's volume and bump market day, then mark the
     * post-sleep world day settled (may equal previous calendar day on some packs).
     */
    static void rollForSleep(ServerPlayer player, long worldDayAfterSleep) {
        PlayerNetShopData data = PlayerNetShopData.get(player);
        healSettledAheadOfWorld(data, worldDayAfterSleep);
        if (!data.market().isInitialized()) {
            data.market().rollNewDay(worldDayAfterSleep, player.getRandom());
            data.market().markSettledWorldDay(worldDayAfterSleep);
            PlayerNetShopData.save(player, data);
            com.homestead.sellveg.network.QuickListPriceSyncPacket.send(player);
            return;
        }
        data.market().applyEndOfDayAdjustments();
        long nextMarketDay = Math.max(worldDayAfterSleep, data.market().getDayIndex() + 1L);
        data.market().rollNewDay(nextMarketDay, player.getRandom());
        data.market().markSettledWorldDay(worldDayAfterSleep);
        PlayerNetShopData.save(player, data);
        com.homestead.sellveg.network.QuickListPriceSyncPacket.send(player);
    }

    /** Legacy / daytick: settled must not stay above the real world day. */
    static void healSettledAheadOfWorld(PlayerNetShopData data, long worldDay) {
        if (data.market().getLastSettledWorldDay() > worldDay) {
            // Force the next settle check to run for the current world day.
            data.market().markSettledWorldDay(worldDay - 1L);
        }
    }

    /** @deprecated use {@link #settleToWorldDay} */
    public static void advancePlayerDay(ServerPlayer player, long day) {
        settleToWorldDay(player, day);
    }

    public static long overworldDayIndex(ServerPlayer player) {
        ServerLevel overworld = player.server.getLevel(Level.OVERWORLD);
        if (overworld != null) {
            return overworldDayIndex(overworld);
        }
        return player.serverLevel().getDayTime() / 24000L;
    }

    public static long overworldDayIndex(ServerLevel overworld) {
        return overworld.getDayTime() / 24000L;
    }
}
