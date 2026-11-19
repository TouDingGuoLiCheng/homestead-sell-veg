package com.homestead.sellveg.registry;

import com.homestead.sellveg.NetShopMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, NetShopMod.MOD_ID);

    public static final RegistryObject<SoundEvent> PARCEL_BOX_OPEN = SOUND_EVENTS.register("parcel_box_open",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(NetShopMod.MOD_ID, "parcel_box_open")));

    private ModSounds() {
    }
}
