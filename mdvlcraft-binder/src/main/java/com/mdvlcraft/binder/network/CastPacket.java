package com.mdvlcraft.binder.network;

import com.mdvlcraft.binder.ability.AbilityActions;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

public record CastPacket(boolean pressed, int slot) {
    public static void encode(CastPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.pressed);
        buf.m_130130_(packet.slot);
    }

    public static CastPacket decode(FriendlyByteBuf buf) {
        return new CastPacket(buf.readBoolean(), buf.m_130242_());
    }

    public static void handle(CastPacket packet, Supplier<Context> context) {
        context.get().enqueueWork(() -> {
            if (!packet.pressed) {
                AbilityActions.release(context.get().getSender());
            } else if (packet.slot >= 0 && packet.slot < 8) {
                AbilityActions.press(context.get().getSender(), packet.slot);
            }
        });
        context.get().setPacketHandled(true);
    }
}
