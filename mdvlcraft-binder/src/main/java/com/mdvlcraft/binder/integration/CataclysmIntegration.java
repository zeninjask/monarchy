package com.mdvlcraft.binder.integration;

import com.github.L_Ender.cataclysm.init.ModEntities;
import com.mdvlcraft.binder.MDVLBinder;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

public final class CataclysmIntegration {
    private CataclysmIntegration() {
    }

    public static void register(IEventBus modBus, IEventBus forgeBus) {
        modBus.addListener(CataclysmIntegration::onCommonSetup);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        MDVLBinder.LOGGER.info("[Cataclysm] {} entity types registered", ModEntities.ENTITY_TYPE.getEntries().size());
    }
}
