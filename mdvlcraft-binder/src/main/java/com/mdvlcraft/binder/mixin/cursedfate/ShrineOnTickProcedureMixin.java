package com.mdvlcraft.binder.mixin.cursedfate;

import com.mdvlcraft.binder.ability.ShrineSlashes;
import cursedfate.procedures.ShrineOnTickProcedure;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
    value = {ShrineOnTickProcedure.class},
    remap = false
)
public abstract class ShrineOnTickProcedureMixin {
    @Inject(
        method = {"execute"},
        at = {@At("HEAD")}
    )
    private static void mdvlcraft$holdShrineSlashes(LevelAccessor world, double x, double y, double z, Entity shrine, CallbackInfo ci) {
        if (shrine != null) {
            ServerPlayer owner = ShrineSlashes.owner(shrine, "PlayerUUID");
            if (owner != null && ShrineSlashes.held(owner)) {
                shrine.getPersistentData().putDouble("domainnum2", 0.0);
            }
        }
    }
}
