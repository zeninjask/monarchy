package com.mdvlcraft.binder.client.doppelganger;

import com.mdvlcraft.binder.doppelganger.DoppelgangerSetup;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import yesman.epicfight.api.client.forgeevent.PatchedRenderersEvent;

public final class DoppelgangerClientSetup {
    private DoppelgangerClientSetup() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(DoppelgangerClientSetup::onRegisterRenderers);
        modBus.addListener(DoppelgangerClientSetup::onPatchedRenderers);
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(DoppelgangerSetup.DOPPELGANGER.get(), DoppelgangerRenderer::new);
    }

    private static void onPatchedRenderers(PatchedRenderersEvent.Add event) {
        event.addPatchedEntityRenderer(
            DoppelgangerSetup.DOPPELGANGER.get(),
            type -> new PDoppelgangerRenderer(event.getContext(), type).initLayerLast(event.getContext(), type)
        );
    }
}
