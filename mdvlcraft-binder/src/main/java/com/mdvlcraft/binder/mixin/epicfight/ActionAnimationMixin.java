package com.mdvlcraft.binder.mixin.epicfight;

import com.mdvlcraft.binder.attribute.BinderAttributes;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.api.animation.types.ActionAnimation;
import yesman.epicfight.api.animation.types.DodgeAnimation;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

@Mixin(
    value = {ActionAnimation.class},
    remap = false
)
public abstract class ActionAnimationMixin {
    @Inject(
        method = {"getCoordVector"},
        at = {@At("RETURN")},
        cancellable = true
    )
    private void mdvlcraft$dodgeDistance(
        LivingEntityPatch<?> entitypatch, AssetAccessor<? extends DynamicAnimation> animation, CallbackInfoReturnable<Vec3> cir
    ) {
        if (this instanceof DodgeAnimation && entitypatch.getOriginal() instanceof Player player) {
            double bonus = player.m_21133_((Attribute)BinderAttributes.DODGE_DISTANCE.get());
            if (bonus > 0.0) {
                Vec3 move = (Vec3)cir.getReturnValue();
                cir.setReturnValue(new Vec3(move.f_82479_ * (1.0 + bonus), move.f_82480_, move.f_82481_ * (1.0 + bonus)));
            }
        }
    }
}
