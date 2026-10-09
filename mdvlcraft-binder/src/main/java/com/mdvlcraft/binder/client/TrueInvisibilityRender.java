package com.mdvlcraft.binder.client;

import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.client.event.RenderLivingEvent;

/**
 * Iron's Spells hides a truly invisible entity (armour and held items included) by cancelling
 * RenderLivingEvent.Pre. Epic Fight renders players in battle mode from the same event at the same
 * priority, runs first and cancels it itself, so Iron's never gets the chance and the player's gear
 * stays visible. This listener runs first and does Iron's check.
 */
final class TrueInvisibilityRender {
    private TrueInvisibilityRender() {
    }

    static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
        LocalPlayer viewer = Minecraft.getInstance().player;
        LivingEntity entity = event.getEntity();
        if (viewer != null && entity.hasEffect(MobEffectRegistry.TRUE_INVISIBILITY.get()) && entity.isInvisibleTo(viewer)) {
            event.setCanceled(true);
        }
    }
}
