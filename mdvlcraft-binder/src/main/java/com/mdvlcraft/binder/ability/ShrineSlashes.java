package com.mdvlcraft.binder.ability;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class ShrineSlashes {
    private ShrineSlashes() {
    }

    @Nullable
    public static ServerPlayer owner(Entity domain, String key) {
        String uuid = domain.getPersistentData().getString(key);
        if (uuid.isEmpty()) {
            return null;
        } else {
            return domain.level().getPlayerByUUID(UUID.fromString(uuid)) instanceof ServerPlayer player ? player : null;
        }
    }

    public static boolean held(ServerPlayer owner) {
        return !TechniqueRunner.isSlashing(owner);
    }
}
