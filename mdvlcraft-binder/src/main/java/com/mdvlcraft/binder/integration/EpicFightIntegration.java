package com.mdvlcraft.binder.integration;

import com.mdvlcraft.binder.MDVLBinder;
import net.minecraftforge.eventbus.api.IEventBus;
import yesman.epicfight.api.forgeevent.EntityPatchRegistryEvent;

public final class EpicFightIntegration {
    private EpicFightIntegration() {
    }

    public static void register(IEventBus modBus, IEventBus forgeBus) {
        modBus.addListener(EpicFightIntegration::onEntityPatchRegistry);
    }

    private static void onEntityPatchRegistry(EntityPatchRegistryEvent event) {
        MDVLBinder.LOGGER.info("[Epic Fight] {} entity types have Epic Fight patches", event.getTypeEntry().size());
    }
}
