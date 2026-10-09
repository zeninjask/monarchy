package com.mdvlcraft.binder.epicskill;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
    modid = "mdvlcraft"
)
public final class EpicSkillEvents {
    private EpicSkillEvents() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerLoggedInEvent event) {
        EpicSkillGrants.markDirty((ServerPlayer)event.getEntity());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent event) {
        if (event.phase == Phase.END) {
            EpicSkillGrants.flush(event.getServer());
        }
    }
}
