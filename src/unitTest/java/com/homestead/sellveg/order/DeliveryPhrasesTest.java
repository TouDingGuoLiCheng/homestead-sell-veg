package com.homestead.sellveg.order;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeliveryPhrasesTest {
    @Test
    void sameShipmentKeepsTheSameLine() {
        UUID id = UUID.fromString("11111111-2222-3333-4444-555555555555");
        assertEquals(DeliveryPhrases.index(id), DeliveryPhrases.index(id));
    }

    @Test
    void poolUsesBothLines() {
        boolean zero = false;
        boolean one = false;
        for (long i = 0; i < 64; i++) {
            int idx = DeliveryPhrases.index(new UUID(0L, i));
            if (idx == 0) {
                zero = true;
            }
            if (idx == 1) {
                one = true;
            }
        }
        assertTrue(zero && one);
    }

    @Test
    void indexStaysInsideThePool() {
        int i = DeliveryPhrases.index(UUID.randomUUID());
        assertTrue(i >= 0 && i < DeliveryPhrases.COUNT);
    }
}
