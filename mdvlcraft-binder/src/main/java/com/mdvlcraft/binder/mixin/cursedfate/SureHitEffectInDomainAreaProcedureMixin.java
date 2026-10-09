package com.mdvlcraft.binder.mixin.cursedfate;

import com.mdvlcraft.binder.ability.ShrineSlashes;
import cursedfate.procedures.SureHitEffectInDomainAreaProcedure;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
    value = {SureHitEffectInDomainAreaProcedure.class},
    remap = false
)
public abstract class SureHitEffectInDomainAreaProcedureMixin {
    @Inject(
        method = {"execute"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private static void mdvlcraft$holdShrineSlashes(LevelAccessor world, double x, double y, double z, Entity domain, CallbackInfo ci) {
        if (domain != null) {
            ServerPlayer owner = ShrineSlashes.owner(domain, "CurrentDomainOwner");
            if (owner != null && "malevolentshrine".equals(owner.getPersistentData().m_128461_("DomainID")) && ShrineSlashes.held(owner)) {
                ci.cancel();
            }
        }
    }
}
