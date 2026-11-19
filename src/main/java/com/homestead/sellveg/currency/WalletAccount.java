package com.homestead.sellveg.currency;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class WalletAccount {
    public static final int MAX_LEDGER = 50;

    private long balance;
    private final List<LedgerEntry> ledger = new ArrayList<>();

    public long balance() {
        return balance;
    }

    public List<LedgerEntry> ledger() {
        return Collections.unmodifiableList(ledger);
    }

    public void give(long amount) {
        if (amount <= 0) {
            return;
        }
        balance += amount;
    }

    public boolean take(long amount) {
        if (amount <= 0) {
            return true;
        }
        if (balance < amount) {
            return false;
        }
        balance -= amount;
        return true;
    }

    public void record(boolean income, long amount, String summary, long timeMillis) {
        if (amount <= 0) {
            return;
        }
        String text = summary == null ? "" : summary;
        if (text.length() > 64) {
            text = text.substring(0, 64);
        }
        ledger.add(0, new LedgerEntry(timeMillis, income, amount, text));
        while (ledger.size() > MAX_LEDGER) {
            ledger.remove(ledger.size() - 1);
        }
    }

    public long applySet(long next, String summary, long timeMillis) {
        long target = Math.max(0L, next);
        long delta = target - balance;
        balance = target;
        if (delta > 0) {
            record(true, delta, summary, timeMillis);
        } else if (delta < 0) {
            record(false, -delta, summary, timeMillis);
        }
        return balance;
    }

    void setBalance(long balance) {
        this.balance = Math.max(0L, balance);
    }

    void replaceLedger(List<LedgerEntry> entries) {
        ledger.clear();
        int n = Math.min(MAX_LEDGER, entries.size());
        for (int i = 0; i < n; i++) {
            ledger.add(entries.get(i));
        }
    }
}
