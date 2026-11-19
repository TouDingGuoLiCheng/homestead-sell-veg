package com.homestead.sellveg.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Client-only shop UI options. Players can override the mall background texture.
 */
public final class SellVegClientConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.ConfigValue<String> SHOP_BACKGROUND = BUILDER
            .comment("GUI background texture (namespace:path). Empty uses the built-in pixel shop art.",
                    "Example: homestead_sell_veg:textures/gui/shop_bg.png",
                    "Put a custom PNG in a resource pack at the same path, or point this at another texture.")
            .define("shopBackgroundTexture", "homestead_sell_veg:textures/gui/shop_bg.png");

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private SellVegClientConfig() {
    }
}
