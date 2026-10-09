package com.mdvlcraft.binder.mixin.naoyaaddon;

import com.bless.naoyaaddon.procedures.ProjectionDashProcedure;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
    value = {ProjectionDashProcedure.class},
    remap = false
)
public abstract class ProjectionDashProcedureMixin {
    @Inject(
        method = {"runCommand"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private static void mdvlcraft$noShakeOrWind(Entity entity, String command, CallbackInfo ci) {
        if (command.contains("cursedfate:shake") || command.contains("photon:windflash1")) {
            ci.cancel();
        }
    }
}
