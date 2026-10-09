package com.mdvlcraft.binder.mixin.naoyaaddon;

import com.bless.naoyaaddon.procedures.SetAbilityCooldownProcedure;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
    value = {SetAbilityCooldownProcedure.class},
    remap = false
)
public abstract class SetAbilityCooldownProcedureMixin {
    @Inject(
        method = {"setCooldownForAbility"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private static void mdvlcraft$noAddonCooldown(Entity entity, double ability, double cooldown, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(
        method = {"isOnCooldown"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private static void mdvlcraft$neverOnAddonCooldown(Entity entity, double ability, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
