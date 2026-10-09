package com.mdvlcraft.binder.network;

import com.mdvlcraft.binder.ability.StanceElement;
import com.mdvlcraft.binder.client.ClientStances;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

public record StanceSyncPacket(int entityId, StanceElement element, boolean active) {
    public static void encode(StanceSyncPacket packet, FriendlyByteBuf buf) {
        buf.m_130130_(packet.entityId);
        buf.m_130068_(packet.element);
        buf.writeBoolean(packet.active);
    }

    public static StanceSyncPacket decode(FriendlyByteBuf buf) {
        return new StanceSyncPacket(buf.m_130242_(), (StanceElement)buf.m_130066_(StanceElement.class), buf.readBoolean());
    }

    public static void handle(StanceSyncPacket packet, Supplier<Context> context) {
        context.get()
            .enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientStances.update(packet.entityId, packet.element, packet.active)));
        context.get().setPacketHandled(true);
    }
}
