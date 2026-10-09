package com.mdvlcraft.binder.ability;

import com.bless.naoyaaddon.network.NaoyaAddonVariables;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class ProjectionSorcery {
    public static final String MAX_OUTPUT = "100";
    public static final String ENOUGH_ENERGY = "1000000";
    private static final String FLOW_KEY = "mdvlcraft:kick_flow";

    private ProjectionSorcery() {
    }

    static void prepare(ServerPlayer player) {
        player.getCapability(NaoyaAddonVariables.PLAYER_VARIABLES_CAPABILITY).ifPresent(variables -> {
            variables.NewTechnique = variables.CursedfateTechnique;
            variables.NaobitoProjectionStyle = true;
            variables.JaronaProjectionStyle = false;
            variables.syncPlayerVariables(player);
        });
    }

    static void toggleFlow(ServerPlayer player) {
        boolean flow = !flow(player);
        player.getPersistentData().m_128379_("mdvlcraft:kick_flow", flow);
        player.f_8906_
            .m_9829_(
                new ClientboundSetActionBarTextPacket(
                    Component.m_237115_(flow ? "ability.mdvlcraft.follow_up_kick.flow" : "ability.mdvlcraft.follow_up_kick.normal")
                        .m_130940_(ChatFormatting.BLUE)
                )
            );
    }

    public static boolean flow(Player player) {
        return player.getPersistentData().m_128471_("mdvlcraft:kick_flow");
    }
}
