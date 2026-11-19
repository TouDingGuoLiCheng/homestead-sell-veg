package com.homestead.sellveg.command;

import com.homestead.sellveg.data.DropPointIndex;
import com.homestead.sellveg.data.DropPointRecord;
import com.homestead.sellveg.data.PlayerNetShopData;
import com.homestead.sellveg.market.FoodRecipeIndex;
import com.homestead.sellveg.market.MarketEvents;
import com.homestead.sellveg.market.PriceCatalog;
import com.homestead.sellveg.order.CartData;
import com.homestead.sellveg.order.CheckoutService;
import com.homestead.sellveg.order.DeliveryService;
import com.homestead.sellveg.order.Shipment;
import com.homestead.sellveg.shop.Listing;
import com.homestead.sellveg.shop.ListingService;
import com.homestead.sellveg.network.ShopSyncPacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class SellVegCommands {
    private SellVegCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register(Commands.literal("sellveg")
                .then(Commands.literal("list")
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 64 * 36))
                                .then(Commands.argument("price", IntegerArgumentType.integer(1))
                                        .executes(ctx -> listHeld(
                                                ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "count"),
                                                IntegerArgumentType.getInteger(ctx, "price"))))))
                .then(Commands.literal("listings")
                        .executes(ctx -> showListings(ctx.getSource())))
                .then(Commands.literal("cancel")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(ctx -> cancel(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                .then(Commands.literal("shop")
                        .executes(ctx -> showShop(ctx.getSource())))
                .then(Commands.literal("cartadd")
                        .then(Commands.argument("item", ItemArgument.item(context))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 64 * 54))
                                        .executes(ctx -> cartAdd(
                                                ctx.getSource(),
                                                ItemArgument.getItem(ctx, "item").getItem(),
                                                IntegerArgumentType.getInteger(ctx, "count"))))))
                .then(Commands.literal("cart")
                        .executes(ctx -> showCart(ctx.getSource())))
                .then(Commands.literal("cartdel")
                        .then(Commands.argument("item", ItemArgument.item(context))
                                .executes(ctx -> cartDel(ctx.getSource(), ItemArgument.getItem(ctx, "item").getItem()))))
                .then(Commands.literal("checkout")
                        .executes(ctx -> checkout(ctx.getSource())))
                .then(Commands.literal("logistics")
                        .executes(ctx -> logistics(ctx.getSource())))
                .then(Commands.literal("droppoints")
                        .executes(ctx -> droppoints(ctx.getSource())))
                .then(Commands.literal("default")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(ctx -> setDefault(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                .then(Commands.literal("rushmail")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> rushmail(ctx.getSource())))
                .then(Commands.literal("balance")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("amount", LongArgumentType.longArg(0))
                                .executes(ctx -> setBalance(
                                        ctx.getSource(),
                                        ctx.getSource().getPlayerOrException(),
                                        LongArgumentType.getLong(ctx, "amount")))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> setBalance(
                                                ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player"),
                                                LongArgumentType.getLong(ctx, "amount"))))))
                .then(Commands.literal("price")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("item", ItemArgument.item(context))
                                .executes(ctx -> {
                                    ItemStack stack = ItemArgument.getItem(ctx, "item").createItemStack(1, false);
                                    return showPrice(ctx.getSource(), stack);
                                })))
                .then(Commands.literal("sold")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("item", ItemArgument.item(context))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            ItemStack stack = ItemArgument.getItem(ctx, "item").createItemStack(1, false);
                                            int count = IntegerArgumentType.getInteger(ctx, "count");
                                            PlayerNetShopData data = PlayerNetShopData.get(player);
                                            data.market().recordSold(stack.getItem(), count);
                                            PlayerNetShopData.save(player, data);
                                            ctx.getSource().sendSuccess(() -> Component.literal("Recorded sold "
                                                    + count + " " + stack.getHoverName().getString()), true);
                                            return 1;
                                        }))))
                .then(Commands.literal("daytick")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            MarketEvents.forceNewDay(player.serverLevel());
                            ctx.getSource().sendSuccess(() -> Component.literal("Forced market day tick."), true);
                            return 1;
                        }))
                .then(Commands.literal("tradeable")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("id", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                                            new net.minecraft.resources.ResourceLocation(StringArgumentType.getString(ctx, "id")));
                                    if (item == null) {
                                        ctx.getSource().sendFailure(Component.literal("Unknown item."));
                                        return 0;
                                    }
                                    boolean ok = PriceCatalog.isTradeable(new ItemStack(item));
                                    ctx.getSource().sendSuccess(() -> Component.literal(String.valueOf(ok)), false);
                                    return 1;
                                }))));
    }

    private static int listHeld(CommandSourceStack source, int count, int price) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ListingService.ListResult result = ListingService.createFromHeld(player, count, price);
        switch (result) {
            case OK -> source.sendSuccess(() -> Component.translatable("message.homestead_sell_veg.listing_created"), false);
            case EMPTY_HAND -> source.sendFailure(Component.translatable("message.homestead_sell_veg.listing_empty_hand"));
            case NOT_TRADEABLE -> source.sendFailure(Component.translatable("message.homestead_sell_veg.listing_not_tradeable"));
            case NOT_ENOUGH_ITEMS -> source.sendFailure(Component.translatable("message.homestead_sell_veg.listing_not_enough"));
            case SLOTS_FULL -> source.sendFailure(Component.translatable("message.homestead_sell_veg.listing_slots_full"));
            case BAD_PRICE -> source.sendFailure(Component.translatable("message.homestead_sell_veg.listing_bad_price"));
        }
        return result == ListingService.ListResult.OK ? 1 : 0;
    }

    private static int showListings(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PlayerNetShopData data = PlayerNetShopData.get(player);
        List<Listing> listings = data.shop().listings();
        if (listings.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("message.homestead_sell_veg.listings_empty"), false);
            return 1;
        }
        source.sendSuccess(() -> Component.translatable(
                "message.homestead_sell_veg.listings_header",
                listings.size(),
                data.shop().getListingCapacity()), false);
        for (Listing listing : listings) {
            source.sendSuccess(() -> Component.translatable(
                    "message.homestead_sell_veg.listing_row",
                    listing.getStack().getHoverName(),
                    listing.getStack().getCount(),
                    listing.getUnitPrice()), false);
        }
        return 1;
    }

    private static int cancel(CommandSourceStack source, String idText) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Optional<UUID> id = resolveListingId(PlayerNetShopData.get(player).shop().listings(), idText);
        if (id.isEmpty()) {
            source.sendFailure(Component.translatable("message.homestead_sell_veg.listing_unknown"));
            return 0;
        }
        if (!ListingService.cancel(player, id.get())) {
            source.sendFailure(Component.translatable("message.homestead_sell_veg.listing_unknown"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("message.homestead_sell_veg.listing_cancelled"), false);
        return 1;
    }

    private static int showShop(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PlayerNetShopData data = PlayerNetShopData.get(player);
        source.sendSuccess(() -> Component.translatable(
                "message.homestead_sell_veg.shop_status",
                data.shop().getLevel(),
                data.shop().listings().size(),
                data.shop().getListingCapacity(),
                data.shop().getSoldTotal()), false);
        return 1;
    }

    private static Optional<UUID> resolveListingId(List<Listing> listings, String idText) {
        String needle = idText.toLowerCase(Locale.ROOT);
        Optional<UUID> found = Optional.empty();
        for (Listing listing : listings) {
            String full = listing.getId().toString().toLowerCase(Locale.ROOT);
            if (full.equals(needle) || full.startsWith(needle)) {
                if (found.isPresent()) {
                    return Optional.empty();
                }
                found = Optional.of(listing.getId());
            }
        }
        return found;
    }

    private static String shortId(UUID id) {
        return id.toString().substring(0, 8);
    }

    private static int showPrice(CommandSourceStack source, ItemStack stack) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PlayerNetShopData data = PlayerNetShopData.get(player);
        MarketEvents.ensureMarketInitialized(player);
        data = PlayerNetShopData.get(player);
        Item item = stack.getItem();
        if (!PriceCatalog.isTradeable(stack)) {
            source.sendFailure(Component.literal("Not tradeable: " + stack.getHoverName().getString()));
            return 0;
        }
        int[] orig = PriceCatalog.marketBand(item);
        if (orig == null) {
            source.sendFailure(Component.literal("No price: " + stack.getHoverName().getString()));
            return 0;
        }
        int ref = data.market().getReferencePrice(item);
        int buy = data.market().getBuyPrice(item);
        int[] live = data.market().getLiveBand(item);
        int craft = FoodRecipeIndex.cheapestCraftCostPerResult(item);
        long marketDay = data.market().getDayIndex();
        long settled = data.market().getLastSettledWorldDay();
        long worldDay = MarketEvents.overworldDayIndex(player);
        source.sendSuccess(() -> Component.literal(stack.getHoverName().getString()
                + " ref=" + ref + " buy=" + buy
                + " orig=" + orig[0] + "-" + orig[1]
                + " live=" + live[0] + "-" + live[1]
                + " craft=" + craft
                + " day=" + marketDay + " settled=" + settled + " world=" + worldDay), false);
        return 1;
    }

    private static int cartAdd(CommandSourceStack source, Item item, int count) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CheckoutService.Result result = CheckoutService.addToCart(player, item, count);
        if (result != CheckoutService.Result.OK) {
            source.sendFailure(Component.translatable("message.homestead_sell_veg.listing_not_tradeable"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("message.homestead_sell_veg.cart_added"), false);
        return 1;
    }

    private static int showCart(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PlayerNetShopData data = PlayerNetShopData.get(player);
        if (data.cart().isEmpty()) {
            source.sendSuccess(() -> Component.translatable("message.homestead_sell_veg.cart_empty"), false);
            return 1;
        }
        long total = CheckoutService.cartTotal(player, data);
        data = PlayerNetShopData.get(player);
        source.sendSuccess(() -> Component.translatable("message.homestead_sell_veg.cart_header", total), false);
        for (CartData.Line line : data.cart().lines()) {
            source.sendSuccess(() -> Component.literal(line.shopId + " " + line.stack.getHoverName().getString()
                    + " x" + line.count + " @" + line.unitPrice), false);
        }
        return 1;
    }

    private static int cartDel(CommandSourceStack source, Item item) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PlayerNetShopData data = PlayerNetShopData.get(player);
        if (!data.cart().lines().removeIf(line -> line.item() == item)) {
            source.sendFailure(Component.translatable("message.homestead_sell_veg.cart_empty"));
            return 0;
        }
        PlayerNetShopData.save(player, data);
        source.sendSuccess(() -> Component.translatable("message.homestead_sell_veg.cart_removed"), false);
        return 1;
    }

    private static int checkout(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CheckoutService.Result result = CheckoutService.checkout(player);
        return switch (result) {
            case OK -> 1;
            case EMPTY_CART -> {
                source.sendFailure(Component.translatable("message.homestead_sell_veg.cart_empty"));
                yield 0;
            }
            case NO_DROP_POINT -> {
                source.sendFailure(Component.translatable("message.homestead_sell_veg.checkout_no_point"));
                yield 0;
            }
            case NOT_ENOUGH_COINS -> {
                source.sendFailure(Component.translatable("message.homestead_sell_veg.checkout_no_coins"));
                yield 0;
            }
            case NOT_TRADEABLE -> {
                source.sendFailure(Component.translatable("message.homestead_sell_veg.listing_not_tradeable"));
                yield 0;
            }
            case SALE_DAILY_LIMIT -> {
                source.sendFailure(Component.translatable("message.homestead_sell_veg.sale_daily_limit"));
                yield 0;
            }
            case SHIPMENTS_FULL -> {
                source.sendFailure(Component.translatable("message.homestead_sell_veg.checkout_shipments_full"));
                yield 0;
            }
        };
    }

    private static int logistics(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PlayerNetShopData data = PlayerNetShopData.get(player);
        DropPointRecord def = DropPointIndex.resolveDefault(player, data);
        PlayerNetShopData.save(player, data);
        String name = def == null ? "-" : def.name;
        boolean blocked = data.shipments().stream().anyMatch(Shipment::isBlocked);
        source.sendSuccess(() -> Component.translatable(
                "message.homestead_sell_veg.logistics",
                data.shipments().size(),
                name,
                blocked ? "blocked" : "ok"), false);
        return 1;
    }

    private static int droppoints(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PlayerNetShopData data = PlayerNetShopData.get(player);
        DropPointIndex.pruneMissing(player);
        data = PlayerNetShopData.get(player);
        if (data.dropPoints().isEmpty()) {
            source.sendSuccess(() -> Component.translatable("message.homestead_sell_veg.droppoints_empty"), false);
            return 1;
        }
        UUID def = data.getDefaultDropPointId();
        for (DropPointRecord record : data.dropPoints()) {
            String mark = record.id.equals(def) ? "*" : " ";
            source.sendSuccess(() -> Component.literal(mark + " " + shortId(record.id) + " " + record.name
                    + " " + record.pos.toShortString()), false);
        }
        return 1;
    }

    private static int setDefault(CommandSourceStack source, String idText) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PlayerNetShopData data = PlayerNetShopData.get(player);
        Optional<UUID> id = Optional.empty();
        String needle = idText.toLowerCase(Locale.ROOT);
        for (DropPointRecord record : data.dropPoints()) {
            String full = record.id.toString().toLowerCase(Locale.ROOT);
            if (full.equals(needle) || full.startsWith(needle)) {
                if (id.isPresent()) {
                    source.sendFailure(Component.translatable("message.homestead_sell_veg.listing_unknown"));
                    return 0;
                }
                id = Optional.of(record.id);
            }
        }
        if (id.isEmpty()) {
            source.sendFailure(Component.translatable("message.homestead_sell_veg.listing_unknown"));
            return 0;
        }
        data.setDefaultDropPointId(id.get());
        PlayerNetShopData.save(player, data);
        source.sendSuccess(() -> Component.translatable("message.homestead_sell_veg.default_set"), false);
        return 1;
    }

    private static int setBalance(CommandSourceStack source, ServerPlayer target, long amount) {
        PlayerNetShopData data = PlayerNetShopData.get(target);
        long next = data.wallet().applySet(amount, "cmd", System.currentTimeMillis());
        PlayerNetShopData.save(target, data);
        ShopSyncPacket.send(target, false);
        source.sendSuccess(() -> Component.translatable(
                "message.homestead_sell_veg.balance_set",
                target.getGameProfile().getName(),
                next), true);
        return 1;
    }

    private static int rushmail(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        DeliveryService.rushAll(player);
        source.sendSuccess(() -> Component.literal("Rushing in-transit parcels."), true);
        return 1;
    }
}
