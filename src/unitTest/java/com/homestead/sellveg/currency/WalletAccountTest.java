package com.homestead.sellveg.currency;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WalletAccountTest {
    @Test
    void takeFailsWhenBalanceTooLow() {
        WalletAccount wallet = new WalletAccount();
        wallet.give(10);
        assertFalse(wallet.take(11));
        assertEquals(10, wallet.balance());
    }

    @Test
    void takeAndGiveUpdateBalance() {
        WalletAccount wallet = new WalletAccount();
        wallet.give(100);
        assertTrue(wallet.take(40));
        assertEquals(60, wallet.balance());
    }

    @Test
    void ledgerKeepsNewestFirstAndCaps() {
        WalletAccount wallet = new WalletAccount();
        for (int i = 0; i < WalletAccount.MAX_LEDGER + 5; i++) {
            wallet.record(true, i + 1, "in" + i, i);
        }
        assertEquals(WalletAccount.MAX_LEDGER, wallet.ledger().size());
        assertEquals("in" + (WalletAccount.MAX_LEDGER + 4), wallet.ledger().get(0).summary());
        assertEquals("in5", wallet.ledger().get(WalletAccount.MAX_LEDGER - 1).summary());
    }

    @Test
    void applySetClampsAndRecordsDelta() {
        WalletAccount wallet = new WalletAccount();
        wallet.give(40);
        assertEquals(100, wallet.applySet(100, "cmd", 1L));
        assertEquals(100, wallet.balance());
        assertEquals(true, wallet.ledger().get(0).income());
        assertEquals(60, wallet.ledger().get(0).amount());
        assertEquals(0, wallet.applySet(-5, "cmd", 2L));
        assertEquals(0, wallet.balance());
        assertEquals(false, wallet.ledger().get(0).income());
        assertEquals(100, wallet.ledger().get(0).amount());
    }
}
