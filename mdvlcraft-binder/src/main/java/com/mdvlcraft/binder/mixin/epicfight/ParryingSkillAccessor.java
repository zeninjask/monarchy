package com.mdvlcraft.binder.mixin.epicfight;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import yesman.epicfight.skill.guard.ParryingSkill;

@Mixin(
    value = {ParryingSkill.class},
    remap = false
)
public interface ParryingSkillAccessor {
    @Accessor("PARRY_WINDOW")
    int mdvlcraft$parryWindow();
}
