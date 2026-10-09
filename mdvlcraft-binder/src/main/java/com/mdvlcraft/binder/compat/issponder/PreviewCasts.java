package com.mdvlcraft.binder.compat.issponder;

import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.eventbus.api.EventPriority;

/**
 * Iron's Spells Ponder previews a spell by having a FakePlayer cast it in its own dimension.
 * Epic Fight x Iron's Spells Compat cancels any cast made within a short delay of the caster's
 * last Epic Fight action, and the preview's caster has never ticked, so its "ticks since last
 * action" is 0 and every preview was refused ("This spell could not acquire a valid target").
 * Casts by that FakePlayer in the preview dimension are let through again.
 */
public final class PreviewCasts {
    private static final ResourceLocation PREVIEW_DIMENSION = ResourceLocation.fromNamespaceAndPath("iss_ponder", "spell_preview");

    private PreviewCasts() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, SpellPreCastEvent.class, PreviewCasts::onPreCast);
    }

    private static void onPreCast(SpellPreCastEvent event) {
        if (event.isCanceled()
            && event.getEntity() instanceof FakePlayer caster
            && caster.level().dimension().location().equals(PREVIEW_DIMENSION)) {
            event.setCanceled(false);
        }
    }
}
