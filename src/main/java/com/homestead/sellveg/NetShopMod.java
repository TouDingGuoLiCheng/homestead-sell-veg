package com.homestead.sellveg;

import com.homestead.sellveg.command.SellVegCommands;
import com.homestead.sellveg.config.SellVegClientConfig;
import com.homestead.sellveg.config.SellVegConfig;
import com.homestead.sellveg.network.ModNetwork;
import com.homestead.sellveg.registry.ModBlockEntities;
import com.homestead.sellveg.registry.ModBlocks;
import com.homestead.sellveg.registry.ModCreativeTabs;
import com.homestead.sellveg.registry.ModItems;
import com.homestead.sellveg.registry.ModSounds;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(NetShopMod.MOD_ID)
public class NetShopMod {
    public static final String MOD_ID = "homestead_sell_veg";

    private static final Logger LOGGER = LogUtils.getLogger();

    public NetShopMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        modEventBus.addListener(this::commonSetup);
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SellVegConfig.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SellVegClientConfig.SPEC);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
        LOGGER.info("Homestead Sell Veggies loaded.");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        SellVegCommands.register(event.getDispatcher(), event.getBuildContext());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Homestead Sell Veggies server starting.");
    }
}
