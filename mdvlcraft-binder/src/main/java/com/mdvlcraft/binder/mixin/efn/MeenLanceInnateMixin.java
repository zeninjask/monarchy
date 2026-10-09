package com.mdvlcraft.binder.mixin.efn;

import com.hm.efn.skill.weapon_innate.MeenLanceInnate;
import com.hm.efn.gameasset.animations.EFNLanceAnimations;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;

/**
 * MeenLanceInnate copies its animations into static final fields when the class loads. Epic Fight builds skills
 * during registration, before animations are registered (common setup), so those copies are null and
 * the charge and release animations never match or play. Read the live animation fields instead.
 */
@Mixin(
    value = {MeenLanceInnate.class},
    remap = false
)
public abstract class MeenLanceInnateMixin {
    @Redirect(
        method = {"handleClientInput", "isChargeRelatedAnimation", "isPlayingFullChargeAnimation", "lambda$onInitiate$0", "playAttackAnimation", "releaseAttack", "startCharging"},
        at = @At(value = "FIELD", target = "Lcom/hm/efn/skill/weapon_innate/MeenLanceInnate;CHARGING_ANIM:Lyesman/epicfight/api/animation/AnimationManager$AnimationAccessor;", opcode = Opcodes.GETSTATIC),
        require = 0
    )
    private AnimationAccessor<?> mdvlcraft$chargingAnim() {
        return EFNLanceAnimations.NF_MEEN_CHARGING;
    }

    @Redirect(
        method = {"handleClientInput", "isChargeRelatedAnimation", "isPlayingFullChargeAnimation", "lambda$onInitiate$0", "playAttackAnimation", "releaseAttack", "startCharging"},
        at = @At(value = "FIELD", target = "Lcom/hm/efn/skill/weapon_innate/MeenLanceInnate;FULL_CHARGE_ANIM:Lyesman/epicfight/api/animation/AnimationManager$AnimationAccessor;", opcode = Opcodes.GETSTATIC),
        require = 0
    )
    private AnimationAccessor<?> mdvlcraft$fullChargeAnim() {
        return EFNLanceAnimations.NF_MEEN_CHARGE3;
    }

    @Redirect(
        method = {"handleClientInput", "isChargeRelatedAnimation", "isPlayingFullChargeAnimation", "lambda$onInitiate$0", "playAttackAnimation", "releaseAttack", "startCharging"},
        at = @At(value = "FIELD", target = "Lcom/hm/efn/skill/weapon_innate/MeenLanceInnate;MID_CHARGE_ANIM:Lyesman/epicfight/api/animation/AnimationManager$AnimationAccessor;", opcode = Opcodes.GETSTATIC),
        require = 0
    )
    private AnimationAccessor<?> mdvlcraft$midChargeAnim() {
        return EFNLanceAnimations.NF_MEEN_CHARGE2;
    }

    @Redirect(
        method = {"handleClientInput", "isChargeRelatedAnimation", "isPlayingFullChargeAnimation", "lambda$onInitiate$0", "playAttackAnimation", "releaseAttack", "startCharging"},
        at = @At(value = "FIELD", target = "Lcom/hm/efn/skill/weapon_innate/MeenLanceInnate;LOW_CHARGE_ANIM:Lyesman/epicfight/api/animation/AnimationManager$AnimationAccessor;", opcode = Opcodes.GETSTATIC),
        require = 0
    )
    private AnimationAccessor<?> mdvlcraft$lowChargeAnim() {
        return EFNLanceAnimations.NF_MEEN_CHARGE1;
    }

    @Redirect(
        method = {"handleClientInput", "isChargeRelatedAnimation", "isPlayingFullChargeAnimation", "lambda$onInitiate$0", "playAttackAnimation", "releaseAttack", "startCharging"},
        at = @At(value = "FIELD", target = "Lcom/hm/efn/skill/weapon_innate/MeenLanceInnate;MIN_CHARGE_ANIM:Lyesman/epicfight/api/animation/AnimationManager$AnimationAccessor;", opcode = Opcodes.GETSTATIC),
        require = 0
    )
    private AnimationAccessor<?> mdvlcraft$minChargeAnim() {
        return EFNLanceAnimations.NF_MEEN_AUTO3;
    }
}
