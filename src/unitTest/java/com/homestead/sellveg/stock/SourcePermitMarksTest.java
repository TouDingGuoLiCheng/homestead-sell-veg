package com.homestead.sellveg.stock;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SourcePermitMarksTest {
    @Test
    void toggleAddsAndRemovesWithoutCap() {
        List<SourcePermitMark> marks = new ArrayList<>();
        SourcePermitMark a = new SourcePermitMark("minecraft:overworld", 1, 2, 3);
        assertEquals(SourcePermitMarks.ToggleResult.ADDED, SourcePermitMarks.toggle(marks, a));
        assertEquals(1, marks.size());
        assertEquals(SourcePermitMarks.ToggleResult.REMOVED, SourcePermitMarks.toggle(marks, a));
        assertEquals(0, marks.size());
    }

    @Test
    void toggleAllowsMoreThanEight() {
        List<SourcePermitMark> marks = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            assertEquals(SourcePermitMarks.ToggleResult.ADDED,
                    SourcePermitMarks.toggle(marks, new SourcePermitMark("minecraft:overworld", i, 0, 0)));
        }
        assertEquals(9, marks.size());
    }
}
