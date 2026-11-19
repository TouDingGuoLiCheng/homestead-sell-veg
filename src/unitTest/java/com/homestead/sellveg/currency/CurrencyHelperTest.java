package com.homestead.sellveg.currency;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CurrencyHelperTest {
    @Test
    void formatCountUsesRawBelowThousand() {
        assertEquals("1", CurrencyHelper.formatCount(1));
        assertEquals("999", CurrencyHelper.formatCount(999));
    }

    @Test
    void formatCountUsesKAboveThousand() {
        assertEquals("1k", CurrencyHelper.formatCount(1000));
        assertEquals("1.5k", CurrencyHelper.formatCount(1500));
        assertEquals("100k", CurrencyHelper.formatCount(100_000));
        assertEquals("999.9k", CurrencyHelper.formatCount(999_900));
    }

    @Test
    void formatCountUsesMFromMillion() {
        assertEquals("1M", CurrencyHelper.formatCount(1_000_000));
        assertEquals("1.5M", CurrencyHelper.formatCount(1_500_000));
        assertEquals("100M", CurrencyHelper.formatCount(100_000_000));
    }

    @Test
    void formatCountUsesBFromBillion() {
        assertEquals("1B", CurrencyHelper.formatCount(1_000_000_000L));
        assertEquals("1.5B", CurrencyHelper.formatCount(1_500_000_000L));
        assertEquals("2B", CurrencyHelper.formatCount(2_000_000_000L));
    }
}
