package com.mdvlcraft.binder.mixin.naoyaaddon;

import com.bless.naoyaaddon.procedures.BarrageProcedure;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
    value = {BarrageProcedure.class},
    remap = false
)
public abstract class BarrageProcedureMixin {
    @Inject(
        method = {"getCommandResult"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private static void mdvlcraft$maxOutput(Entity entity, String command, CallbackInfoReturnable<String> cir) {
        if (command.endsWith("@s Output")) {
            cir.setReturnValue("100");
        } else if (command.endsWith("@s CursedEnegry")) {
            cir.setReturnValue("1000000");
        }
    }
}
