package com.mdvlcraft.binder.mixin.epicfight;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.collider.Collider;
import yesman.epicfight.api.collider.MultiCollider;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/**
 * An attack animation made for one skeleton can be played by an entity with another: a recruit
 * swinging Super Golem's Golem Heart asks its humanoid armature for the golem's "garm_down_1_R"
 * joint, Epic Fight throws, and the server crashes on every tick that recruit is loaded.
 * Such a swing now hits nothing instead.
 */
@Mixin(value = {Collider.class, MultiCollider.class}, remap = false)
public abstract class ColliderMixin {
    @Inject(method = "updateAndSelectCollideEntity", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$missingJoint(
        LivingEntityPatch<?> entitypatch,
        AttackAnimation attackAnimation,
        float prevElapsedTime,
        float elapsedTime,
        Joint joint,
        float attackSpeed,
        CallbackInfoReturnable<List<Entity>> cir
    ) {
        Armature armature = entitypatch.getArmature();
        if (joint != null && armature != null && !armature.rootJoint.equals(joint) && !armature.hasJoint(joint.getName())) {
            cir.setReturnValue(new ArrayList<>());
        }
    }
}
