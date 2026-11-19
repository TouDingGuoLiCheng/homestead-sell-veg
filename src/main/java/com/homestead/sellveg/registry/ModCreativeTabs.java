package com.homestead.sellveg.registry;

import com.homestead.sellveg.NetShopMod;
import com.homestead.sellveg.item.ParcelBoxItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, NetShopMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.homestead_sell_veg"))
                    .icon(() -> new ItemStack(ModItems.SOURCE_PERMIT.get()))
                    .displayItems((params, output) -> {
                        // Forge 1.20.1 requires creative-tab stacks to have count == 1.
                        output.accept(ModItems.DROP_POINT.get());
                        output.accept(ParcelBoxItem.createPrefilledDemo());
                        output.accept(ModItems.SOURCE_PERMIT.get());
                        output.accept(ModItems.QUICK_LISTER.get());
                    })
                    .build()
    );

    private ModCreativeTabs() {
    }
}
