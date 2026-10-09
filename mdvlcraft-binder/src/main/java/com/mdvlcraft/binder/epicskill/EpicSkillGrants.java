package com.mdvlcraft.binder.epicskill;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import yesman.epicfight.gameasset.EpicFightSkills;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.server.SPAddLearnedSkill;
import yesman.epicfight.network.server.SPRemoveSkillAndLearn;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlot;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.skill.CapabilitySkill;

public final class EpicSkillGrants {
    private static final Set<Skill> TREE_SKILLS = new HashSet<>();
    private static final Map<UUID, Set<EpicSkillReward>> ACTIVE = new HashMap<>();
    private static final Set<UUID> DIRTY = new HashSet<>();

    private EpicSkillGrants() {
    }

    static void register(Skill skill) {
        TREE_SKILLS.add(skill);
    }

    static void set(ServerPlayer player, EpicSkillReward reward, boolean active) {
        Set<EpicSkillReward> rewards = ACTIVE.computeIfAbsent(player.getUUID(), uuid -> new HashSet<>());
        if (active ? rewards.add(reward) : rewards.remove(reward)) {
            DIRTY.add(player.getUUID());
        }

        if (rewards.isEmpty()) {
            ACTIVE.remove(player.getUUID());
        }
    }

    static void dispose(EpicSkillReward reward) {
        ACTIVE.forEach((uuid, rewards) -> {
            if (rewards.remove(reward)) {
                DIRTY.add(uuid);
            }
        });
        ACTIVE.values().removeIf(Set::isEmpty);
    }

    public static void markDirty(ServerPlayer player) {
        DIRTY.add(player.getUUID());
    }

    public static void flush(MinecraftServer server) {
        for (UUID uuid : DIRTY) {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player != null) {
                sync(player);
            }
        }

        DIRTY.clear();
    }

    private static void sync(ServerPlayer player) {
        ServerPlayerPatch patch = (ServerPlayerPatch)EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (patch == null) {
            throw new IllegalStateException(player.getGameProfile().getName() + " has no Epic Fight player patch");
        } else {
            CapabilitySkill skills = patch.getSkillCapability();
            Set<Skill> granted = new HashSet<>();
            ACTIVE.getOrDefault(player.getUUID(), Set.of()).forEach(reward -> granted.add(reward.skill));

            for (Skill skill : TREE_SKILLS) {
                boolean learned = skills.hasLearned(skill);
                if (granted.contains(skill) && !learned) {
                    learn(player, skills, skill);
                } else if (!granted.contains(skill) && learned) {
                    forget(player, patch, skills, skill);
                }
            }

            for (Skill basic : new Skill[]{EpicFightSkills.ROLL, EpicFightSkills.STEP, EpicFightSkills.GUARD, EpicFightSkills.PARRYING}) {
                if (!skills.hasLearned(basic)) {
                    learn(player, skills, basic);
                }
            }

            equipIfEmpty(patch, SkillSlots.DODGE, EpicFightSkills.ROLL);
            equipIfEmpty(patch, SkillSlots.GUARD, EpicFightSkills.GUARD);
        }
    }

    private static void learn(ServerPlayer player, CapabilitySkill skills, Skill skill) {
        skills.addLearnedSkill(skill);
        EpicFightNetworkManager.sendToPlayer(new SPAddLearnedSkill(new String[]{skill.getRegistryName().toString()}), player, new Object[0]);
    }

    private static void forget(ServerPlayer player, ServerPlayerPatch patch, CapabilitySkill skills, Skill skill) {
        SkillContainer equipped = skills.getSkillContainer(skill);
        SkillSlot slot = equipped != null ? equipped.getSlot() : firstSlotFor(skill);
        if (equipped != null) {
            EpicSkillEquip.apply(patch, equipped, null);
        }

        skills.removeLearnedSkill(skill);
        EpicFightNetworkManager.sendToPlayer(new SPRemoveSkillAndLearn(slot, skill), player, new Object[0]);
    }

    private static void equipIfEmpty(ServerPlayerPatch patch, SkillSlot slot, Skill skill) {
        SkillContainer container = patch.getSkill(slot);
        if (container.getSkill() == null) {
            EpicSkillEquip.apply(patch, container, skill);
        }
    }

    private static SkillSlot firstSlotFor(Skill skill) {
        return SkillSlot.ENUM_MANAGER
            .universalValues()
            .stream()
            .filter(slot -> slot.category() == skill.getCategory())
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No slot for " + skill.getRegistryName()));
    }
}
