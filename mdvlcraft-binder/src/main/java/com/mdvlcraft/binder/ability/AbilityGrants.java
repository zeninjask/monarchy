package com.mdvlcraft.binder.ability;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class AbilityGrants {
    private static final Map<UUID, Set<AbilityReward>> ACTIVE = new HashMap<>();
    private static final Set<UUID> DIRTY = new HashSet<>();

    private AbilityGrants() {
    }

    static void set(ServerPlayer player, AbilityReward reward, boolean active) {
        Set<AbilityReward> rewards = ACTIVE.computeIfAbsent(player.m_20148_(), uuid -> new HashSet<>());
        boolean changed = active ? rewards.add(reward) : rewards.remove(reward);
        if (rewards.isEmpty()) {
            ACTIVE.remove(player.m_20148_());
        }

        if (changed) {
            if (active && reward.ability instanceof SpellAbility spellAbility && spellAbility.spell().requiresLearning()) {
                MagicData.getPlayerMagicData(player).getSyncedData().learnSpell(spellAbility.spell());
            }

            if (!active && reward.ability == StanceAbility.INSTANCE && level(player, StanceAbility.INSTANCE) == 0) {
                Stances.deactivate(player);
            }

            DIRTY.add(player.m_20148_());
        }
    }

    static void dispose(AbilityReward reward) {
        ACTIVE.forEach((uuid, rewards) -> {
            if (rewards.remove(reward)) {
                DIRTY.add(uuid);
            }
        });
        ACTIVE.values().removeIf(Set::isEmpty);
    }

    public static Map<Ability, Integer> of(ServerPlayer player) {
        Map<Ability, Integer> granted = new LinkedHashMap<>();

        for (AbilityReward reward : ACTIVE.getOrDefault(player.m_20148_(), Set.of())) {
            granted.merge(reward.ability, reward.level, Math::max);
        }

        return granted;
    }

    public static int level(ServerPlayer player, Ability ability) {
        return of(player).getOrDefault(ability, 0);
    }

    static void markDirty(ServerPlayer player) {
        DIRTY.add(player.m_20148_());
    }

    static void flush(MinecraftServer server) {
        for (UUID uuid : DIRTY) {
            ServerPlayer player = server.m_6846_().m_11259_(uuid);
            if (player != null) {
                AbilitySync.send(player);
            }
        }

        DIRTY.clear();
    }
}
