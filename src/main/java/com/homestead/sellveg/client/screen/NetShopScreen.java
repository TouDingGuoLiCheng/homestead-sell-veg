package com.homestead.sellveg.client.screen;

import com.homestead.sellveg.NetShopMod;
import com.homestead.sellveg.client.FoodDetailFormat;
import com.homestead.sellveg.client.FoodDetailMath;
import com.homestead.sellveg.client.MallBrowse;
import com.homestead.sellveg.client.ShopBackground;
import com.homestead.sellveg.client.ShopClientState;
import com.homestead.sellveg.client.ShopToasts;
import com.homestead.sellveg.client.StarIcons;
import com.homestead.sellveg.currency.CurrencyHelper;
import com.homestead.sellveg.market.PriceCatalog;
import com.homestead.sellveg.order.Courier;
import com.homestead.sellveg.order.ShipmentSummary;
import com.homestead.sellveg.shop.GearOffers;
import com.homestead.sellveg.shop.GearPricing;
import com.homestead.sellveg.shop.GearTradeRules;
import com.homestead.sellveg.shop.ProductReviewPicker;
import com.homestead.sellveg.shop.ShopOffers;
import com.homestead.sellveg.shop.PresetShop;
import com.homestead.sellveg.shop.ShopGroup;
import com.homestead.sellveg.shop.PresetShops;
import com.homestead.sellveg.network.ModNetwork;
import com.homestead.sellveg.network.ShopActionPacket;
import com.homestead.sellveg.network.ShopSyncPacket;
import com.homestead.sellveg.stock.StockPacker;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class NetShopScreen extends Screen {
    private static final int GUI_W = 320;
    private static final int GUI_H = 232;
    /** Inner parchment of shop_bg.png (wood rails sit outside this). */
    private static final int INNER_L = 38;
    private static final int INNER_T = 34;
    private static final int INNER_R = 286;
    private static final int INNER_B = 198;
    private static final int RAIL_TOP = 9;
    private static final int TAB_H = 16;
    private static final int FOOTER_H = 16;
    private static final int BTN_W = 64;
    private static final int INK = 0xFF3F2A14;
    private static final int INK_MUTED = 0xFF78716C;
    private static final int MALL_COLS = 4;
    private static final int MALL_ROWS = 3;
    private static final int CELL_W = 56;
    private static final int CELL_H = 52;
    private static final int CELL_GAP = 4;
    private static final int STOCK_COLS = 10;
    private static final int STOCK_ROWS = 4;
    /** Listing stock icons (vanilla is 16). */
    private static final float STOCK_ICON_SCALE = 1.25F;
    private static final int STOCK_ICON_PX = Math.round(16 * STOCK_ICON_SCALE);
    private static final int STOCK_ROW_PITCH = 25;
    private static final ResourceLocation REFRESH_ICON =
            new ResourceLocation(NetShopMod.MOD_ID, "textures/gui/refresh.png");
    private static final int REFRESH_SIZE = 16;
    private int left;
    private int top;
    private int tab;
    private int mallScroll;
    private int shopScroll;
    private int stockScroll;
    private int cartScroll;
    private int walletScroll;
    private double walletDragAcc;
    private int logisticsScroll;
    private boolean dropPointDropdownOpen;
    private boolean cartView;
    private boolean walletView;
    private boolean ledgerView;
    private boolean transferView;
    private boolean transferConfirm;
    private boolean transferDropdownOpen;
    private boolean peerView;
    private java.util.UUID openedPeer = new java.util.UUID(0, 0);
    private java.util.UUID transferTarget = new java.util.UUID(0, 0);
    private int peerScroll;
    private int transferScroll;
    private String transferAmount = "";
    private EditBox transferBox;
    private boolean listView;
    private boolean shopListView;
    private boolean shopGroupView;
    private ShopGroup shopGroup;
    private String shopFilter = "";
    private int shopListScroll;
    private ShopSyncPacket.MallRow popup;
    private ItemStack listPick = ItemStack.EMPTY;
    private int listSuggestLo;
    private int listSuggestHi;
    private String savedCount = "1";
    private String savedPrice = "10";
    private String savedPopupCount = "1";
    private String searchQuery = "";
    private String stockSearchQuery = "";
    private EditBox countBox;
    private EditBox priceBox;
    private EditBox popupCountBox;
    private EditBox searchBox;
    private EditBox stockSearchBox;
    private ItemStack hoverTooltip = ItemStack.EMPTY;
    private long lastCoinLayout = Long.MIN_VALUE;
    /** Scroll offset (lines) for product-detail enchant panel. */
    private int gearEnchantScroll;
    private int enchantPanelX;
    private int enchantPanelY;
    private int enchantPanelW;
    private int enchantPanelH;
    /** Reused for food effect list scroll (mutually exclusive with gear enchants). */
    private static final ResourceLocation GUI_ICONS =
            new ResourceLocation("minecraft", "textures/gui/icons.png");
    /** Full hunger drumstick in {@code icons.png} (256×256 atlas). */
    private static final int HUNGER_ICON_U = 52;
    private static final int HUNGER_ICON_V = 27;
    private static final int HUNGER_ICON_SRC = 9;
    /** Drawn size for hunger / saturation icons on the food detail row. */
    private static final int FOOD_ICON_DRAW = 14;
    private static final int ICONS_TEX_SIZE = 256;
    /** Gap between star row bottom and the first detail panel. */
    private static final int DETAIL_BELOW_STARS = 6;
    private static final int STAR_ROW_OFFSET = 76;
    private int enchantLineCount;
    /** Cached mall filter result; invalidated when source / shop / search changes. */
    private List<ShopSyncPacket.MallRow> mallFilterCache;
    private List<ShopSyncPacket.MallRow> mallFilterSource;
    private String mallFilterShop = "";
    private String mallFilterQuery = "";

    public NetShopScreen() {
        super(Component.translatable("gui.homestead_sell_veg.title"));
    }

    public void refresh() {
        rememberFields();
        invalidateMallFilterCache();
        int maxShip = Math.max(0, ShopClientState.DATA.shipments.size() - visibleShipmentRows());
        logisticsScroll = Mth.clamp(logisticsScroll, 0, maxShip);
        rebuildWidgets();
    }

    private void invalidateMallFilterCache() {
        mallFilterCache = null;
        mallFilterSource = null;
    }

    private void rememberFields() {
        if (countBox != null) {
            savedCount = countBox.getValue();
        }
        if (priceBox != null) {
            savedPrice = priceBox.getValue();
        }
        if (popupCountBox != null) {
            savedPopupCount = popupCountBox.getValue();
        }
        if (searchBox != null) {
            searchQuery = searchBox.getValue();
        }
        if (stockSearchBox != null) {
            stockSearchQuery = stockSearchBox.getValue();
        }
    }

    @Override
    protected void init() {
        left = (width - GUI_W) / 2;
        top = (height - GUI_H) / 2;
        addTopRail();

        int listBarX = left + INNER_L;
        countBox = new EditBox(font, listBarX, listBarY(), 28, 16, Component.literal("count"));
        priceBox = new EditBox(font, listBarX + 86, listBarY(), 36, 16, Component.literal("price"));
        bindQtyBox(countBox, () -> savedCount = countBox.getValue(), this::listQtyCap);
        countBox.setValue(clampSavedListCount(savedCount));
        priceBox.setValue(savedPrice);
        priceBox.setFilter(s -> s.isEmpty() || s.chars().allMatch(Character::isDigit));

        int qtyY = detailActionY();
        int qtyBoxX = detailSplitX() + font.width(Component.translatable("gui.homestead_sell_veg.qty")) + 6;
        popupCountBox = new EditBox(font, qtyBoxX, qtyY, 28, FOOTER_H, Component.literal("buycount"));
        bindQtyBox(popupCountBox, () -> savedPopupCount = popupCountBox.getValue());
        popupCountBox.setValue(savedPopupCount);

        if (transferView) {
            addTransferWidgets();
        } else if (ledgerView) {
            addRenderableWidget(TextButton.wood(cartX(), footerY(), BTN_W, FOOTER_H,
                    Component.translatable("gui.homestead_sell_veg.wallet_back"), b -> {
                ledgerView = false;
                walletView = true;
                rebuildWidgets();
            }));
        } else if (walletView) {
            addWalletWidgets();
        } else if (peerView) {
            addPeerShopWidgets();
        } else if (tab == 0 && shopGroupView) {
            addShopGroupWidgets();
        } else if (tab == 0 && shopListView) {
            addShopDirectoryWidgets();
        } else if (tab == 0 && cartView) {
            addCartWidgets();
        } else if (tab == 0 && popup == null) {
            addRenderableWidget(TextButton.woodVertical(leftRailX(), contentTop() + 8, 16, 40,
                    Component.translatable("gui.homestead_sell_veg.shops"), b -> {
                shopGroupView = true;
                rebuildWidgets();
            }));
            addRenderableWidget(TextButton.wood(cartX(), footerY(), BTN_W, FOOTER_H,
                    Component.translatable("gui.homestead_sell_veg.cart"), b -> {
                cartView = true;
                rebuildWidgets();
            }));
            addRenderableWidget(TextButton.wood(checkoutX(), footerY(), BTN_W, FOOTER_H,
                    Component.translatable("gui.homestead_sell_veg.checkout"), b ->
                            ModNetwork.CHANNEL.sendToServer(ShopActionPacket.checkout())));
            addRenderableWidget(new IconButton(refreshX(), tabY(), REFRESH_SIZE, REFRESH_ICON, 12,
                    Component.translatable("gui.homestead_sell_veg.refresh"), b -> {
                ShopClientState.shuffleMall();
                invalidateMallFilterCache();
                mallScroll = 0;
            }));
        } else if (tab == 0) {
            addProductDetailWidgets();
        } else if (tab == 1 && listView) {
            addListPanelWidgets();
        } else if (tab == 1) {
            Component shopLine = Component.translatable("gui.homestead_sell_veg.shop_line",
                    ShopClientState.DATA.level, ShopClientState.DATA.listingUsed,
                    ShopClientState.DATA.listingCap);
            int listX = left + INNER_L + font.width(shopLine) + 8;
            addRenderableWidget(TextButton.ink(listX, contentTop(), 40, 12,
                    Component.translatable("gui.homestead_sell_veg.open_list"), b -> {
                listView = true;
                rebuildWidgets();
            }));
            int y = shopListingsTop();
            int skipped = shopScroll;
            for (ShopSyncPacket.ListingRow row : ShopClientState.DATA.listings) {
                if (row.stack().isEmpty()) {
                    continue;
                }
                if (skipped-- > 0) {
                    continue;
                }
                if (y + 16 > top + INNER_B - 4) {
                    break;
                }
                addCancelButton(row, y);
                y += 18;
            }
        } else if (tab == 2) {
            addLogisticsWidgets();
        }
        if (!walletView && !ledgerView && !transferView) {
            addFooterMoney();
        }
    }

    private void addLogisticsWidgets() {
        int pw = INNER_R - INNER_L - 8;
        int dropY = contentTop() + 16;
        Component dropLabel = dropPointDropdownLabel();
        addRenderableWidget(TextButton.ink(left + INNER_L + 4, dropY, pw, 16, dropLabel, b -> {
            dropPointDropdownOpen = !dropPointDropdownOpen;
            rebuildWidgets();
        }));
        if (!dropPointDropdownOpen) {
            return;
        }
        int y = dropY + 18;
        for (ShopSyncPacket.PointRow row : ShopClientState.DATA.points) {
            if (y + 16 > top + INNER_B - 8) {
                break;
            }
            String mark = row.isDefault() ? "* " : "  ";
            addRenderableWidget(TextButton.ink(left + INNER_L + 4, y, pw, 16,
                    Component.literal(mark + row.name()), b -> {
                ModNetwork.CHANNEL.sendToServer(ShopActionPacket.setDefault(row.id()));
                dropPointDropdownOpen = false;
                rebuildWidgets();
            }));
            y += 18;
        }
    }

    private Component dropPointDropdownLabel() {
        ShopSyncPacket data = ShopClientState.DATA;
        String name = data.defaultName == null || data.defaultName.isBlank() || "-".equals(data.defaultName)
                ? Component.translatable("gui.homestead_sell_veg.drop_point_unset").getString()
                : data.defaultName;
        String arrow = dropPointDropdownOpen ? " ▴" : " ▾";
        String full = Component.translatable("gui.homestead_sell_veg.drop_point_dropdown", name).getString() + arrow;
        int maxPx = INNER_R - INNER_L - 16;
        return Component.literal(ellipsizePx(full, maxPx));
    }

    private void addWalletWidgets() {
        int cardW = (INNER_R - INNER_L - 6) / 2;
        addRenderableWidget(RoundedCardButton.of(left + INNER_L, contentTop() + 62, cardW, 24,
                Component.translatable("gui.homestead_sell_veg.wallet_detail"),
                new ItemStack(net.minecraft.world.item.Items.BOOK), 0xFF8B5E34, b -> {
            ledgerView = true;
            walletView = false;
            walletScroll = 0;
            rebuildWidgets();
        }));
        addRenderableWidget(RoundedCardButton.of(left + INNER_L + cardW + 6, contentTop() + 62, cardW, 24,
                Component.translatable("gui.homestead_sell_veg.wallet_transfer"),
                new ItemStack(net.minecraft.world.item.Items.EMERALD), 0xFF2F9E6E, b -> {
            transferView = true;
            transferConfirm = false;
            walletView = false;
            transferScroll = 0;
            transferTarget = new java.util.UUID(0, 0);
            transferAmount = "";
            rebuildWidgets();
        }));
        addRenderableWidget(TextButton.wood(cartX(), footerY(), BTN_W, FOOTER_H,
                Component.translatable("gui.homestead_sell_veg.wallet_back"), b -> {
            walletView = false;
            rebuildWidgets();
        }));
    }

    private void addTransferWidgets() {
        addRenderableWidget(TextButton.wood(cartX(), footerY(), BTN_W, FOOTER_H,
                Component.translatable("gui.homestead_sell_veg.wallet_back"), b -> {
            transferView = false;
            transferConfirm = false;
            walletView = true;
            rebuildWidgets();
        }));
        if (transferConfirm) {
            addRenderableWidget(TextButton.ink(left + INNER_L, contentTop() + 40, 70, 16,
                    Component.translatable("gui.homestead_sell_veg.transfer_yes"), b -> {
                long amount = parseAmount(transferAmount);
                if (amount > 0L && !transferTarget.equals(new java.util.UUID(0, 0))) {
                    ModNetwork.CHANNEL.sendToServer(ShopActionPacket.transfer(transferTarget, amount));
                }
                transferConfirm = false;
                transferView = false;
                walletView = true;
                rebuildWidgets();
            }));
            addRenderableWidget(TextButton.ink(left + INNER_L + 78, contentTop() + 40, 70, 16,
                    Component.translatable("gui.homestead_sell_veg.transfer_no"), b -> {
                transferConfirm = false;
                rebuildWidgets();
            }));
            return;
        }
        transferBox = new EditBox(font, left + INNER_L + 146, contentTop() + 18, 60, 18,
                Component.literal("amount"));
        transferBox.setFilter(text -> text.isEmpty() || text.chars().allMatch(Character::isDigit));
        transferBox.setValue(transferAmount);
        transferBox.setResponder(text -> transferAmount = text);
        addRenderableWidget(transferBox);
        addRenderableWidget(new PlayerFaceButton(left + INNER_L, contentTop() + 18, 140, 18,
                pickLabel(), transferTarget, b -> {
            transferDropdownOpen = !transferDropdownOpen;
            rebuildWidgets();
        }));
        if (transferDropdownOpen) {
            int y = contentTop() + 38;
            int skipped = transferScroll;
            for (ShopSyncPacket.PeerShopRow row : ShopClientState.DATA.peerShops) {
                if (skipped-- > 0) {
                    continue;
                }
                if (y + 16 > top + INNER_B - 4) {
                    break;
                }
                java.util.UUID id = row.ownerId();
                addRenderableWidget(new PlayerFaceButton(left + INNER_L, y, 140, 16,
                        Component.literal(row.name()), id, b -> {
                    transferTarget = id;
                    transferDropdownOpen = false;
                    rebuildWidgets();
                }));
                y += 18;
            }
        }
        addRenderableWidget(TextButton.ink(left + INNER_L + 146, contentTop() + 40, 60, 16,
                Component.translatable("gui.homestead_sell_veg.transfer_next"), b -> {
            if (parseAmount(transferAmount) <= 0L || transferTarget.equals(new java.util.UUID(0, 0))) {
                return;
            }
            transferConfirm = true;
            rebuildWidgets();
        }));
    }

    private void addPeerShopWidgets() {
        addRenderableWidget(TextButton.wood(cartX(), footerY(), BTN_W, FOOTER_H,
                Component.translatable("gui.homestead_sell_veg.peer_back"), b -> {
            peerView = false;
            shopListView = true;
            ModNetwork.CHANNEL.sendToServer(
                    com.homestead.sellveg.network.RequestOpenShopPacket.openSelf());
            rebuildWidgets();
        }));
        int y = contentTop() + 20;
        int skipped = peerScroll;
        for (ShopSyncPacket.PeerListingRow row : ShopClientState.DATA.peerListings) {
            if (skipped-- > 0) {
                continue;
            }
            if (y + 18 > top + INNER_B - 4) {
                break;
            }
            java.util.UUID ownerId = row.ownerId();
            java.util.UUID listingId = row.listingId();
            addRenderableWidget(TextButton.ink(left + INNER_R - 58, y + 1, 58, 16,
                    Component.translatable("gui.homestead_sell_veg.peer_buy"), b ->
                    ModNetwork.CHANNEL.sendToServer(ShopActionPacket.buyPeer(ownerId, listingId))));
            y += 18;
        }
    }

    private Component pickLabel() {
        String name = transferTarget.equals(new java.util.UUID(0, 0))
                ? Component.translatable("gui.homestead_sell_veg.transfer_pick").getString()
                : peerName(transferTarget);
        return Component.literal(name + "\u25be");
    }

    private static long parseAmount(String text) {
        try {
            return Long.parseLong(text == null ? "" : text.trim());
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private void addFooterMoney() {
        Component coins = coinsLabel();
        addRenderableWidget(TextButton.wood(footerGroupX(), footerY(), coinsBtnW(), FOOTER_H, coins, b -> {
            walletView = true;
            ledgerView = false;
            transferView = false;
            transferConfirm = false;
            cartView = false;
            shopListView = false;
            popup = null;
            listView = false;
            rebuildWidgets();
        }));
    }

    private Component coinsLabel() {
        return Component.translatable("gui.homestead_sell_veg.coins",
                CurrencyHelper.formatCount(liveCoins()));
    }

    private int coinsBtnW() {
        return Math.max(BTN_W, font.width(coinsLabel()) + 10);
    }

    private void addProductDetailWidgets() {
        int split = detailSplitX();
        int qtyY = detailActionY();
        int qtyBoxX = split + font.width(Component.translatable("gui.homestead_sell_veg.qty")) + 6;
        int bumpX = qtyBoxX + 32;
        boolean gear = isGearStack(popup.stack());
        if (gear) {
            popupCountBox.setValue("1");
            popupCountBox.setEditable(false);
            savedPopupCount = "1";
        } else {
            popupCountBox.setEditable(true);
        }
        addRenderableWidget(popupCountBox);
        addRenderableWidget(TextButton.ink(split, contentTop() + 6, INNER_R - (split - left), 12,
                shopTitle(popup.shopId()), b -> {
            shopFilter = popup.shopId();
            popup = null;
            mallScroll = 0;
            invalidateMallFilterCache();
            rebuildWidgets();
        }));
        if (!gear) {
            addRenderableWidget(TextButton.ink(bumpX, qtyY, 22, FOOTER_H, Component.literal("+1"), b -> bumpPopupCount(1)));
            addRenderableWidget(TextButton.ink(bumpX + 24, qtyY, 28, FOOTER_H, Component.literal("+16"), b -> bumpPopupCount(16)));
        }
        int btnY = qtyY + FOOTER_H + 6;
        int btnW = 64;
        addRenderableWidget(TextButton.wood(split, btnY, btnW, FOOTER_H,
                Component.translatable("gui.homestead_sell_veg.buy_now"), b -> {
            ModNetwork.CHANNEL.sendToServer(ShopActionPacket.buyNow(popup.shopId(), popup.stack(), parseQty(popupCountBox.getValue())));
            popup = null;
            rebuildWidgets();
        }));
        addRenderableWidget(TextButton.wood(split + btnW + 4, btnY, btnW, FOOTER_H,
                Component.translatable("gui.homestead_sell_veg.add_cart"), b ->
                        ModNetwork.CHANNEL.sendToServer(ShopActionPacket.cartAdd(
                                popup.shopId(), popup.stack(), parseQty(popupCountBox.getValue())))));
        addRenderableWidget(TextButton.wood(split + btnW + 4, btnY + FOOTER_H + 4, btnW, FOOTER_H,
                Component.translatable("gui.cancel"), b -> {
            popup = null;
            rebuildWidgets();
        }));
    }

    private void bumpPopupCount(int delta) {
        if (popup != null && isGearStack(popup.stack())) {
            popupCountBox.setValue("1");
            savedPopupCount = "1";
            return;
        }
        popupCountBox.setValue(String.valueOf(StockPacker.bumpQty(popupCountBox.getValue(), delta, StockPacker.LIST_MAX)));
        savedPopupCount = popupCountBox.getValue();
    }

    private void bumpListCount(int delta) {
        if (isGearStack(listPick)) {
            countBox.setValue("1");
            savedCount = "1";
            return;
        }
        int cap = listQtyCap();
        if (cap < 1) {
            countBox.setValue("1");
        } else {
            countBox.setValue(String.valueOf(StockPacker.bumpQty(countBox.getValue(), delta, cap)));
        }
        savedCount = countBox.getValue();
    }

    private int listQtyCap() {
        if (listPick.isEmpty()) {
            return StockPacker.LIST_MAX;
        }
        return GearTradeRules.tradeQtyCap(isGearStack(listPick), stockAvailable(listPick));
    }

    private int stockAvailable(ItemStack pick) {
        if (pick == null || pick.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (ShopSyncPacket.StockRow row : ShopClientState.DATA.stock) {
            if (ItemStack.isSameItemSameTags(row.template(), pick)) {
                total += row.count();
            }
        }
        return total;
    }

    private String clampSavedListCount(String raw) {
        int cap = listQtyCap();
        if (cap < 1) {
            return "1";
        }
        String cleaned = StockPacker.sanitizeQtyTyping(raw == null ? "" : raw, cap);
        return cleaned.isEmpty() ? "1" : cleaned;
    }

    private void bindQtyBox(EditBox box, Runnable persist) {
        bindQtyBox(box, persist, () -> StockPacker.LIST_MAX);
    }

    private void bindQtyBox(EditBox box, Runnable persist, java.util.function.IntSupplier max) {
        box.setMaxLength(8);
        box.setFilter(s -> s.isEmpty() || s.chars().allMatch(Character::isDigit));
        box.setResponder(text -> {
            int cap = Math.max(0, max.getAsInt());
            String next = StockPacker.sanitizeQtyTyping(text, Math.max(1, cap));
            if (cap < 1 && !text.isEmpty()) {
                next = "1";
            }
            if (!next.equals(text)) {
                box.setValue(next);
                return;
            }
            persist.run();
        });
    }

    private int detailSplitX() {
        return left + INNER_L + 108;
    }

    /**
     * Qty / buy / cart / cancel block sits on the parchment bottom so gear
     * enchant panels cannot cover the hitboxes (panel stops above this Y).
     */
    private int detailActionY() {
        // qty row + gap + buy row + gap + cancel row
        int blockH = FOOTER_H + 6 + FOOTER_H + 4 + FOOTER_H;
        return top + INNER_B - blockH - 2;
    }

    private void addListPanelWidgets() {
        int x = left + INNER_L;
        boolean gear = isGearStack(listPick);
        if (gear) {
            countBox.setValue("1");
            countBox.setEditable(false);
            savedCount = "1";
        } else {
            countBox.setEditable(true);
        }
        addRenderableWidget(countBox);
        if (!gear) {
            addRenderableWidget(TextButton.ink(x + 30, listBarY() - 1, 22, 18, Component.literal("+1"), b -> bumpListCount(1)));
            addRenderableWidget(TextButton.ink(x + 54, listBarY() - 1, 28, 18, Component.literal("+16"), b -> bumpListCount(16)));
        }
        addRenderableWidget(priceBox);
        int listBtnX = x + 126;
        int listBtnW = 40;
        addRenderableWidget(TextButton.ink(listBtnX, listBarY() - 1, listBtnW, 18,
                Component.translatable("gui.homestead_sell_veg.list"), b -> {
            rememberFields();
            if (listPick.isEmpty()) {
                return;
            }
            ModNetwork.CHANNEL.sendToServer(ShopActionPacket.list(listPick, parseQty(countBox.getValue()), parse(priceBox.getValue(), 1)));
        }));
        int searchX = listBtnX + listBtnW + 4;
        int searchW = Math.max(48, left + INNER_R - searchX);
        stockSearchBox = new EditBox(font, searchX, listBarY(), searchW, 16,
                Component.translatable("gui.homestead_sell_veg.search"));
        stockSearchBox.setValue(stockSearchQuery);
        stockSearchBox.setHint(Component.translatable("gui.homestead_sell_veg.search"));
        stockSearchBox.setResponder(text -> {
            stockSearchQuery = text;
            stockScroll = 0;
        });
        addRenderableWidget(stockSearchBox);
        addRenderableWidget(TextButton.ink(left + INNER_R - 54, contentTop(), 54, 16,
                Component.translatable("gui.homestead_sell_veg.list_close"), b -> {
            listView = false;
            rebuildWidgets();
        }));
    }

    private List<ShopSyncPacket.StockRow> filteredStock() {
        List<ShopSyncPacket.StockRow> out = new ArrayList<>();
        String q = stockSearchQuery;
        for (ShopSyncPacket.StockRow row : ShopClientState.DATA.stock) {
            ItemStack stack = row.template().copy();
            stack.setCount(1);
            String name = stack.getHoverName().getString();
            String id = BuiltInRegistries.ITEM.getKey(row.item()).toString();
            if (MallBrowse.matchesQuery(q, name, id)) {
                out.add(row);
            }
        }
        return out;
    }

    private void addCartWidgets() {
        addRenderableWidget(TextButton.wood(cartX(), footerY(), BTN_W, FOOTER_H,
                Component.translatable("gui.homestead_sell_veg.cart_back"), b -> {
            cartView = false;
            rebuildWidgets();
        }));
        addRenderableWidget(TextButton.wood(checkoutX(), footerY(), BTN_W, FOOTER_H,
                Component.translatable("gui.homestead_sell_veg.checkout"), b ->
                        ModNetwork.CHANNEL.sendToServer(ShopActionPacket.checkout())));
        int y = cartRowsTop();
        int skipped = cartScroll;
        for (ShopSyncPacket.CartRow row : ShopClientState.DATA.cart) {
            if (skipped-- > 0) {
                continue;
            }
            if (y + 16 > cartRowsBottom()) {
                break;
            }
            int yy = y;
            ShopSyncPacket.CartRow captured = row;
            boolean gear = isGearStack(captured.stack());
            addRenderableWidget(TextButton.ink(left + INNER_R - 90, yy, 16, 16, Component.literal("-"), b ->
                    ModNetwork.CHANNEL.sendToServer(ShopActionPacket.cartSet(captured.shopId(), captured.stack(), captured.count() - 1))));
            if (!gear) {
                addRenderableWidget(TextButton.ink(left + INNER_R - 72, yy, 16, 16, Component.literal("+"), b ->
                        ModNetwork.CHANNEL.sendToServer(ShopActionPacket.cartSet(captured.shopId(), captured.stack(), captured.count() + 1))));
            }
            addRenderableWidget(TextButton.ink(left + INNER_R - 54, yy, 50, 16,
                    Component.translatable("gui.homestead_sell_veg.cart_remove"), b ->
                    ModNetwork.CHANNEL.sendToServer(ShopActionPacket.cartDel(captured.shopId(), captured.stack()))));
            y += 20;
        }
    }

    private void addShopGroupWidgets() {
        addRenderableWidget(TextButton.wood(cartX(), footerY(), BTN_W, FOOTER_H,
                Component.translatable("gui.homestead_sell_veg.cart_back"), b -> {
            shopGroupView = false;
            rebuildWidgets();
        }));
        int cols = 3;
        int gap = 4;
        int cellW = (INNER_R - INNER_L - gap * (cols - 1)) / cols;
        int cellH = 42;
        int startX = left + INNER_L;
        int startY = contentTop() + 18;
        ShopGroup[] groups = ShopGroup.values();
        for (int i = 0; i < groups.length; i++) {
            ShopGroup group = groups[i];
            addRenderableWidget(RoundedCardButton.tile(
                    startX + (i % cols) * (cellW + gap),
                    startY + (i / cols) * (cellH + gap),
                    cellW, cellH,
                    Component.translatable(group.langKey()),
                    com.homestead.sellveg.client.ShopIcons.ofGroup(group),
                    b -> openShopGroup(group)));
        }
        int i = groups.length;
        addRenderableWidget(RoundedCardButton.tile(
                startX + (i % cols) * (cellW + gap),
                startY + (i / cols) * (cellH + gap),
                cellW, cellH,
                Component.translatable("gui.homestead_sell_veg.peer_group"),
                new ItemStack(net.minecraft.world.item.Items.PLAYER_HEAD),
                b -> openShopGroup(null)));
    }

    private void openShopGroup(ShopGroup group) {
        shopGroup = group;
        shopGroupView = false;
        shopListView = true;
        shopListScroll = 0;
        rebuildWidgets();
    }

    private Component shopDirectoryTitle() {
        return shopGroup == null
                ? Component.translatable("gui.homestead_sell_veg.peer_group")
                : Component.translatable(shopGroup.langKey());
    }

    private void renderShopGroups(GuiGraphics graphics) {
        graphics.drawString(font, Component.translatable("gui.homestead_sell_veg.shops_title"),
                left + INNER_L + 2, contentTop() + 4, INK, false);
    }

    private void addShopDirectoryWidgets() {
        addRenderableWidget(TextButton.wood(cartX(), footerY(), BTN_W, FOOTER_H,
                Component.translatable("gui.homestead_sell_veg.cart_back"), b -> {
            shopListView = false;
            shopGroupView = true;
            rebuildWidgets();
        }));
        int y = contentTop() + 20;
        int skipped = shopListScroll;
        int pw = INNER_R - INNER_L;
        for (PresetShop shop : shopGroup == null ? List.<PresetShop>of() : PresetShops.inGroup(shopGroup)) {
            if (skipped-- > 0) {
                continue;
            }
            if (y + 16 > top + INNER_B - 4) {
                break;
            }
            String id = shop.id();
            int yy = y;
            addRenderableWidget(RoundedCardButton.of(left + INNER_L, yy, pw, 22, shopTitle(id),
                    com.homestead.sellveg.client.ShopIcons.ofShop(shop.id()), 0, b -> {
                shopFilter = id;
                shopListView = false;
                mallScroll = 0;
                invalidateMallFilterCache();
                rebuildWidgets();
            }));
            y += 24;
        }
        if (shopGroup != null) {
            return;
        }
        for (ShopSyncPacket.PeerShopRow row : ShopClientState.DATA.peerShops) {
            if (skipped-- > 0) {
                continue;
            }
            if (y + 20 > top + INNER_B - 4) {
                break;
            }
            java.util.UUID id = row.ownerId();
            Component label = Component.translatable("gui.homestead_sell_veg.peer_shop",
                    row.name(), row.listingCount());
            addRenderableWidget(new PlayerFaceButton(left + INNER_L, y, pw, 20, label, id, b -> {
                openedPeer = id;
                peerView = true;
                shopListView = false;
                peerScroll = 0;
                ModNetwork.CHANNEL.sendToServer(
                        com.homestead.sellveg.network.RequestOpenShopPacket.openPeer(id));
                rebuildWidgets();
            }));
            y += 24;
        }
    }

    private Component shopTitle(String shopId) {
        return Component.translatable("shop.homestead_sell_veg." + shopId);
    }

    private void addCancelButton(ShopSyncPacket.ListingRow row, int y) {
        addRenderableWidget(TextButton.ink(left + INNER_R - 58, y, 58, 16,
                Component.translatable("gui.homestead_sell_veg.cancel_listing"), b ->
                ModNetwork.CHANNEL.sendToServer(ShopActionPacket.cancel(row.id()))));
    }

    private void switchTab(int next) {
        tab = next;
        popup = null;
        cartView = false;
        walletView = false;
        ledgerView = false;
        transferView = false;
        transferConfirm = false;
        transferDropdownOpen = false;
        peerView = false;
        shopGroupView = false;
        shopGroup = null;
        listView = false;
        shopListView = false;
        dropPointDropdownOpen = false;
        logisticsScroll = 0;
        rebuildWidgets();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        hoverTooltip = ItemStack.EMPTY;
        renderBackground(graphics);
        graphics.blit(ShopBackground.current(), left, top, 0, 0, GUI_W, GUI_H, GUI_W, GUI_H);

        if (walletView) {
            renderWalletBoard(graphics);
        } else if (ledgerView) {
            renderLedger(graphics);
        } else if (transferView) {
            renderTransfer(graphics);
        } else if (peerView) {
            renderPeerShop(graphics, mouseX, mouseY);
        } else if (tab == 0 && shopGroupView) {
            renderShopGroups(graphics);
        } else if (tab == 0 && shopListView) {
            graphics.drawString(font, shopDirectoryTitle(),
                    left + INNER_L + 2, contentTop() + 2, INK, false);
        } else if (tab == 0 && cartView) {
            renderCart(graphics, mouseX, mouseY);
        } else if (tab == 0 && popup == null) {
            renderMall(graphics, mouseX, mouseY);
        } else if (tab == 0) {
            renderProductDetail(graphics);
        } else if (tab == 1 && listView) {
            renderListPanel(graphics, mouseX, mouseY);
        } else if (tab == 1) {
            renderShop(graphics, mouseX, mouseY);
        } else if (tab == 2) {
            renderLogistics(graphics);
        }
        if (tab == 0 && popup == null && !cartView && !shopListView && !shopGroupView && !shopFilter.isEmpty()) {
            drawVerticalLabel(graphics, shopTitle(shopFilter).getString(), shopNameRailX(),
                    contentTop(), top + INNER_B);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        if (!hoverTooltip.isEmpty()) {
            graphics.renderTooltip(font, hoverTooltip, mouseX, mouseY);
        }
        ShopToasts.render(graphics, font, left, top, GUI_W, GUI_H);
    }

    private void renderLogistics(GuiGraphics graphics) {
        ShopSyncPacket data = ShopClientState.DATA;
        graphics.drawString(font, Component.translatable("gui.homestead_sell_veg.logistics_summary",
                        data.shipmentCount,
                        Component.translatable(data.shipmentBlocked
                                ? "gui.homestead_sell_veg.logistics_blocked"
                                : "gui.homestead_sell_veg.logistics_ok")),
                left + INNER_L + 2, contentTop() + 2, INK, false);

        int textX = left + INNER_L;
        int titleY = contentTop() + 36;
        graphics.drawString(font, Component.translatable("gui.homestead_sell_veg.shipments_title"),
                textX + 2, titleY, INK, false);
        if (data.shipments.isEmpty()) {
            graphics.drawString(font, Component.translatable("gui.homestead_sell_veg.shipments_empty"),
                    textX + 2, titleY + 14, INK_MUTED, false);
            return;
        }
        if (dropPointDropdownOpen) {
            return;
        }
        int y = shipmentsListTop();
        int skipped = logisticsScroll;
        for (ShopSyncPacket.ShipmentRow row : data.shipments) {
            if (skipped-- > 0) {
                continue;
            }
            if (y + 16 > top + INNER_B - 4) {
                break;
            }
            if (!row.primary().isEmpty()) {
                graphics.renderItem(row.primary(), textX, y);
            }
            String goods = shipmentGoodsLabel(row);
            String courier = Component.translatable(Courier.fromId(row.courierId()).langKey()).getString();
            String status = shipmentStatusLabel(row);
            String line = goods + "  " + courier + "  " + status;
            graphics.drawString(font, ellipsizePx(line, INNER_R - INNER_L - 24),
                    textX + 20, y + 4, INK, false);
            y += 18;
        }
    }

    private int shipmentsListTop() {
        return contentTop() + 50;
    }

    private int visibleShipmentRows() {
        return Math.max(1, (top + INNER_B - 4 - shipmentsListTop()) / 18);
    }

    private String shipmentGoodsLabel(ShopSyncPacket.ShipmentRow row) {
        if (row.primary().isEmpty()) {
            return Component.translatable("gui.homestead_sell_veg.shipment_unknown").getString();
        }
        String name = row.primary().getHoverName().getString();
        if (row.kindCount() <= 1) {
            return Component.translatable("gui.homestead_sell_veg.shipment_one",
                    name, row.primary().getCount()).getString();
        }
        return Component.translatable("gui.homestead_sell_veg.shipment_many",
                name, row.primary().getCount(), row.kindCount()).getString();
    }

    private String shipmentStatusLabel(ShopSyncPacket.ShipmentRow row) {
        if (row.blocked()) {
            return Component.translatable("gui.homestead_sell_veg.shipment_status_blocked").getString();
        }
        int remain = ShopClientState.displayRemainingTicks(row);
        int minutes = ShipmentSummary.etaMinutes(remain);
        if (minutes <= 0) {
            return Component.translatable("gui.homestead_sell_veg.shipment_status_soon").getString();
        }
        return Component.translatable("gui.homestead_sell_veg.shipment_status_enroute", minutes).getString();
    }

    private void renderWalletBoard(GuiGraphics graphics) {
        int cardX = left + INNER_L;
        int cardY = contentTop() + 2;
        int cardW = INNER_R - INNER_L;
        RoundedCardButton.drawRounded(graphics, cardX, cardY, cardW, 54, 6, RoundedCardButton.BOARD_BG);
        graphics.pose().pushPose();
        graphics.pose().translate(cardX + 13, cardY + 11, 0);
        graphics.pose().scale(2.0F, 2.0F, 1.0F);
        graphics.renderItem(new ItemStack(net.minecraft.world.item.Items.GOLD_INGOT), 0, 0);
        graphics.pose().popPose();
        graphics.pose().pushPose();
        graphics.pose().translate(cardX + 52, cardY + 14, 0);
        graphics.pose().scale(2.0F, 2.0F, 1.0F);
        graphics.drawString(font, CurrencyHelper.formatCount(ShopClientState.DATA.coins), 0, 0, INK, false);
        graphics.pose().popPose();
        graphics.drawString(font, Component.translatable("gui.homestead_sell_veg.wallet_balance_label"),
                cardX + 52, cardY + 38, INK, false);
    }

    private void renderTransfer(GuiGraphics graphics) {
        ShopSyncPacket data = ShopClientState.DATA;
        graphics.drawString(font, Component.translatable("gui.homestead_sell_veg.wallet_transfer"),
                left + INNER_L + 2, contentTop() + 2, INK, false);
        if (transferConfirm) {
            graphics.drawString(font, Component.translatable("gui.homestead_sell_veg.transfer_confirm_line",
                            peerName(transferTarget), parseAmount(transferAmount)),
                    left + INNER_L + 2, contentTop() + 22, INK, false);
            return;
        }
        if (data.peerShops.isEmpty()) {
            graphics.drawString(font, Component.translatable("gui.homestead_sell_veg.transfer_no_peers"),
                    left + INNER_L + 2, contentTop() + 22, INK_MUTED, false);
        }
    }

    private void renderPeerShop(GuiGraphics graphics, int mouseX, int mouseY) {
        ShopSyncPacket data = ShopClientState.DATA;
        graphics.drawString(font, Component.translatable("gui.homestead_sell_veg.peer_title",
                        peerName(openedPeer)),
                left + INNER_L + 2, contentTop() + 2, INK, false);
        if (data.peerListings.isEmpty()) {
            graphics.drawString(font, Component.translatable("gui.homestead_sell_veg.peer_empty"),
                    left + INNER_L + 2, contentTop() + 22, INK_MUTED, false);
            return;
        }
        int y = contentTop() + 20;
        int skipped = peerScroll;
        for (ShopSyncPacket.PeerListingRow row : data.peerListings) {
            if (skipped-- > 0) {
                continue;
            }
            if (y + 18 > top + INNER_B - 4) {
                break;
            }
            ItemStack stack = row.stack().copy();
            graphics.renderItem(stack, left + INNER_L, y);
            long total = (long) row.unitPrice() * stack.getCount();
            String line = stack.getHoverName().getString() + " x" + stack.getCount()
                    + "  " + CurrencyHelper.formatCount(total);
            graphics.drawString(font, ellipsizePx(line, INNER_R - INNER_L - 80),
                    left + INNER_L + 20, y + 4, INK, false);
            if (mouseX >= left + INNER_L && mouseX < left + INNER_L + 16
                    && mouseY >= y && mouseY < y + 16) {
                hoverTooltip = stack;
            }
            y += 18;
        }
    }

    private String peerName(java.util.UUID ownerId) {
        for (ShopSyncPacket.PeerShopRow row : ShopClientState.DATA.peerShops) {
            if (row.ownerId().equals(ownerId)) {
                return row.name();
            }
        }
        return "?";
    }

    private void renderLedger(GuiGraphics graphics) {
        ShopSyncPacket data = ShopClientState.DATA;
        graphics.drawString(font, Component.translatable("gui.homestead_sell_veg.wallet_title",
                        CurrencyHelper.formatCount(data.coins)),
                left + INNER_L + 2, contentTop() + 2, INK, false);
        if (data.ledger.isEmpty()) {
            graphics.drawString(font, Component.translatable("gui.homestead_sell_veg.wallet_empty"),
                    left + INNER_L + 2, contentTop() + 22, INK_MUTED, false);
            return;
        }
        int y = cartRowsTop();
        int skipped = walletScroll;
        for (ShopSyncPacket.LedgerRow row : data.ledger) {
            if (skipped-- > 0) {
                continue;
            }
            if (y + 16 > cartRowsBottom()) {
                break;
            }
            int color = row.income() ? 0xFF166534 : 0xFF9F1239;
            String signAmt = (row.income() ? "+" : "-") + CurrencyHelper.formatCount(row.amount());
            String line = formatLedgerTime(row.timeMillis()) + "  " + signAmt + "  " + row.summary();
            graphics.drawString(font, ellipsizePx(line, INNER_R - INNER_L - 8),
                    left + INNER_L + 2, y + 4, color, false);
            y += 16;
        }
    }

    private String formatLedgerTime(long millis) {
        try {
            return DateTimeFormatter.ofPattern("MM-dd HH:mm")
                    .format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()));
        } catch (Exception ignored) {
            return "";
        }
    }

    private int visibleWalletRows() {
        return Math.max(1, (cartRowsBottom() - cartRowsTop()) / 16);
    }

    private void renderCart(GuiGraphics graphics, int mouseX, int mouseY) {
        ShopSyncPacket data = ShopClientState.DATA;
        graphics.drawString(font, Component.translatable("gui.homestead_sell_veg.cart_total", data.cartTotal),
                left + INNER_L + 2, contentTop() + 2, INK, false);
        if (data.cart.isEmpty()) {
            graphics.drawString(font, Component.translatable("gui.homestead_sell_veg.cart_empty_hint"),
                    left + INNER_L + 2, contentTop() + 22, INK_MUTED, false);
            return;
        }
        int y = cartRowsTop();
        int skipped = cartScroll;
        for (ShopSyncPacket.CartRow row : data.cart) {
            if (skipped-- > 0) {
                continue;
            }
            if (y + 16 > cartRowsBottom()) {
                break;
            }
            ItemStack stack = row.stack().copy();
            stack.setCount(1);
            graphics.renderItem(stack, left + INNER_L, y);
            if (mouseX >= left + INNER_L && mouseX < left + INNER_L + 16
                    && mouseY >= y && mouseY < y + 16) {
                hoverTooltip = stack;
            }
            String gearBit = gearRowSuffix(stack);
            graphics.drawString(font, ellipsizePx(shopTitle(row.shopId()).getString() + " · "
                            + stack.getHoverName().getString() + gearBit
                            + " x" + row.count() + " @" + row.unitPrice(), INNER_R - INNER_L - 110),
                    left + INNER_L + 20, y + 4, INK, false);
            y += 20;
        }
    }

    private void renderProductDetail(GuiGraphics graphics) {
        int bodyTop = contentTop() + 6;
        int midX = left + INNER_L + 48;
        int split = left + INNER_L + 108;
        ItemStack stack = popup.stack().copy();
        stack.setCount(1);
        String name = ellipsizePx(stack.getHoverName().getString(), 90);
        int iconY = bodyTop;
        graphics.pose().pushPose();
        graphics.pose().translate(midX - 24, iconY, 0);
        graphics.pose().scale(3.0F, 3.0F, 1.0F);
        graphics.renderItem(stack, 0, 0);
        graphics.pose().popPose();
        graphics.drawString(font, name, midX - font.width(name) / 2, iconY + 52, INK, false);
        drawPricePair(graphics, midX, iconY + 64, popup.baseBuyPrice(), popup.buyPrice(), popup.sale());
        if (popup.sale()) {
            drawSalePercentBadge(graphics, midX + 40, iconY, popup.salePercent());
        }
        if (popup.hot()) {
            drawHotBadge(graphics, midX - 40, iconY);
        }
        ProductReviewPicker.Pick reviews = ProductReviewPicker.pick(
                popup.shopId(), ShopOffers.itemKey(popup.item()), PriceCatalog.shelfKind(popup.item()));
        StarIcons.blit(graphics, midX - 40, iconY + STAR_ROW_OFFSET, reviews.rating());

        if (isGearStack(stack)) {
            int leftX = midX - 42;
            int leftW = 92;
            int gy = detailPanelTop(iconY);
            int panelBottom = top + INNER_B - 4;
            String dur = durabilityLabel(stack);
            int durBoxH = 18;
            drawInfoPanel(graphics, leftX, gy, leftW, durBoxH);
            String durText = dur == null
                    ? Component.translatable("gui.homestead_sell_veg.durability", "—").getString()
                    : Component.translatable("gui.homestead_sell_veg.durability", dur).getString();
            graphics.drawString(font, ellipsizePx(durText, leftW - 8),
                    leftX + 4, gy + (durBoxH - 8) / 2, INK, false);
            gy += durBoxH + 4;

            int enchH = Math.max(28, panelBottom - gy);
            drawInfoPanel(graphics, leftX, gy, leftW, enchH);
            graphics.drawString(font, Component.translatable("gui.homestead_sell_veg.enchants"),
                    leftX + 4, gy + 3, INK, false);
            enchantPanelX = leftX;
            enchantPanelY = gy;
            enchantPanelW = leftW;
            enchantPanelH = enchH;

            int clipTop = gy + 14;
            int clipBottom = gy + enchH - 3;
            int contentH = Math.max(0, clipBottom - clipTop);
            int lineH = 10;
            int visible = Math.max(1, contentH / lineH);
            List<Component> lines = gearEnchantLines(stack);
            enchantLineCount = lines.size();
            int maxScroll = Math.max(0, enchantLineCount - visible);
            gearEnchantScroll = Mth.clamp(gearEnchantScroll, 0, maxScroll);

            try {
                graphics.enableScissor(leftX + 3, clipTop, leftX + leftW - 3, clipBottom);
                int ly = clipTop - gearEnchantScroll * lineH;
                for (int i = 0; i < lines.size(); i++) {
                    Component line = lines.get(i);
                    if (ly + lineH > clipTop && ly < clipBottom) {
                        int color = enchantLineColor(line, i == 0 && !stack.isEnchanted());
                        graphics.drawString(font, ellipsizePx(line.getString(), leftW - 10),
                                leftX + 4, ly, color, false);
                    }
                    ly += lineH;
                }
            } finally {
                graphics.disableScissor();
            }
            if (maxScroll > 0) {
                drawEnchantScrollHint(graphics, leftX + leftW - 6, clipTop, contentH, gearEnchantScroll, maxScroll);
            }
        } else if (FoodDetailFormat.isFood(stack)) {
            renderFoodDetailPanels(graphics, stack, midX, iconY);
        } else {
            enchantLineCount = 0;
        }

        int textW = INNER_R - 108 - 4;
        int y = bodyTop + 16;
        graphics.drawString(font, Component.translatable("gui.homestead_sell_veg.reviews"), split, y, INK, false);
        y += 12;
        for (ProductReviewPicker.LinePick line : List.of(reviews.line0(), reviews.line1())) {
            if (y + 20 > detailActionY()) {
                break;
            }
            String nick = Component.translatable(line.nickKey()).getString() + "  " + line.stars();
            graphics.drawString(font, ellipsizePx(nick, textW), split, y, INK_MUTED, false);
            String body = Component.translatable(line.textKey()).getString();
            graphics.drawString(font, ellipsizePx(body, textW), split, y + 10, INK, false);
            y += 24;
        }
        graphics.drawString(font, Component.translatable("gui.homestead_sell_veg.qty"), split, detailActionY() + 4, INK, false);
    }

    private void addTopRail() {
        int tabW = 62;
        int gap = 2;
        int x = left + 8;
        int y = tabY();
        addRenderableWidget(TextButton.wood(x, y, tabW, TAB_H,
                Component.translatable("gui.homestead_sell_veg.tab.mall"), b -> {
            shopFilter = "";
            invalidateMallFilterCache();
            switchTab(0);
        }));
        x += tabW + gap;
        addRenderableWidget(TextButton.wood(x, y, tabW, TAB_H,
                Component.translatable("gui.homestead_sell_veg.tab.shop"), b -> switchTab(1)));
        x += tabW + gap;
        addRenderableWidget(TextButton.wood(x, y, tabW, TAB_H,
                Component.translatable("gui.homestead_sell_veg.tab.logistics"), b -> switchTab(2)));
        x += tabW + gap;
        int searchEnd = left + GUI_W - 10;
        if (tab == 0 && popup == null && !cartView && !shopListView && !shopGroupView && !walletView
                && !ledgerView && !transferView && !peerView) {
            searchEnd -= REFRESH_SIZE + 4;
        }
        int searchW = searchEnd - x;
        searchBox = new EditBox(font, x, y, Math.max(48, searchW), TAB_H,
                Component.translatable("gui.homestead_sell_veg.search"));
        searchBox.setValue(searchQuery);
        searchBox.setHint(Component.translatable("gui.homestead_sell_veg.search"));
        searchBox.setResponder(text -> {
            searchQuery = text;
            mallScroll = 0;
            invalidateMallFilterCache();
        });
        if (!walletView && !ledgerView && !transferView && !peerView && !shopGroupView && !(tab == 1 && listView)) {
            addRenderableWidget(searchBox);
        }
    }

    private int tabY() {
        return top + RAIL_TOP;
    }

    private int refreshX() {
        return left + GUI_W - 10 - REFRESH_SIZE;
    }

    private int contentTop() {
        return top + INNER_T;
    }

    private int footerY() {
        return top + INNER_B + 6;
    }

    private int footerGroupX() {
        return left + INNER_L;
    }

    private int leftRailX() {
        return left + 10;
    }

    /** Left parchment gutter inside the frame, clear of the wheat decoration. */
    private int shopNameRailX() {
        return left + INNER_L - 2;
    }

    private void drawVerticalLabel(GuiGraphics graphics, String text, int x, int topY, int bottomY) {
        if (text == null || text.isEmpty()) {
            return;
        }
        int lineH = 10;
        int colW = 12;
        int total = text.length() * lineH;
        int y = topY + Math.max(0, (bottomY - topY - total) / 2);
        int padX = 3;
        int padY = 3;
        graphics.fill(x - padX, y - padY, x + colW + padX, y + total + padY, 0xA8EFE6D8);
        for (int i = 0; i < text.length(); i++) {
            String ch = text.substring(i, i + 1);
            graphics.drawString(font, ch, x + Math.max(0, (colW - font.width(ch)) / 2), y + i * lineH, INK, false);
        }
    }

    private int checkoutX() {
        return cartX() + BTN_W + 4;
    }

    private int cartX() {
        return footerGroupX() + coinsBtnW() + 4;
    }

    private long liveCoins() {
        return ShopClientState.DATA.coins;
    }

    @Override
    public void tick() {
        super.tick();
        long coins = liveCoins();
        if (coins != lastCoinLayout) {
            lastCoinLayout = coins;
            rememberFields();
            rebuildWidgets();
        }
    }

    private int mallGridTop() {
        return contentTop() + 4;
    }

    private int mallCellX(int index) {
        int gridW = MALL_COLS * CELL_W + (MALL_COLS - 1) * CELL_GAP;
        int start = left + INNER_L + Math.max(0, (INNER_R - INNER_L - gridW) / 2);
        return start + (index % MALL_COLS) * (CELL_W + CELL_GAP);
    }

    private int mallCellY(int index) {
        return mallGridTop() + (index / MALL_COLS) * (CELL_H + CELL_GAP);
    }

    private int cartRowsTop() {
        return contentTop() + 18;
    }

    private int cartRowsBottom() {
        return top + INNER_B - 4;
    }

    private int visibleCartRows() {
        return Math.max(1, (cartRowsBottom() - cartRowsTop()) / 20);
    }

    private List<ShopSyncPacket.MallRow> filteredMall() {
        List<ShopSyncPacket.MallRow> source = ShopClientState.DATA.mall;
        String q = searchQuery;
        if (mallFilterCache != null
                && mallFilterSource == source
                && mallFilterShop.equals(shopFilter)
                && mallFilterQuery.equals(q)) {
            return mallFilterCache;
        }
        List<ShopSyncPacket.MallRow> out = MallBrowse.filterByShopAndQuery(
                source,
                shopFilter,
                q,
                ShopSyncPacket.MallRow::shopId,
                row -> row.stack().getHoverName().getString(),
                row -> BuiltInRegistries.ITEM.getKey(row.item()).toString());
        mallFilterCache = out;
        mallFilterSource = source;
        mallFilterShop = shopFilter;
        mallFilterQuery = q;
        return out;
    }

    private void renderMall(GuiGraphics graphics, int mouseX, int mouseY) {
        var mall = filteredMall();
        int start = mallScroll * MALL_COLS;
        int visible = MALL_COLS * MALL_ROWS;
        for (int i = 0; i < visible && start + i < mall.size(); i++) {
            ShopSyncPacket.MallRow row = mall.get(start + i);
            int cx = mallCellX(i);
            int cy = mallCellY(i);
            boolean hover = mouseX >= cx && mouseX < cx + CELL_W && mouseY >= cy && mouseY < cy + CELL_H;
            if (hover) {
                int edge = 0xAA8B5A2B;
                graphics.fill(cx, cy, cx + CELL_W, cy + 1, edge);
                graphics.fill(cx, cy + CELL_H - 1, cx + CELL_W, cy + CELL_H, edge);
                graphics.fill(cx, cy, cx + 1, cy + CELL_H, edge);
                graphics.fill(cx + CELL_W - 1, cy, cx + CELL_W, cy + CELL_H, edge);
            }
            ItemStack stack = row.stack();
            float scale = 2.0F;
            int iconPx = Math.round(16 * scale);
            int iconX = cx + (CELL_W - iconPx) / 2;
            int iconY = cy + 2;
            graphics.pose().pushPose();
            graphics.pose().translate(iconX, iconY, 0);
            graphics.pose().scale(scale, scale, 1.0F);
            graphics.renderItem(stack, 0, 0);
            graphics.pose().popPose();
            if (row.hot()) {
                drawHotBadge(graphics, cx + 2, cy + 1);
            }
            if (row.sale()) {
                drawSalePercentBadge(graphics, cx + CELL_W - 2, cy + 1, row.salePercent());
            }
            if (hover) {
                hoverTooltip = stack;
            }
            String label = ellipsizePx(stack.getHoverName().getString(), CELL_W - 4);
            graphics.drawString(font, label, cx + (CELL_W - font.width(label)) / 2, cy + 36, INK, false);
            // Mall shelf: no durability %; enchanted gear only shows ✦ beside the price.
            drawPricePair(graphics, cx + CELL_W / 2, cy + 45, row.baseBuyPrice(), row.buyPrice(), row.sale(),
                    isGearStack(stack) && stack.isEnchanted());
        }
    }

    private void drawPricePair(GuiGraphics graphics, int centerX, int y, int basePrice, int buyPrice, boolean onSale) {
        drawPricePair(graphics, centerX, y, basePrice, buyPrice, onSale, false);
    }

    private void drawPricePair(GuiGraphics graphics, int centerX, int y, int basePrice, int buyPrice, boolean onSale,
                               boolean enchantedMark) {
        int priceRight;
        if (!onSale || basePrice <= buyPrice) {
            String price = String.valueOf(buyPrice);
            int px = centerX - font.width(price) / 2;
            graphics.drawString(font, price, px, y, 0xFF166534, false);
            priceRight = px + font.width(price);
        } else {
            Component original = Component.literal(String.valueOf(basePrice)).withStyle(ChatFormatting.STRIKETHROUGH);
            int ow = font.width(original);
            String sale = String.valueOf(buyPrice);
            int sw = font.width(sale);
            int gap = 3;
            int total = ow + gap + sw;
            if (total > CELL_W - 4) {
                original = Component.literal(ellipsizePx(String.valueOf(basePrice), Math.max(12, CELL_W / 2 - 4)))
                        .withStyle(ChatFormatting.STRIKETHROUGH);
                ow = font.width(original);
                total = ow + gap + sw;
            }
            int x = centerX - total / 2;
            graphics.drawString(font, original, x, y, 0xFFA8A29E, false);
            graphics.drawString(font, sale, x + ow + gap, y, 0xFF166534, false);
            priceRight = x + total;
        }
        if (enchantedMark) {
            graphics.drawString(font, "✦", priceRight + 2, y, 0xFF7C3AED, false);
        }
    }

    /** Right-aligned green polygon badge with {@code -xx%} text. {@code right} is the right edge. */
    private void drawSalePercentBadge(GuiGraphics graphics, int right, int top, int percentOff) {
        String text = "-" + Math.max(10, Math.min(99, percentOff)) + "%";
        float scale = 0.7F;
        int tw = Math.round(font.width(text) * scale);
        int padX = 2;
        int h = 8;
        int w = tw + padX * 2;
        int x = right - w;
        int y = top;
        int cut = 2;
        int fill = 0xE016A34A;
        int edge = 0xFF4ADE80;
        graphics.fill(x + cut, y, x + w - cut, y + h, fill);
        graphics.fill(x, y + 1, x + w, y + h - 1, fill);
        graphics.fill(x + cut, y, x + w - cut, y + 1, edge);
        graphics.fill(x + cut, y + h - 1, x + w - cut, y + h, edge);
        graphics.fill(x, y + 1, x + 1, y + h - 1, edge);
        graphics.fill(x + w - 1, y + 1, x + w, y + h - 1, edge);
        graphics.pose().pushPose();
        graphics.pose().translate(x + padX, y + 1, 0);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, text, 0, 0, 0xFFF0FDF4, false);
        graphics.pose().popPose();
    }

    private void drawHotBadge(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 7, y + 8, 0xE0EA580C);
        graphics.drawString(font, "H", x + 1, y, 0xFFFFF7ED, false);
    }

    private int shopListingsTop() {
        return contentTop() + 24;
    }

    private void drawUpgradeBar(GuiGraphics graphics, ShopSyncPacket data, int x, int y) {
        Component label;
        float fill;
        if (data.upgradeGoal < 0) {
            fill = 1.0F;
            label = Component.translatable("gui.homestead_sell_veg.shop_max");
        } else {
            long span = Math.max(1L, data.upgradeGoal - data.upgradePrev);
            fill = Math.min(1.0F, Math.max(0L, data.soldTotal - data.upgradePrev) / (float) span);
            label = Component.translatable("gui.homestead_sell_veg.shop_xp",
                    CurrencyHelper.formatCount(data.soldTotal),
                    CurrencyHelper.formatCount(data.upgradeGoal));
        }
        int gap = 4;
        int labelW = font.width(label);
        int w = Math.max(24, INNER_R - INNER_L - 4 - labelW - gap);
        int h = 6;
        graphics.fill(x, y, x + w, y + h, 0xFF2A1C10);
        int filled = Math.max(0, Math.round(w * fill));
        if (filled > 0) {
            graphics.fill(x, y, x + filled, y + h, 0xFF4ADE80);
        }
        graphics.fill(x, y, x + w, y + 1, 0xFF5C4030);
        graphics.fill(x, y + h - 1, x + w, y + h, 0xFF5C4030);
        graphics.fill(x, y, x + 1, y + h, 0xFF5C4030);
        graphics.fill(x + w - 1, y, x + w, y + h, 0xFF5C4030);
        graphics.drawString(font, label, x + w + gap, y - 1, INK, false);
    }

    private int listBarY() {
        return contentTop() + 38;
    }

    private int stockCellW() {
        return Math.max(STOCK_ICON_PX, (INNER_R - INNER_L) / STOCK_COLS);
    }

    private int stockGridTop() {
        return listBarY() + 18;
    }

    private int stockCellX(int indexInPage) {
        return left + INNER_L + (indexInPage % STOCK_COLS) * stockCellW();
    }

    private int stockCellY(int indexInPage) {
        return stockGridTop() + (indexInPage / STOCK_COLS) * STOCK_ROW_PITCH;
    }

    private void renderListPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = left + INNER_L;
        Component listTitle = Component.translatable("gui.homestead_sell_veg.list_panel");
        graphics.drawString(font, listTitle, x, contentTop() + 2, INK, false);
        graphics.drawString(font, Component.translatable("gui.homestead_sell_veg.pick_item"), x, contentTop() + 18, INK_MUTED, false);
        if (ShopClientState.DATA.unloadedSources > 0) {
            graphics.drawString(font, Component.translatable("gui.homestead_sell_veg.stock_unloaded",
                    ShopClientState.DATA.unloadedSources), x + font.width(listTitle) + 8, contentTop() + 2, 0xFFB45309, false);
        }
        graphics.drawString(font, Component.translatable("gui.homestead_sell_veg.qty"), x, listBarY() - 11, INK, false);
        graphics.drawString(font, Component.translatable("gui.homestead_sell_veg.unit_price"), x + 86, listBarY() - 11, INK, false);
        if (!listPick.isEmpty() && listSuggestHi > 0) {
            Component hint = Component.translatable(
                    "gui.homestead_sell_veg.suggest_band", listSuggestLo, listSuggestHi);
            graphics.drawString(font, hint, x + 126, listBarY() - 11, INK_MUTED, false);
        }
        if (!listPick.isEmpty()) {
            graphics.renderItem(listPick, left + INNER_R - 70, listBarY() - 14);
            graphics.drawString(font, ellipsizePx(listPick.getHoverName().getString(), 48),
                    left + INNER_R - 52, listBarY() - 10, INK, false);
        }
        List<ShopSyncPacket.StockRow> stock = filteredStock();
        int start = stockScroll * STOCK_COLS;
        int cellW = stockCellW();
        int iconPad = Math.max(0, (cellW - STOCK_ICON_PX) / 2);
        for (int i = 0; i < STOCK_COLS * STOCK_ROWS && start + i < stock.size(); i++) {
            ShopSyncPacket.StockRow row = stock.get(start + i);
            int sx = stockCellX(i);
            int sy = stockCellY(i);
            int iconX = sx + iconPad;
            ItemStack stack = row.template().copy();
            stack.setCount(1);
            graphics.pose().pushPose();
            graphics.pose().translate(iconX, sy, 0);
            graphics.pose().scale(STOCK_ICON_SCALE, STOCK_ICON_SCALE, 1.0F);
            graphics.renderItem(stack, 0, 0);
            graphics.pose().popPose();
            String countText = CurrencyHelper.formatCount(row.count());
            int metaY = sy + STOCK_ICON_PX;
            graphics.drawString(font, countText, sx, metaY, INK_MUTED, false);
            if (isGearStack(stack)) {
                // Compact durability % + enchant mark on the same row, right of count.
                drawGearCellDecor(graphics, stack, sx + font.width(countText) + 1, metaY, 0.55F);
            }
            if (mouseX >= iconX && mouseX < iconX + STOCK_ICON_PX
                    && mouseY >= sy && mouseY < sy + STOCK_ICON_PX) {
                hoverTooltip = stack;
            }
            if (ItemStack.isSameItemSameTags(listPick, stack) && !listPick.isEmpty()) {
                graphics.fill(iconX, sy, iconX + STOCK_ICON_PX, sy + STOCK_ICON_PX, 0x6600AA88);
            }
        }
    }

    private void renderShop(GuiGraphics graphics, int mouseX, int mouseY) {
        ShopSyncPacket data = ShopClientState.DATA;
        int textX = left + INNER_L;
        graphics.drawString(font, Component.translatable("gui.homestead_sell_veg.shop_line",
                        data.level, data.listingUsed, data.listingCap),
                textX, contentTop() + 2, INK, false);
        drawUpgradeBar(graphics, data, textX, contentTop() + 14);
        int y = shopListingsTop();
        int skipped = shopScroll;
        for (ShopSyncPacket.ListingRow row : data.listings) {
            if (row.stack().isEmpty()) {
                continue;
            }
            if (skipped-- > 0) {
                continue;
            }
            if (y + 16 > top + INNER_B - 4) {
                break;
            }
            ItemStack stack = row.stack().copy();
            graphics.renderItem(stack, textX, y);
            if (mouseX >= textX && mouseX < textX + 16 && mouseY >= y && mouseY < y + 16) {
                hoverTooltip = stack;
            }
            String hot = row.hot() ? " HOT" : "";
            String wait = row.remainingTicks() < 0
                    ? " " + Component.translatable("gui.homestead_sell_veg.listing_unsaleable").getString()
                    : "";
            String gearBit = gearRowSuffix(stack);
            graphics.drawString(font, ellipsizePx(stack.getHoverName().getString() + gearBit
                    + " x" + stack.getCount()
                    + " @" + row.unitPrice() + hot + wait, INNER_R - INNER_L - 80), textX + 20, y + 4, INK, false);
            y += 18;
        }
    }

    private static boolean isGearStack(ItemStack stack) {
        return stack != null && !stack.isEmpty() && GearOffers.isGearItem(stack.getItem());
    }

    private void renderFoodDetailPanels(GuiGraphics graphics, ItemStack stack, int midX, int iconY) {
        FoodProperties food = FoodDetailFormat.foodOf(stack);
        if (food == null) {
            enchantLineCount = 0;
            return;
        }
        int leftX = midX - 42;
        int leftW = 92;
        int gy = detailPanelTop(iconY);
        int panelBottom = top + INNER_B - 4;
        int nutriH = 20;
        drawInfoPanel(graphics, leftX, gy, leftW, nutriH);
        drawFoodNutritionRow(graphics, food, leftX + 4, gy + (nutriH - FOOD_ICON_DRAW) / 2, leftW - 8);
        gy += nutriH + 4;

        int effectH = Math.max(28, panelBottom - gy);
        drawInfoPanel(graphics, leftX, gy, leftW, effectH);
        graphics.drawString(font, Component.translatable("gui.homestead_sell_veg.use_effects"),
                leftX + 4, gy + 3, INK, false);
        enchantPanelX = leftX;
        enchantPanelY = gy;
        enchantPanelW = leftW;
        enchantPanelH = effectH;

        int clipTop = gy + 14;
        int clipBottom = gy + effectH - 3;
        int contentH = Math.max(0, clipBottom - clipTop);
        int lineH = 10;
        int visible = Math.max(1, contentH / lineH);
        List<Component> lines = FoodDetailFormat.effectLines(food);
        enchantLineCount = lines.size();
        int maxScroll = Math.max(0, enchantLineCount - visible);
        gearEnchantScroll = Mth.clamp(gearEnchantScroll, 0, maxScroll);
        boolean mutedEmpty = food.getEffects().isEmpty();

        try {
            graphics.enableScissor(leftX + 3, clipTop, leftX + leftW - 3, clipBottom);
            int ly = clipTop - gearEnchantScroll * lineH;
            for (int i = 0; i < lines.size(); i++) {
                Component line = lines.get(i);
                if (ly + lineH > clipTop && ly < clipBottom) {
                    int color = enchantLineColor(line, mutedEmpty && i == 0);
                    graphics.drawString(font, ellipsizePx(line.getString(), leftW - 10),
                            leftX + 4, ly, color, false);
                }
                ly += lineH;
            }
        } finally {
            graphics.disableScissor();
        }
        if (maxScroll > 0) {
            drawEnchantScrollHint(graphics, leftX + leftW - 6, clipTop, contentH, gearEnchantScroll, maxScroll);
        }
    }

    private static int detailPanelTop(int iconY) {
        return iconY + STAR_ROW_OFFSET + StarIcons.height() + DETAIL_BELOW_STARS;
    }

    private void drawFoodNutritionRow(GuiGraphics graphics, FoodProperties food, int x, int y, int maxW) {
        int hunger = food.getNutrition();
        String hungerQty = FoodDetailMath.formatHungerQty(hunger);
        String satQty = FoodDetailMath.formatSaturationQty(FoodDetailFormat.saturationRestored(food));
        int textY = y + Math.max(0, (FOOD_ICON_DRAW - 8) / 2);
        int cursor = x;
        blitHungerIcon(graphics, cursor, y);
        cursor += FOOD_ICON_DRAW + 2;
        graphics.drawString(font, hungerQty, cursor, textY, INK, false);
        cursor += font.width(hungerQty) + 6;
        if (cursor + FOOD_ICON_DRAW + 2 + font.width(satQty) > x + maxW) {
            // Narrow panel: keep sat on the same conceptual row by ellipsizing qty if needed.
            int remain = Math.max(0, x + maxW - cursor - FOOD_ICON_DRAW - 2);
            satQty = ellipsizePx(satQty, remain);
        }
        blitSaturationIcon(graphics, cursor, y);
        cursor += FOOD_ICON_DRAW + 2;
        graphics.drawString(font, satQty, cursor, textY, INK, false);
    }

    private void blitHungerIcon(GuiGraphics graphics, int x, int y) {
        graphics.blit(GUI_ICONS, x, y, FOOD_ICON_DRAW, FOOD_ICON_DRAW,
                (float) HUNGER_ICON_U, (float) HUNGER_ICON_V,
                HUNGER_ICON_SRC, HUNGER_ICON_SRC, ICONS_TEX_SIZE, ICONS_TEX_SIZE);
    }

    /** Gold-tinted drumstick so saturation is visually distinct from hunger. */
    private void blitSaturationIcon(GuiGraphics graphics, int x, int y) {
        graphics.setColor(1.0F, 0.82F, 0.15F, 1.0F);
        blitHungerIcon(graphics, x, y);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static String durabilityLabel(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.isDamageableItem()) {
            return null;
        }
        return GearTradeRules.durabilityPercentLabel(stack.getDamageValue(), stack.getMaxDamage());
    }

    private List<Component> gearEnchantLines(ItemStack stack) {
        List<Component> lines = new ArrayList<>();
        if (!stack.isEnchanted()) {
            lines.add(Component.translatable("gui.homestead_sell_veg.not_enchanted"));
            return lines;
        }
        Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(stack);
        for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
            lines.add(entry.getKey().getFullname(entry.getValue()));
        }
        return lines;
    }

    private static int enchantLineColor(Component line, boolean muted) {
        if (muted) {
            return INK_MUTED;
        }
        if (line.getStyle().getColor() != null) {
            return 0xFF000000 | line.getStyle().getColor().getValue();
        }
        return 0xFF7F7FE0;
    }

    private void drawInfoPanel(GuiGraphics graphics, int x, int y, int w, int h) {
        int fill = 0xE8F5E6D5;
        int edge = 0xFF8B5A2B;
        graphics.fill(x, y, x + w, y + h, fill);
        graphics.fill(x, y, x + w, y + 1, edge);
        graphics.fill(x, y + h - 1, x + w, y + h, edge);
        graphics.fill(x, y, x + 1, y + h, edge);
        graphics.fill(x + w - 1, y, x + w, y + h, edge);
    }

    private void drawEnchantScrollHint(GuiGraphics graphics, int barX, int contentTop, int contentH,
                                       int scroll, int maxScroll) {
        if (contentH < 8 || maxScroll < 1) {
            return;
        }
        int track = contentH;
        int thumbH = Math.max(6, track * track / (track + maxScroll * 10));
        int travel = Math.max(1, track - thumbH);
        int thumbY = contentTop + (int) Math.round(travel * (scroll / (double) maxScroll));
        graphics.fill(barX, contentTop, barX + 2, contentTop + track, 0x668B5A2B);
        graphics.fill(barX, thumbY, barX + 2, thumbY + thumbH, 0xFF8B5A2B);
    }

    private boolean mouseOverEnchantPanel(double mouseX, double mouseY) {
        return enchantLineCount > 0
                && mouseX >= enchantPanelX && mouseX < enchantPanelX + enchantPanelW
                && mouseY >= enchantPanelY && mouseY < enchantPanelY + enchantPanelH;
    }

    /**
     * Compact gear meta: durability % then enchant mark, left-to-right from {@code x,y}.
     * {@code scale} &lt; 1 shrinks the font (listing uses ~0.55 so it sits beside count).
     */
    private void drawGearCellDecor(GuiGraphics graphics, ItemStack stack, int x, int y, float scale) {
        if (!isGearStack(stack)) {
            return;
        }
        String dur = durabilityLabel(stack);
        boolean ench = stack.isEnchanted();
        if (dur == null && !ench) {
            return;
        }
        float s = scale > 0.0F && Float.isFinite(scale) ? scale : 1.0F;
        int baselineNudge = Math.max(0, Math.round((1.0F - s) * 3.5F));
        graphics.pose().pushPose();
        graphics.pose().translate(x, y + baselineNudge, 0);
        graphics.pose().scale(s, s, 1.0F);
        int ox = 0;
        if (dur != null) {
            graphics.drawString(font, dur, ox, 0, INK_MUTED, false);
            ox += font.width(dur) + 1;
        }
        if (ench) {
            graphics.drawString(font, "✦", ox, 0, 0xFF7C3AED, false);
        }
        graphics.pose().popPose();
    }

    private String gearRowSuffix(ItemStack stack) {
        if (!isGearStack(stack)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        String dur = durabilityLabel(stack);
        if (dur != null) {
            sb.append(' ').append(dur);
        }
        if (stack.isEnchanted()) {
            sb.append(" ✦");
        }
        return sb.toString();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (ledgerView) {
            if (button == 0 && mouseX >= left + INNER_L && mouseX < left + INNER_R
                    && mouseY >= cartRowsTop() && mouseY < cartRowsBottom()) {
                walletDragAcc = 0;
                setDragging(true);
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (walletView || transferView || peerView) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (tab == 0 && popup == null && !cartView && !shopListView && !shopGroupView) {
            var mall = filteredMall();
            int start = mallScroll * MALL_COLS;
            int visible = MALL_COLS * MALL_ROWS;
            for (int i = 0; i < visible && start + i < mall.size(); i++) {
                int mx = mallCellX(i);
                int my = mallCellY(i);
                if (mouseX >= mx && mouseX < mx + CELL_W && mouseY >= my && mouseY < my + CELL_H) {
                    popup = mall.get(start + i);
                    gearEnchantScroll = 0;
                    rebuildWidgets();
                    return true;
                }
            }
        }
        if (tab == 1 && listView) {
            List<ShopSyncPacket.StockRow> stock = filteredStock();
            int start = stockScroll * STOCK_COLS;
            int cellW = stockCellW();
            int iconPad = Math.max(0, (cellW - STOCK_ICON_PX) / 2);
            for (int i = 0; i < STOCK_COLS * STOCK_ROWS && start + i < stock.size(); i++) {
                int sx = stockCellX(i) + iconPad;
                int sy = stockCellY(i);
                if (mouseX >= sx && mouseX < sx + STOCK_ICON_PX && mouseY >= sy && mouseY < sy + STOCK_ICON_PX) {
                    ShopSyncPacket.StockRow row = stock.get(start + i);
                    listPick = row.template().copy();
                    listPick.setCount(1);
                    suggestPrice(row);
                    if (countBox != null) {
                        if (isGearStack(listPick)) {
                            countBox.setValue("1");
                            countBox.setEditable(false);
                        } else {
                            countBox.setEditable(true);
                            countBox.setValue(clampSavedListCount(countBox.getValue()));
                        }
                        savedCount = countBox.getValue();
                    }
                    rebuildWidgets();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void suggestPrice(ShopSyncPacket.StockRow row) {
        ItemStack stack = row.template().copy();
        stack.setCount(1);
        boolean gear = isGearStack(stack);
        GearPricing.ListingSuggest suggest = GearPricing.listingSuggest(
                row.bandLo(),
                row.bandHi(),
                row.refPrice(),
                gear ? GearOffers.pricingLines(stack) : List.of(),
                gear ? GearOffers.durabilityRatio(stack) : 1.0D,
                gear);
        // TODO(SELLVEG-DIAG): temporary probe for the listing panel enchant band; remove after diagnosis.
        org.slf4j.LoggerFactory.getLogger("homestead_sellveg.diag").info(
                "[SELLVEG-DIAG] suggest {} ench={} tags={} lines={} mult={} dur={} in={}-{} ref={} out={}-{} fill={}",
                BuiltInRegistries.ITEM.getKey(stack.getItem()),
                stack.isEnchanted(),
                stack.getEnchantmentTags().size(),
                gear ? GearOffers.pricingLines(stack).size() : -1,
                gear ? GearPricing.enchantMultiplier(GearOffers.pricingLines(stack)) : 1.0D,
                gear ? GearOffers.durabilityRatio(stack) : 1.0D,
                row.bandLo(),
                row.bandHi(),
                row.refPrice(),
                suggest.lo(),
                suggest.hi(),
                suggest.fill());
        listSuggestLo = suggest.lo();
        listSuggestHi = suggest.hi();
        // rebuildWidgets() → init() restores from savedPrice; must update both.
        savedPrice = String.valueOf(suggest.fill());
        if (priceBox != null) {
            priceBox.setValue(savedPrice);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (tab == 0 && popup != null && mouseOverEnchantPanel(mouseX, mouseY)) {
            int step = (int) Math.signum(delta);
            if (step != 0) {
                int lineH = 10;
                int contentH = Math.max(0, enchantPanelH - 17);
                int visible = Math.max(1, contentH / lineH);
                int maxScroll = Math.max(0, enchantLineCount - visible);
                gearEnchantScroll = Mth.clamp(gearEnchantScroll - step, 0, maxScroll);
                return true;
            }
        }
        if (applyListScroll(delta)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (ledgerView && button == 0 && isDragging()) {
            walletDragAcc += dragY;
            while (walletDragAcc >= 16) {
                walletDragAcc -= 16;
                applyListScroll(1);
            }
            while (walletDragAcc <= -16) {
                walletDragAcc += 16;
                applyListScroll(-1);
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private boolean applyListScroll(double delta) {
        int step = (int) Math.signum(delta);
        if (step == 0) {
            return false;
        }
        if (ledgerView) {
            int max = Math.max(0, ShopClientState.DATA.ledger.size() - visibleWalletRows());
            walletScroll = Mth.clamp(walletScroll - step, 0, max);
            return true;
        }
        if (transferView) {
            int max = Math.max(0, ShopClientState.DATA.peerShops.size() - 6);
            transferScroll = Mth.clamp(transferScroll - step, 0, max);
            rebuildWidgets();
            return true;
        }
        if (peerView) {
            int max = Math.max(0, ShopClientState.DATA.peerListings.size() - 8);
            peerScroll = Mth.clamp(peerScroll - step, 0, max);
            rebuildWidgets();
            return true;
        }
        if (tab == 0 && shopListView) {
            int rows = shopGroup == null
                    ? ShopClientState.DATA.peerShops.size()
                    : PresetShops.inGroup(shopGroup).size();
            int max = Math.max(0, rows - 5);
            shopListScroll = Mth.clamp(shopListScroll - step, 0, max);
            rebuildWidgets();
            return true;
        }
        if (tab == 0 && cartView) {
            int max = Math.max(0, ShopClientState.DATA.cart.size() - visibleCartRows());
            cartScroll = Mth.clamp(cartScroll - step, 0, max);
            rebuildWidgets();
            return true;
        }
        if (tab == 0 && popup == null && !shopListView) {
            int max = Math.max(0, (filteredMall().size() + MALL_COLS - 1) / MALL_COLS - MALL_ROWS);
            mallScroll = Mth.clamp(mallScroll - step, 0, max);
            return true;
        }
        if (tab == 1 && listView) {
            int max = Math.max(0, (filteredStock().size() + STOCK_COLS - 1) / STOCK_COLS - STOCK_ROWS);
            stockScroll = Mth.clamp(stockScroll - step, 0, max);
            return true;
        }
        if (tab == 1 && !listView) {
            shopScroll = Math.max(0, shopScroll - step);
            rebuildWidgets();
            return true;
        }
        if (tab == 2 && !dropPointDropdownOpen) {
            int max = Math.max(0, ShopClientState.DATA.shipments.size() - visibleShipmentRows());
            logisticsScroll = Mth.clamp(logisticsScroll - step, 0, max);
            return true;
        }
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private String ellipsizePx(String text, int maxPx) {
        if (font.width(text) <= maxPx) {
            return text;
        }
        String ellipsis = "…";
        int budget = Math.max(1, maxPx - font.width(ellipsis));
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (font.width(out.toString() + c) > budget) {
                break;
            }
            out.append(c);
        }
        return out + ellipsis;
    }

    private static int parseQty(String text) {
        String cleaned = StockPacker.sanitizeQtyTyping(text, StockPacker.LIST_MAX);
        if (cleaned.isEmpty()) {
            return 1;
        }
        return Integer.parseInt(cleaned);
    }

    private static int parse(String text, int fallback) {
        try {
            return Math.max(1, Integer.parseInt(text.trim()));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
