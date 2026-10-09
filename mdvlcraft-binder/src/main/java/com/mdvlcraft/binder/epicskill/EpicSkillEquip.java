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
                        player.connection
                            .send(
                                new ClientboundSetActionBarTextPacket(
                                    Component.translatable("screen.mdvlcraft.skills.on_cooldown", new Object[]{container.getReplaceCooldown() / 20})
                                        .withStyle(ChatFormatting.RED)
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
                        int cooldown = (Integer)EpicFightGameRules.SKILL_REPLACE_COOLDOWN.getRuleValue(player.level());
                        container.setReplaceCooldown(cooldown);
                        EpicFightNetworkManager.sendToPlayer(SPSetSkillContainerValue.replaceCooldown(slot, cooldown, player.getId()), player, new Object[0]);
                    }
                }
            }
        }
    }

    public static void apply(ServerPlayerPatch patch, SkillContainer container, @Nullable Skill skill) {
        ServerPlayer player = (ServerPlayer)patch.getOriginal();
        container.setSkill(skill);
        EpicFightNetworkManager.sendToPlayer(new SPChangeSkill(container.getSlot(), player.getId(), skill), player, new Object[0]);
        EpicFightNetworkManager.sendToAllPlayerTrackingThisEntity(
            new SPSetRemotePlayerSkill(player.getId(), container.getSlot(), skill), player, new Object[0]
        );
    }
}
