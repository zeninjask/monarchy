package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.client.ponder.ShipPonderPlugin;
import com.mdvlcraft.binder.client.ponder.WeaponPonderPlugin;
import cursedfate.CtrlDashHandler;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class ClientContentSetup {
    private ClientContentSetup() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(ClientContentSetup::onClientSetup);
        MinecraftForge.EVENT_BUS.addListener(ClientContentSetup::onLoggingIn);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MinecraftForge.EVENT_BUS.unregister(CtrlDashHandler.class);
            PonderIndex.addPlugin(new WeaponPonderPlugin());
            PonderIndex.addPlugin(new ShipPonderPlugin());
        });
    }

    private static void onLoggingIn(LoggingIn event) {
        if (Minecraft.m_91087_().m_91091_()) {
            PonderIndex.reload();
        }
    }
}
