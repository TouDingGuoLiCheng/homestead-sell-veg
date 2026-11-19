package com.homestead.sellveg.client;

import com.homestead.sellveg.NetShopMod;
import com.homestead.sellveg.config.SellVegClientConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ShopBackground {
    public static final ResourceLocation DEFAULT =
            new ResourceLocation(NetShopMod.MOD_ID, "textures/gui/shop_bg.png");

    private ShopBackground() {
    }

    /**
     * Player-customizable mall/shop panel background. Invalid IDs fall back to DEFAULT.
     */
    public static ResourceLocation current() {
        try {
            String raw = SellVegClientConfig.SHOP_BACKGROUND.get();
            if (raw == null || raw.isBlank()) {
                return DEFAULT;
            }
            ResourceLocation id;
            try {
                id = new ResourceLocation(raw.trim());
            } catch (Exception ex) {
                return DEFAULT;
            }
            return id;
        } catch (Exception ignored) {
            return DEFAULT;
        }
    }
}
