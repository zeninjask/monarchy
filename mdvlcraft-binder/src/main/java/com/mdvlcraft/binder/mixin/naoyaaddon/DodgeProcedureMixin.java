package com.mdvlcraft.binder.mixin.naoyaaddon;

import com.bless.naoyaaddon.procedures.DodgeProcedure;
import com.mdvlcraft.binder.ability.ProjectionSorcery;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
    value = {DodgeProcedure.class},
    remap = false
)
public abstract class DodgeProcedureMixin {
    @Inject(
        method = {"hasFlow"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private static void mdvlcraft$binderFlow(Player player, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(ProjectionSorcery.flow(player));
    }
}
