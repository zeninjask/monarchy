package com.mdvlcraft.binder.mixin.playerrevive;

import com.mdvlcraft.binder.client.AbilityKeys;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * PlayerRevive gives up (while bleeding out) when you hold the attack key, which cannot be rebound on its own.
 * Hold the Binder's own "Give Up" key instead; its on-screen hint names that key too.
 */
@Pseudo
@Mixin(targets = "team.creative.playerrevive.client.ReviveEventClient", remap = false)
public abstract class ReviveEventClientMixin {
    @Redirect(
        method = {"clientTick", "tick"},
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;keyAttack:Lnet/minecraft/client/KeyMapping;", opcode = Opcodes.GETFIELD,
            remap = true),
        require = 0
    )
    private KeyMapping mdvlcraft$giveUpKey(Options options) {
        return AbilityKeys.GIVE_UP;
    }
}
