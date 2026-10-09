package com.mdvlcraft.binder.mixin.efn;

import com.hm.efn.util.EffekUnits;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
    value = {EffekUnits.class},
    remap = false
)
public abstract class EffekUnitsMixin {
    @Inject(
        method = {"VFXENABLE"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private static void mdvlcraft$noServerEffects(CallbackInfoReturnable<Boolean> cir) {
        if (FMLEnvironment.dist.isDedicatedServer()) {
            cir.setReturnValue(false);
        }
    }
}
