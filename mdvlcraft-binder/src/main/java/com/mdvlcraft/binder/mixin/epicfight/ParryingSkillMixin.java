package com.mdvlcraft.binder.mixin.epicfight;

import com.mdvlcraft.binder.attribute.BinderAttributes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.guard.ParryingSkill;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.entity.eventlistener.TakeDamageEvent.Attack;

@Mixin(
    value = {ParryingSkill.class},
    remap = false
)
public abstract class ParryingSkillMixin {
    @Redirect(
        method = {"guard"},
        at = @At(
            value = "FIELD",
            target = "Lyesman/epicfight/skill/guard/ParryingSkill;PARRY_WINDOW:I",
            opcode = 180
        )
    )
    private int mdvlcraft$parryWindow(
        ParryingSkill skill, SkillContainer container, CapabilityItem itemCapability, Attack event, float knockback, float impact, boolean advanced
    ) {
        double bonus = ((ServerPlayer)((ServerPlayerPatch)event.getPlayerPatch()).getOriginal()).m_21133_((Attribute)BinderAttributes.PARRY_WINDOW.get());
        return (int)Math.round(((ParryingSkillAccessor)skill).mdvlcraft$parryWindow() * (1.0 + bonus));
    }
}
