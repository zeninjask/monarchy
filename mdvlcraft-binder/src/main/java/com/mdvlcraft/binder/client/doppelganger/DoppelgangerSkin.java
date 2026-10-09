package com.mdvlcraft.binder.client.doppelganger;

import com.mdvlcraft.binder.doppelganger.DoppelgangerEntity;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

/** The owner's skin and arm model, from the tab list so it works even when the owner is out of view. */
final class DoppelgangerSkin {
    private DoppelgangerSkin() {
    }

    static ResourceLocation texture(DoppelgangerEntity entity) {
        return entity.ownerId().map(id -> {
            PlayerInfo info = info(id);
            return info != null ? info.getSkinLocation() : DefaultPlayerSkin.getDefaultSkin(id);
        }).orElse(DefaultPlayerSkin.getDefaultSkin());
    }

    static boolean slim(DoppelgangerEntity entity) {
        return entity.ownerId().map(id -> {
            PlayerInfo info = info(id);
            return "slim".equals(info != null ? info.getModelName() : DefaultPlayerSkin.getSkinModelName(id));
        }).orElse(false);
    }

    private static PlayerInfo info(UUID id) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        return connection == null ? null : connection.getPlayerInfo(id);
    }
}
