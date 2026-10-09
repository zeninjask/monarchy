package com.mdvlcraft.binder.mixin.epicfight;

import java.util.function.Supplier;
import net.minecraftforge.network.NetworkEvent.Context;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.network.client.CPChangeSkill;

@Mixin(
    value = {CPChangeSkill.class},
    remap = false
)
public abstract class CPChangeSkillMixin {
    @Inject(
        method = {"handle"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private static void mdvlcraft$ignore(CPChangeSkill msg, Supplier<Context> ctx, CallbackInfo ci) {
        ctx.get().setPacketHandled(true);
        ci.cancel();
    }
}
