package com.mdvlcraft.binder.network;

import com.mdvlcraft.binder.client.ClientAbilities;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

public record SyncAbilitiesPacket(Map<ResourceLocation, Integer> granted, List<Optional<ResourceLocation>> loadout) {
    public static void encode(SyncAbilitiesPacket packet, FriendlyByteBuf buf) {
        buf.m_236831_(packet.granted, FriendlyByteBuf::m_130085_, FriendlyByteBuf::m_130130_);
        packet.loadout.forEach(slot -> buf.m_236835_(slot, FriendlyByteBuf::m_130085_));
    }

    public static SyncAbilitiesPacket decode(FriendlyByteBuf buf) {
        Map<ResourceLocation, Integer> granted = buf.m_236847_(FriendlyByteBuf::m_130281_, FriendlyByteBuf::m_130242_);
        List<Optional<ResourceLocation>> loadout = new ArrayList<>();

        for (int i = 0; i < 8; i++) {
            loadout.add(buf.m_236860_(FriendlyByteBuf::m_130281_));
        }

        return new SyncAbilitiesPacket(granted, loadout);
    }

    public static void handle(SyncAbilitiesPacket packet, Supplier<Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientAbilities.update(packet.granted, packet.loadout)));
        context.get().setPacketHandled(true);
    }
}
