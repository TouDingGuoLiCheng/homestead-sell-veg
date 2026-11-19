package com.homestead.sellveg.shop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuickListHintsTest {
    @Test
    void noScrollMeansNoHoverHint() {
        QuickListHints.Hover hover = QuickListHints.hover(false, false, false, true, 7);
        assertEquals(QuickListHints.Kind.NONE, hover.kind());
    }

    @Test
    void hoveringTheScrollItselfShowsNoPrice() {
        QuickListHints.Hover hover = QuickListHints.hover(true, true, false, true, 7);
        assertEquals(QuickListHints.Kind.NONE, hover.kind());
    }

    @Test
    void emptySlotShowsNoPrice() {
        QuickListHints.Hover hover = QuickListHints.hover(true, false, true, true, 7);
        assertEquals(QuickListHints.Kind.NONE, hover.kind());
    }

    @Test
    void tradeableWithSyncedMidShowsPrice() {
        QuickListHints.Hover hover = QuickListHints.hover(true, false, false, true, 7);
        assertEquals(QuickListHints.Kind.PRICE, hover.kind());
        assertEquals(7, hover.price());
    }

    @Test
    void tradeableBeforeSyncShowsPending() {
        QuickListHints.Hover hover = QuickListHints.hover(true, false, false, true, 0);
        assertEquals(QuickListHints.Kind.PENDING, hover.kind());
    }

    @Test
    void syncedMidShowsPriceEvenIfClientCatalogMisses() {
        QuickListHints.Hover hover = QuickListHints.hover(true, false, false, false, 200);
        assertEquals(QuickListHints.Kind.PRICE, hover.kind());
        assertEquals(200, hover.price());
    }

    @Test
    void untradeableWithoutSyncShowsNothing() {
        QuickListHints.Hover hover = QuickListHints.hover(true, false, false, false, 0);
        assertEquals(QuickListHints.Kind.NONE, hover.kind());
    }

    @Test
    void interceptsFilledSlotWhenScrollReady() {
        assertTrue(QuickListHints.shouldInterceptClick(true, false, false));
        assertFalse(QuickListHints.shouldInterceptClick(false, false, false));
        assertFalse(QuickListHints.shouldInterceptClick(true, true, false));
        assertFalse(QuickListHints.shouldInterceptClick(true, false, true));
    }
}
