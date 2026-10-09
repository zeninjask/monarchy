package com.mdvlcraft.binder.network;

import com.mdvlcraft.binder.client.ClientTechniqueCooldowns;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

public record TechniqueCooldownsPacket(Map<ResourceLocation, Integer> remaining) {
    public static void encode(TechniqueCooldownsPacket packet, FriendlyByteBuf buf) {
        buf.writeMap(packet.remaining, FriendlyByteBuf::writeResourceLocation, FriendlyByteBuf::writeVarInt);
    }

    public static TechniqueCooldownsPacket decode(FriendlyByteBuf buf) {
        return new TechniqueCooldownsPacket(buf.readMap(FriendlyByteBuf::readResourceLocation, FriendlyByteBuf::readVarInt));
    }

    public static void handle(TechniqueCooldownsPacket packet, Supplier<Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientTechniqueCooldowns.update(packet.remaining)));
        context.get().setPacketHandled(true);
    }
}
