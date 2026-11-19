package com.homestead.sellveg.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public final class SellVegConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.DoubleValue BUY_PRICE_MULTIPLIER = BUILDER
            .comment("Mall buy price = market reference price * this multiplier.")
            .defineInRange("buyPriceMultiplier", 1.15D, 1.0D, 10.0D);

    public static final ForgeConfigSpec.DoubleValue MARKET_MIN_RATIO = BUILDER
            .comment("Daily market price floor as ratio of base price.")
            .defineInRange("marketMinRatio", 0.5D, 0.01D, 1.0D);

    public static final ForgeConfigSpec.DoubleValue MARKET_MAX_RATIO = BUILDER
            .comment("Daily market price cap as ratio of base price.")
            .defineInRange("marketMaxRatio", 2.0D, 1.0D, 10.0D);

    public static final ForgeConfigSpec.IntValue MARKET_VOLUME_UNIT = BUILDER
            .comment("Item count that reaches the max daily band shift for items at/above the price anchor (default 256).")
            .defineInRange("marketVolumeUnit", 256, 1, 1_000_000);

    public static final ForgeConfigSpec.DoubleValue MARKET_MAX_SHIFT_RATIO = BUILDER
            .comment("Max daily band slide as a fraction of the original band midpoint.")
            .defineInRange("marketMaxShiftRatio", 0.5D, 0.0D, 1.0D);

    public static final ForgeConfigSpec.DoubleValue MARKET_VOLUME_PRICE_ANCHOR = BUILDER
            .comment("Original-band midpoint at which volume weight reaches 1.0 (default 128). Cheaper goods scale down toward marketVolumeWeightMin.")
            .defineInRange("marketVolumePriceAnchor", 128.0D, 1.0D, 1_000_000.0D);

    public static final ForgeConfigSpec.DoubleValue MARKET_VOLUME_WEIGHT_MIN = BUILDER
            .comment("Floor for volume weight. With defaults, ~640 trades hit the max daily shift for cheap goods (0.5 * 256 / 640 = 0.2).")
            .defineInRange("marketVolumeWeightMin", 0.2D, 0.01D, 1.0D);

    public static final ForgeConfigSpec.DoubleValue MARKET_IDLE_RECOVER = BUILDER
            .comment("When an item has no net buy/sell that day, lerp the live band toward the original band.")
            .defineInRange("marketIdleRecover", 0.15D, 0.0D, 1.0D);

    public static final ForgeConfigSpec.IntValue BACKEND_SLOT_CAP = BUILDER
            .comment("Virtual backend slot cap per merged item line.")
            .defineInRange("backendSlotCap", 10_000, 64, 1_000_000);

    public static final ForgeConfigSpec.IntValue SALE_DAILY_CAP = BUILDER
            .comment("Max discounted (sale) items a player can buy per in-game day.")
            .defineInRange("saleDailyCap", 64, 1, 4096);

    public static final ForgeConfigSpec.IntValue LIST_MAX_COUNT = BUILDER
            .comment("Maximum items per listing action.")
            .defineInRange("listMaxCount", 64, 1, 64);

    public static final ForgeConfigSpec.IntValue LISTING_BASE_DELAY_TICKS = BUILDER
            .comment("Base listing delay in ticks at market reference (exponential in price ratio).")
            .defineInRange("listingBaseDelayTicks", 6000, 1, 720000);

    public static final ForgeConfigSpec.IntValue LISTING_MIN_DELAY_TICKS = BUILDER
            .comment("Minimum listing delay in ticks.")
            .defineInRange("listingMinDelayTicks", 20, 0, 720000);

    public static final ForgeConfigSpec.IntValue LISTING_MAX_DELAY_TICKS = BUILDER
            .comment("Unused legacy field; delay no longer hard-caps (unsaleable ratio applies instead).")
            .defineInRange("listingMaxDelayTicks", 72000, 20, 720000);

    public static final ForgeConfigSpec.DoubleValue LISTING_UNSALEABLE_RATIO = BUILDER
            .comment("If unit price is at or above live-band high times this, the listing never sells (can cancel). Wait still uses price/reference.")
            .defineInRange("listingUnsaleableRatio", 2.0D, 1.01D, 100.0D);

    public static final ForgeConfigSpec.DoubleValue INSTANT_SELL_RATIO = BUILDER
            .comment("If playerPrice/marketRef is at or below this, instant-sell chance applies.")
            .defineInRange("instantSellRatio", 0.85D, 0.01D, 1.0D);

    public static final ForgeConfigSpec.DoubleValue INSTANT_SELL_CHANCE = BUILDER
            .comment("Chance to sell immediately when underpriced.")
            .defineInRange("instantSellChance", 0.5D, 0.0D, 1.0D);

    public static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> SHOP_UPGRADE_THRESHOLDS = BUILDER
            .comment("Cumulative sell revenue to reach shop levels 1-5.")
            .defineList("shopUpgradeThresholds", List.of(5000, 10000, 100000, 500000, 1000000), o -> o instanceof Number);

    public static final ForgeConfigSpec.IntValue BOX_SLOT_CAPACITY = BUILDER
            .comment("Inventory slots per parcel box.")
            .defineInRange("boxSlotCapacity", 27, 1, 54);

    public static final ForgeConfigSpec.IntValue DELIVERY_MIN_TICKS = BUILDER
            .comment("Minimum ticks before a parcel arrives.")
            .defineInRange("deliveryMinTicks", 2400, 1, 720000);

    public static final ForgeConfigSpec.IntValue DELIVERY_MAX_TICKS = BUILDER
            .comment("Maximum ticks before a parcel arrives.")
            .defineInRange("deliveryMaxTicks", 12000, 1, 720000);

    public static final ForgeConfigSpec.IntValue DELIVERY_RETRY_TICKS = BUILDER
            .comment("Retry delay when the drop column is blocked.")
            .defineInRange("deliveryRetryTicks", 200, 1, 72000);

    public static final ForgeConfigSpec.IntValue DELIVERY_COLUMN_MAX = BUILDER
            .comment("Treat the column as full after this many blocks above the drop point.")
            .defineInRange("deliveryColumnMax", 32, 1, 128);

    public static final ForgeConfigSpec.IntValue DELIVERY_PENDING_CAP = BUILDER
            .comment("Max in-transit parcel boxes per player. Checkout/buy-now refuse when full.")
            .defineInRange("deliveryPendingCap", 32, 1, 256);

    public static final ForgeConfigSpec.BooleanValue DELIVERY_CHAT_ENABLED = BUILDER
            .comment("Send chat messages on delivery or when the column is blocked. Sound is controlled separately.")
            .define("deliveryChatEnabled", true);

    public static final ForgeConfigSpec.BooleanValue DELIVERY_SOUND_ENABLED = BUILDER
            .comment("Play a short sound on delivery. Independent of deliveryChatEnabled.")
            .define("deliverySoundEnabled", true);

    public static final ForgeConfigSpec.DoubleValue HOT_SALE_DELAY_MULTIPLIER = BUILDER
            .comment("Listing delay multiplier for daily hot-sale items.")
            .defineInRange("hotSaleDelayMultiplier", 0.7D, 0.1D, 1.0D);

    public static final ForgeConfigSpec.DoubleValue SALE_BUY_MULTIPLIER = BUILDER
            .comment("Unused legacy field; daily sales now roll 10–99% off per shop listing.")
            .defineInRange("saleBuyMultiplier", 0.8D, 0.1D, 1.0D);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private SellVegConfig() {
    }
}
