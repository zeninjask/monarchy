package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.ability.Technique;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public final class ClientTechniqueCooldowns {
    private static final Map<Technique, Long> ENDS = new HashMap<>();

    private ClientTechniqueCooldowns() {
    }

    public static void update(Map<ResourceLocation, Integer> remaining) {
        long now = Minecraft.getInstance().level.getGameTime();
        ENDS.clear();
        remaining.forEach((id, ticks) -> ENDS.put(Technique.byId(id).orElseThrow(), now + ticks.intValue()));
    }

    public static void reset() {
        ENDS.clear();
    }

    static float percent(Technique technique) {
        Long end = ENDS.get(technique);
        if (end == null) {
            return 0.0F;
        } else {
            long left = end - Minecraft.getInstance().level.getGameTime();
            return left <= 0L ? 0.0F : Math.min(1.0F, (float)left / technique.cooldownTicks());
        }
    }
}
