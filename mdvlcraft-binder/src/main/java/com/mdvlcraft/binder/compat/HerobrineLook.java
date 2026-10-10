package com.mdvlcraft.binder.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * True Herobrine is meant to stare at you from a distance, but his only look AI is vanilla's look-at-player goal:
 * it starts on a 2% chance a tick, gives up after 2-4 seconds and needs a clear line of sight, which the trees he
 * stands among nearly always block. So his head hardly ever turned. Point his head at the nearest player every tick
 * instead (the body follows on its own when he stands still).
 */
public final class HerobrineLook {
    private static final ResourceLocation HEROBRINE = new ResourceLocation("true_herobrine", "herobrine");
    private static final double RANGE = 256.0;

    private HerobrineLook() {
    }

    public static void register() {
        if (ModList.get().isLoaded(HEROBRINE.getNamespace())) {
            MinecraftForge.EVENT_BUS.addListener(HerobrineLook::onLivingTick);
        }
    }

    private static void onLivingTick(LivingTickEvent event) {
        if (event.getEntity() instanceof Mob mob && !mob.level().isClientSide && HEROBRINE.equals(ForgeRegistries.ENTITY_TYPES.getKey(mob.getType()))) {
            Player player = mob.level().getNearestPlayer(mob, RANGE);
            if (player != null && !player.isSpectator()) {
                mob.getLookControl().setLookAt(player, 30.0F, (float) mob.getMaxHeadXRot());
            }
        }
    }
}
