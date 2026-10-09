package com.mdvlcraft.binder.epicskill;

import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.server.SPChangeSkill;
import yesman.epicfight.network.server.SPSetRemotePlayerSkill;
import yesman.epicfight.network.server.SPSetSkillContainerValue;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlot;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.gamerule.EpicFightGameRules;

public final class EpicSkillEquip {
    private EpicSkillEquip() {
    }

    public static void request(ServerPlayer player, SkillSlot slot, @Nullable Skill skill) {
        ServerPlayerPatch patch = (ServerPlayerPatch)EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (patch != null && slot.category().learnable()) {
            if (skill == null || skill.getCategory() == slot.category() && patch.getSkillCapability().hasLearned(skill)) {
                SkillContainer container = patch.getSkill(slot);
                if (container.getSkill() != skill) {
                    if (container.onReplaceCooldown()) {
                        player.f_8906_
                            .m_9829_(
                                new ClientboundSetActionBarTextPacket(
                                    Component.m_237110_("screen.mdvlcraft.skills.on_cooldown", new Object[]{container.getReplaceCooldown() / 20})
                                        .m_130940_(ChatFormatting.RED)
                                )
                            );
                    } else {
                        if (skill != null) {
                            SkillContainer other = patch.getSkillCapability().getSkillContainer(skill);
                            if (other != null) {
                                apply(patch, other, null);
                            }
                        }

                        apply(patch, container, skill);
                        int cooldown = (Integer)EpicFightGameRules.SKILL_REPLACE_COOLDOWN.getRuleValue(player.m_9236_());
                        container.setReplaceCooldown(cooldown);
                        EpicFightNetworkManager.sendToPlayer(SPSetSkillContainerValue.replaceCooldown(slot, cooldown, player.m_19879_()), player, new Object[0]);
                    }
                }
            }
        }
    }

    public static void apply(ServerPlayerPatch patch, SkillContainer container, @Nullable Skill skill) {
        ServerPlayer player = (ServerPlayer)patch.getOriginal();
        container.setSkill(skill);
        EpicFightNetworkManager.sendToPlayer(new SPChangeSkill(container.getSlot(), player.m_19879_(), skill), player, new Object[0]);
        EpicFightNetworkManager.sendToAllPlayerTrackingThisEntity(
            new SPSetRemotePlayerSkill(player.m_19879_(), container.getSlot(), skill), player, new Object[0]
        );
    }
}
