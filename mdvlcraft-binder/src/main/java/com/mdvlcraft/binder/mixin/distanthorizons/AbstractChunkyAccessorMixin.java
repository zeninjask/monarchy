package com.mdvlcraft.binder.mixin.distanthorizons;

import com.mdvlcraft.binder.mixin.chunky.ChunkyProviderAccessor;
import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.AbstractChunkyAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
    value = {AbstractChunkyAccessor.class},
    remap = false
)
public abstract class AbstractChunkyAccessorMixin {
    @Inject(
        method = {"tryRunFirstTimeSetup"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private void mdvlcraft$waitForChunky(CallbackInfo ci) {
        if (ChunkyProviderAccessor.mdvlcraft$instance() == null) {
            ci.cancel();
        }
    }
}
