package com.mdvlcraft.binder.ability;

import com.mdvlcraft.binder.network.BinderNetwork;
import com.mdvlcraft.binder.network.StanceSyncPacket;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.StartTracking;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
    modid = "mdvlcraft"
)
public final class Stances {
    private static final String ELEMENT_KEY = "mdvlcraft_stance";
    private static final Set<UUID> ACTIVE = new HashSet<>();

    private Stances() {
    }

    static void press(ServerPlayer player) {
        boolean active = ACTIVE.contains(player.m_20148_());
        if (player.m_6144_()) {
            StanceElement element = element(player).next();
            saveElement(player, element);
            apply(player, active);
            player.f_8906_
                .m_9829_(
                    new ClientboundSetActionBarTextPacket(
                        Component.m_237110_(
                            active ? "stance.mdvlcraft.switched_active" : "stance.mdvlcraft.switched", new Object[]{element.displayName(), element.effect()}
                        )
                    )
                );
        } else {
            apply(player, !active);
            player.f_8906_
                .m_9829_(
                    new ClientboundSetActionBarTextPacket(
                        Component.m_237110_(
                            active ? "stance.mdvlcraft.off" : "stance.mdvlcraft.on", new Object[]{element(player).displayName(), element(player).effect()}
                        )
                    )
                );
        }
    }

    static void deactivate(ServerPlayer player) {
        if (ACTIVE.contains(player.m_20148_())) {
            apply(player, false);
        }
    }

    public static StanceElement element(Player player) {
        CompoundTag data = player.getPersistentData().m_128469_("PlayerPersisted");
        return data.m_128441_("mdvlcraft_stance") ? StanceElement.values()[data.m_128445_("mdvlcraft_stance")] : StanceElement.FIRE;
    }

    private static void saveElement(ServerPlayer player, StanceElement element) {
        CompoundTag persisted = player.getPersistentData().m_128469_("PlayerPersisted");
        persisted.m_128344_("mdvlcraft_stance", (byte)element.ordinal());
        player.getPersistentData().m_128365_("PlayerPersisted", persisted);
    }

    private static void apply(ServerPlayer player, boolean active) {
        StanceElement current = element(player);

        for (StanceElement element : StanceElement.values()) {
            AttributeInstance instance = player.m_21051_(element.attribute.get());
            if (instance == null) {
                throw new IllegalStateException("Player has no " + element.attribute.get().m_22087_() + " attribute");
            }

            instance.m_22120_(element.modifier.m_22209_());
            if (active && element == current) {
                instance.m_22118_(element.modifier);
            }
        }

        if (active) {
            ACTIVE.add(player.m_20148_());
        } else {
            ACTIVE.remove(player.m_20148_());
        }

        BinderNetwork.sendToTrackingAndSelf(player, packet(player));
    }

    private static StanceSyncPacket packet(ServerPlayer player) {
        return new StanceSyncPacket(player.m_19879_(), element(player), ACTIVE.contains(player.m_20148_()));
    }

    @SubscribeEvent
    public static void onLogin(PlayerLoggedInEvent event) {
        ServerPlayer player = (ServerPlayer)event.getEntity();
        BinderNetwork.sendTo(player, packet(player));
    }

    @SubscribeEvent
    public static void onLogout(PlayerLoggedOutEvent event) {
        ACTIVE.remove(event.getEntity().m_20148_());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerRespawnEvent event) {
        ServerPlayer player = (ServerPlayer)event.getEntity();
        ACTIVE.remove(player.m_20148_());
        BinderNetwork.sendToTrackingAndSelf(player, packet(player));
    }

    @SubscribeEvent
    public static void onStartTracking(StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target) {
            BinderNetwork.sendTo((ServerPlayer)event.getEntity(), packet(target));
        }
    }
}
