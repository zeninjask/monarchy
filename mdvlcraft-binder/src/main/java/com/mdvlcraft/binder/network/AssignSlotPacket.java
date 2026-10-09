package com.mdvlcraft.binder.network;

import com.mdvlcraft.binder.ability.AbilityActions;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent.Context;

public record AssignSlotPacket(int slot, Optional<ResourceLocation> ability) {
    public static void encode(AssignSlotPacket packet, FriendlyByteBuf buf) {
        buf.m_130130_(packet.slot);
        buf.m_236835_(packet.ability, FriendlyByteBuf::m_130085_);
    }

    public static AssignSlotPacket decode(FriendlyByteBuf buf) {
        return new AssignSlotPacket(buf.m_130242_(), buf.m_236860_(FriendlyByteBuf::m_130281_));
    }

    public static void handle(AssignSlotPacket packet, Supplier<Context> context) {
        context.get().enqueueWork(() -> {
            if (packet.slot >= 0 && packet.slot < 8) {
                AbilityActions.assign(context.get().getSender(), packet.slot, packet.ability);
            }
        });
        context.get().setPacketHandled(true);
    }
}
