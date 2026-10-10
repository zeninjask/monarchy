package com.mdvlcraft.binder.mixin.irons;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastType;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hasty Casting (Epic Fight x Iron's Spells mover skill) also halves the cast time of spells that charge up. The
 * skill marks the player with its casting-movement modifier while it is equipped, so that is how it is detected.
 * Continuous spells are left alone: their "cast time" is how long they channel.
 */
@Mixin(value = AbstractSpell.class, remap = false)
public abstract class AbstractSpellCastTimeMixin {
    private static final UUID HASTY_CASTING = UUID.fromString("0aa15453-26e2-4e85-8042-1fe4c65a8c3c");

    @Inject(method = "getEffectiveCastTime", at = @At("RETURN"), cancellable = true)
    private void mdvlcraft$hastyCasting(int spellLevel, @Nullable LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        if (entity == null || ((AbstractSpell) (Object) this).getCastType() == CastType.CONTINUOUS) {
            return;
        }
        AttributeInstance casting = entity.getAttribute(AttributeRegistry.CASTING_MOVESPEED.get());
        if (casting != null && casting.getModifier(HASTY_CASTING) != null) {
            cir.setReturnValue(Math.round(cir.getReturnValue() * 0.5F));
        }
    }
}
