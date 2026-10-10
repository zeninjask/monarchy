package com.mdvlcraft.binder.mixin.invincible;

import com.mdvlcraft.binder.client.MonkGuardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Weapons with an Invincible combo moveset (Nightfall's Feral Claws) take every press of their combo keys, right
 * click included, as combo input, which keeps Epic Fight's Guard from being raised. A Monk holding them leaves the
 * Guard key to the parry (the claws have no right-click combo).
 */
@Pseudo
@Mixin(targets = "com.p1nero.invincible.client.InputManager", remap = false)
public abstract class InputManagerMixin {
    @Inject(method = "handlePress", at = @At("HEAD"), cancellable = true, require = 0)
    private static void mdvlcraft$leaveGuardToMonk(int inputKey, CallbackInfo ci) {
        if (MonkGuardInput.guardKeyForMonk(inputKey)) {
            ci.cancel();
        }
    }
}
