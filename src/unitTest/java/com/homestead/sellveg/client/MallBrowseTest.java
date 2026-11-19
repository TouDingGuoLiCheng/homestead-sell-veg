package com.homestead.sellveg.client;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MallBrowseTest {
    @Test
    void queryMatchesItemNameNotShopTitle() {
        assertTrue(MallBrowse.matchesQuery("面包", "面包", "minecraft:bread"));
        assertFalse(MallBrowse.matchesQuery("面包", "苹果", "minecraft:apple"));
    }

    @Test
    void queryMatchesItemId() {
        assertTrue(MallBrowse.matchesQuery("bread", "Bread", "minecraft:bread"));
    }

    @Test
    void saleRowsComeFirstKeepingRelativeOrder() {
        record Row(String id, boolean sale) {
        }
        List<Row> rows = new ArrayList<>(List.of(
                new Row("a", false),
                new Row("b", true),
                new Row("c", false),
                new Row("d", true)
        ));
        MallBrowse.saleFirst(rows, Row::sale);
        assertEquals(List.of("b", "d", "a", "c"), rows.stream().map(Row::id).toList());
    }

    @Test
    void saleThenHotArePromoted() {
        record Row(String id, boolean sale, boolean hot) {
        }
        List<Row> rows = new ArrayList<>(List.of(
                new Row("plain", false, false),
                new Row("hotOnly", false, true),
                new Row("saleOnly", true, false),
                new Row("both", true, true)
        ));
        MallBrowse.featureFirst(rows, Row::sale, Row::hot);
        assertEquals(List.of("both", "saleOnly", "hotOnly", "plain"), rows.stream().map(Row::id).toList());
    }

    @Test
    void filterByShopAndQueryKeepsSourceOrder() {
        record Row(String shopId, String name, String itemId) {
        }
        List<Row> source = List.of(
                new Row("weapon", "铁剑", "minecraft:iron_sword"),
                new Row("food", "面包", "minecraft:bread"),
                new Row("weapon", "钻石剑", "minecraft:diamond_sword"),
                new Row("food", "苹果", "minecraft:apple")
        );
        List<Row> filtered = MallBrowse.filterByShopAndQuery(
                source, "weapon", "", Row::shopId, Row::name, Row::itemId);
        assertEquals(List.of("铁剑", "钻石剑"), filtered.stream().map(Row::name).toList());

        List<Row> byName = MallBrowse.filterByShopAndQuery(
                source, "", "剑", Row::shopId, Row::name, Row::itemId);
        assertEquals(List.of("铁剑", "钻石剑"), byName.stream().map(Row::name).toList());

        List<Row> both = MallBrowse.filterByShopAndQuery(
                source, "food", "apple", Row::shopId, Row::name, Row::itemId);
        assertEquals(List.of("苹果"), both.stream().map(Row::name).toList());
    }
}
