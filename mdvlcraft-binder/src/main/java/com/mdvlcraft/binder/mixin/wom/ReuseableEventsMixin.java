package com.mdvlcraft.binder.mixin.wom;

import com.mdvlcraft.binder.compat.wom.EnderObscurisTeleport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import reascer.wom.gameasset.WOMAnimations;
import yesman.epicfight.api.animation.property.AnimationParameters;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/**
 * Swaps Weapons of Miracles' Ender Obscuris teleport events (ReuseableEvents.ENDER_OBSCURIS and
 * MOB_ENDER_OBSCURIS, compiled as these lambdas in WoM 2.0.171) for {@link EnderObscurisTeleport}.
 * Each handler checks the animation it was fired for, so a WoM update that renumbers its lambdas
 * leaves other events untouched instead of replacing them.
 */
@Pseudo
@Mixin(targets = "reascer.wom.gameasset.ReuseableEvents", remap = false)
public abstract class ReuseableEventsMixin {
    @Inject(method = "lambda$static$23", at = @At("HEAD"), cancellable = true, require = 0)
    private static void mdvlcraft$enderObscuris(LivingEntityPatch<?> patch, AssetAccessor<?> animation, AnimationParameters<?, ?, ?, ?, ?, ?, ?, ?, ?, ?> params, CallbackInfo ci) {
        if (animation != null && animation.registryName().equals(WOMAnimations.ENDERSTEP_OBSCURIS.registryName())) {
            EnderObscurisTeleport.player(patch);
            ci.cancel();
        }
    }

    @Inject(method = "lambda$static$21", at = @At("HEAD"), cancellable = true, require = 0)
    private static void mdvlcraft$mobEnderObscuris(LivingEntityPatch<?> patch, AssetAccessor<?> animation, AnimationParameters<?, ?, ?, ?, ?, ?, ?, ?, ?, ?> params, CallbackInfo ci) {
        if (animation != null && animation.registryName().equals(WOMAnimations.MOB_ENDERSTEP_OBSCURIS.registryName())) {
            EnderObscurisTeleport.mob(patch);
            ci.cancel();
        }
    }
}
