package com.mdvlcraft.binder.ability;

import com.mdvlcraft.binder.network.BinderNetwork;
import com.mdvlcraft.binder.network.SyncAbilitiesPacket;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

final class AbilitySync {
    private AbilitySync() {
    }

    static void send(ServerPlayer player) {
        Map<ResourceLocation, Integer> granted = new LinkedHashMap<>();
        AbilityGrants.of(player).forEach((ability, level) -> granted.put(ability.id(), level));
        BinderNetwork.sendTo(player, new SyncAbilitiesPacket(granted, Loadout.get(player)));
        TechniqueCooldowns.send(player);
    }
}
