package com.homestead.sellveg.stock;

import java.util.List;

public final class SourcePermitMarks {
    public enum ToggleResult {
        ADDED,
        REMOVED
    }

    private SourcePermitMarks() {
    }

    public static ToggleResult toggle(List<SourcePermitMark> marks, SourcePermitMark mark) {
        int index = marks.indexOf(mark);
        if (index >= 0) {
            marks.remove(index);
            return ToggleResult.REMOVED;
        }
        marks.add(mark);
        return ToggleResult.ADDED;
    }

    public static void clear(List<SourcePermitMark> marks) {
        marks.clear();
    }
}
