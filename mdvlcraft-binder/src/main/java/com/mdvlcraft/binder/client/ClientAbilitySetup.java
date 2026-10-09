package com.mdvlcraft.binder.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class ClientAbilitySetup {
    private ClientAbilitySetup() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(AbilityKeys::onRegisterKeys);
        modBus.addListener(ClientAbilitySetup::onRegisterOverlays);
        modBus.addListener(ClientAbilitySetup::onClientSetup);
        MinecraftForge.EVENT_BUS.addListener(AbilityKeys::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(ClientAbilitySetup::onLoggingOut);
        MinecraftForge.EVENT_BUS.addListener(SlateGui::onTooltipColour);
        MinecraftForge.EVENT_BUS.addListener(SpellPreviews::onClientTick);
        modBus.addListener(SlateRecruits::onAddPackFinders);
        modBus.addListener(BdHillFix::onAddPackFinders);
        MinecraftForge.EVENT_BUS.addListener(SlateRecruits::onRenderPre);
        MinecraftForge.EVENT_BUS.addListener(SlateRecruits::onRenderPost);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, RenderLivingEvent.Pre.class, TrueInvisibilityRender::onRenderLiving);
    }

    private static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "ability_hud", AbilityHud.INSTANCE);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> AbilityKeys.logConflicts(Minecraft.getInstance()));
    }

    private static void onLoggingOut(LoggingOut event) {
        ClientAbilities.reset();
        ClientStances.reset();
        ClientTechniqueCooldowns.reset();
    }
}
