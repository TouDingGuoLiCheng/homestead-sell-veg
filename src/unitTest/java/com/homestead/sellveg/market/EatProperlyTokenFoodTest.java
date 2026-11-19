package com.homestead.sellveg.market;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EatProperlyTokenFoodTest {
    @Test
    void blocksEdibleEatProperlyItems() {
        assertTrue(EatProperlyTradeFilter.isTokenFood("touhou_maid_eat_properly", true));
    }

    @Test
    void allowsVanillaFoodAndNonEdibleTokens() {
        assertFalse(EatProperlyTradeFilter.isTokenFood("minecraft", true));
        assertFalse(EatProperlyTradeFilter.isTokenFood("touhou_maid_eat_properly", false));
    }
}
