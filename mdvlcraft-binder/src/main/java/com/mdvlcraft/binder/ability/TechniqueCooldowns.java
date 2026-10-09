package com.mdvlcraft.binder.ability;

import com.mdvlcraft.binder.network.BinderNetwork;
import com.mdvlcraft.binder.network.TechniqueCooldownsPacket;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class TechniqueCooldowns {
    private static final String KEY = "mdvlcraft:technique_ready";

    private TechniqueCooldowns() {
    }

    static long remaining(ServerPlayer player, Technique technique) {
        long ready = player.getPersistentData().m_128469_("mdvlcraft:technique_ready").m_128454_(technique.id().toString());
        return Math.max(0L, ready - player.m_9236_().m_46467_());
    }

    static void start(ServerPlayer player, Technique technique) {
        if (technique.cooldownTicks() != 0) {
            CompoundTag ready = player.getPersistentData().m_128469_("mdvlcraft:technique_ready");
            ready.m_128356_(technique.id().toString(), player.m_9236_().m_46467_() + technique.cooldownTicks());
            player.getPersistentData().m_128365_("mdvlcraft:technique_ready", ready);
            send(player);
        }
    }

    static void send(ServerPlayer player) {
        Map<ResourceLocation, Integer> cooling = new LinkedHashMap<>();

        for (Technique technique : Technique.values()) {
            long remaining = remaining(player, technique);
            if (remaining > 0L) {
                cooling.put(technique.id(), (int)remaining);
            }
        }

        BinderNetwork.sendTo(player, new TechniqueCooldownsPacket(cooling));
    }
}
