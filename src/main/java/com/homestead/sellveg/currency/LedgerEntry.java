package com.homestead.sellveg.currency;

public record LedgerEntry(long timeMillis, boolean income, long amount, String summary) {
}
