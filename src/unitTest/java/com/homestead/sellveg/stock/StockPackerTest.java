package com.homestead.sellveg.stock;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StockPackerTest {
    @Test
    void split15000IntoTwoSlots() {
        assertEquals(List.of(10000, 5000), StockPacker.split(15000, 10000));
    }

    @Test
    void splitExactCapIsOneSlot() {
        assertEquals(List.of(10000), StockPacker.split(10000, 10000));
    }

    @Test
    void splitZeroIsEmpty() {
        assertEquals(List.of(), StockPacker.split(0, 10000));
    }

    @Test
    void clampListCountCapsAt64() {
        assertEquals(64, StockPacker.clampListCount(200, 64));
        assertEquals(1, StockPacker.clampListCount(1, 64));
        assertEquals(0, StockPacker.clampListCount(0, 64));
    }

    @Test
    void sanitizeQtyTypingClampsOverflowAndRejectsJunk() {
        assertEquals("64", StockPacker.sanitizeQtyTyping("1000", 64));
        assertEquals("64", StockPacker.sanitizeQtyTyping("65", 64));
        assertEquals("12", StockPacker.sanitizeQtyTyping("12", 64));
        assertEquals("", StockPacker.sanitizeQtyTyping("你好", 64));
        assertEquals("", StockPacker.sanitizeQtyTyping("abc", 64));
        assertEquals("", StockPacker.sanitizeQtyTyping("", 64));
        assertEquals("64", StockPacker.sanitizeQtyTyping("99999999999999999999", 64));
    }

    @Test
    void bumpQtyAddsAndCaps() {
        assertEquals(1, StockPacker.bumpQty("", 1, 64));
        assertEquals(17, StockPacker.bumpQty("1", 16, 64));
        assertEquals(64, StockPacker.bumpQty("60", 16, 64));
    }

    @Test
    void listQtyCapIsMinOfStockAnd64() {
        assertEquals(0, StockPacker.listQtyCap(0));
        assertEquals(32, StockPacker.listQtyCap(32));
        assertEquals(64, StockPacker.listQtyCap(64));
        assertEquals(64, StockPacker.listQtyCap(200));
    }

    @Test
    void bumpQtyStopsAtStockWhenBelow64() {
        assertEquals(32, StockPacker.bumpQty("16", 16, StockPacker.listQtyCap(32)));
        assertEquals(32, StockPacker.bumpQty("32", 16, StockPacker.listQtyCap(32)));
    }

    @Test
    void takeWaterfallPlayerThenChests() {
        int[] pools = {10, 100};
        int remaining = StockPacker.takeWaterfall(pools, 64);
        assertEquals(0, remaining);
        assertArrayEquals(new int[]{0, 46}, pools);
    }
}
