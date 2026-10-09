package com.mdvlcraft.binder.mixin.efn;

import com.hm.efn.skill.weapon_innate.RuinsGreatSwordInnate;
import com.hm.efn.gameasset.animations.EFNGreatSwordAnimations;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;

/**
 * RuinsGreatSwordInnate copies its animations into static final fields when the class loads. Epic Fight builds skills
 * during registration, before animations are registered (common setup), so those copies are null and
 * the charge and release animations never match or play. Read the live animation fields instead.
 */
@Mixin(
    value = {RuinsGreatSwordInnate.class},
    remap = false
)
public abstract class RuinsGreatSwordInnateMixin {
    @Redirect(
        method = {"forceReleaseDueToLowStamina", "isChargeRelatedAnimation", "isPlayingChargeAnimation", "lambda$onInitiate$1", "playAttackAnimation", "startCharging"},
        at = @At(value = "FIELD", target = "Lcom/hm/efn/skill/weapon_innate/RuinsGreatSwordInnate;CHARGING_ANIM:Lyesman/epicfight/api/animation/AnimationManager$AnimationAccessor;", opcode = Opcodes.GETSTATIC),
        require = 0
    )
    private AnimationAccessor<?> mdvlcraft$chargingAnim() {
        return EFNGreatSwordAnimations.NG_GREATSWORD_CHARGING;
    }

    @Redirect(
        method = {"forceReleaseDueToLowStamina", "isChargeRelatedAnimation", "isPlayingChargeAnimation", "lambda$onInitiate$1", "playAttackAnimation", "startCharging"},
        at = @At(value = "FIELD", target = "Lcom/hm/efn/skill/weapon_innate/RuinsGreatSwordInnate;FULL_CHARGE_ANIM:Lyesman/epicfight/api/animation/AnimationManager$AnimationAccessor;", opcode = Opcodes.GETSTATIC),
        require = 0
    )
    private AnimationAccessor<?> mdvlcraft$fullChargeAnim() {
        return EFNGreatSwordAnimations.NG_GREATSWORD_CHARG1MAX_FIRST;
    }

    @Redirect(
        method = {"forceReleaseDueToLowStamina", "isChargeRelatedAnimation", "isPlayingChargeAnimation", "lambda$onInitiate$1", "playAttackAnimation", "startCharging"},
        at = @At(value = "FIELD", target = "Lcom/hm/efn/skill/weapon_innate/RuinsGreatSwordInnate;LOW_CHARGE_ANIM:Lyesman/epicfight/api/animation/AnimationManager$AnimationAccessor;", opcode = Opcodes.GETSTATIC),
        require = 0
    )
    private AnimationAccessor<?> mdvlcraft$lowChargeAnim() {
        return EFNGreatSwordAnimations.NG_GREATSWORD_CHARG1MIN;
    }
}
