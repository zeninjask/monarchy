package com.mdvlcraft.binder.mixin.wom;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import reascer.wom.gameasset.WOMAnimations;
import reascer.wom.skill.identity.ShootingStyleSkill;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.skill.SkillContainer;

/**
 * Shooting Style fills its kick combo in its constructor, which Epic Fight runs during registration,
 * before WoM's animations exist, so the combo was three nulls: sneak-attacking with an empty hand
 * cancelled the punch and then had no kick to play. Refill it whenever the skill is equipped.
 */
@Pseudo
@Mixin(
    targets = {"reascer.wom.skill.identity.ShootingStyleSkill"},
    remap = false
)
public abstract class ShootingStyleSkillMixin {
    @Inject(
        method = {"onInitiate"},
        at = {@At("HEAD")},
        require = 0
    )
    private void mdvlcraft$fillCombo(SkillContainer container, CallbackInfo ci) {
        AnimationAccessor<?>[] combo = ShootingStyleSkill.combo;
        if (combo == null || combo.length != 3 || combo[0] == null || combo[1] == null || combo[2] == null) {
            ShootingStyleSkill.combo = new AnimationAccessor[]{WOMAnimations.KICK_AUTO_1, WOMAnimations.KICK_AUTO_2, WOMAnimations.KICK_AUTO_3};
        }
    }
}
