package com.homestead.sellveg.shop;

public record PresetShop(String id, ShopKind kind) {
    public String langKey() {
        return "shop.homestead_sell_veg." + id;
    }
}
