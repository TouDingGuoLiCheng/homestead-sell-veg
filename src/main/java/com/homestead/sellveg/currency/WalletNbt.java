package com.homestead.sellveg.currency;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

public final class WalletNbt {
    private WalletNbt() {
    }

    public static CompoundTag save(WalletAccount wallet) {
        CompoundTag tag = new CompoundTag();
        tag.putLong("Balance", wallet.balance());
        ListTag list = new ListTag();
        for (LedgerEntry entry : wallet.ledger()) {
            CompoundTag row = new CompoundTag();
            row.putLong("T", entry.timeMillis());
            row.putBoolean("In", entry.income());
            row.putLong("Amt", entry.amount());
            row.putString("S", entry.summary());
            list.add(row);
        }
        tag.put("Ledger", list);
        return tag;
    }

    public static void load(WalletAccount wallet, CompoundTag tag) {
        wallet.setBalance(tag.getLong("Balance"));
        List<LedgerEntry> rows = new ArrayList<>();
        ListTag list = tag.getList("Ledger", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size() && rows.size() < WalletAccount.MAX_LEDGER; i++) {
            CompoundTag row = list.getCompound(i);
            rows.add(new LedgerEntry(
                    row.getLong("T"),
                    row.getBoolean("In"),
                    Math.max(0L, row.getLong("Amt")),
                    row.getString("S")
            ));
        }
        wallet.replaceLedger(rows);
    }
}
