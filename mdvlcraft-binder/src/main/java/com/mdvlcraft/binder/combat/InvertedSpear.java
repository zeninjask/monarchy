package com.mdvlcraft.binder.combat;

import com.mdvlcraft.binder.config.BinderConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Inverted Spear of Heaven: a melee hit strips every effect from the target before the damage is
 * worked out (so Resistance and the like no longer reduce it), then seals the target so no effect
 * can be applied to it for a few seconds. Every hit strips and seals again.
 */
@EventBusSubscriber(
    modid = "mdvlcraft"
)
public final class InvertedSpear {
    private static final ResourceLocation SPEAR = ResourceLocation.fromNamespaceAndPath("cursedfate", "inverted_spearof_heaven");

    private InvertedSpear() {
    }

    // LivingAttackEvent fires at the start of hurt(), before armour and effects reduce the damage.
    // LOWEST so a hit some other mod cancels (Infinity, a parry) is left alone.
    @SubscribeEvent(
        priority = EventPriority.LOWEST
    )
    public static void onAttack(LivingAttackEvent event) {
        LivingEntity target = event.getEntity();
        DamageSource source = event.getSource();
        if (!target.level().isClientSide()
            && source.getEntity() instanceof LivingEntity attacker
            && source.getDirectEntity() == attacker
            && SPEAR.equals(ForgeRegistries.ITEMS.getKey(attacker.getMainHandItem().getItem()))
            && !target.isInvulnerableTo(source)
            && !target.isDeadOrDying()) {
            target.removeAllEffects();
            target.addEffect(new MobEffectInstance(BinderEffects.SEALED.get(), BinderConfig.invertedSpearSealTicks(), 0, false, true, true), attacker);
        }
    }

    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (event.getEffectInstance().getEffect() != BinderEffects.SEALED.get() && event.getEntity().hasEffect(BinderEffects.SEALED.get())) {
            event.setResult(Result.DENY);
        }
    }
}
