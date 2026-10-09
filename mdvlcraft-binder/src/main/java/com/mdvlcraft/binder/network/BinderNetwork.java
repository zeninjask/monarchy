package com.mdvlcraft.binder.network;

import com.mdvlcraft.binder.epicskill.EquipSkillPacket;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class BinderNetwork {
    private static final String PROTOCOL = "4";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        ResourceLocation.fromNamespaceAndPath("mdvlcraft", "main"), () -> "4", "4"::equals, "4"::equals
    );

    private BinderNetwork() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(
            id++,
            SyncAbilitiesPacket.class,
            SyncAbilitiesPacket::encode,
            SyncAbilitiesPacket::decode,
            SyncAbilitiesPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
            id++,
            AssignSlotPacket.class,
            AssignSlotPacket::encode,
            AssignSlotPacket::decode,
            AssignSlotPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
            id++, CastPacket.class, CastPacket::encode, CastPacket::decode, CastPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
            id++,
            EquipSkillPacket.class,
            EquipSkillPacket::encode,
            EquipSkillPacket::decode,
            EquipSkillPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
            id++,
            StanceSyncPacket.class,
            StanceSyncPacket::encode,
            StanceSyncPacket::decode,
            StanceSyncPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
            id++,
            TechniqueCooldownsPacket.class,
            TechniqueCooldownsPacket::encode,
            TechniqueCooldownsPacket::decode,
            TechniqueCooldownsPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
    }

    public static void sendTo(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToTrackingAndSelf(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), packet);
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }
}
