package com.mdvlcraft.binder.mixin.naoyaaddon;

import com.bless.naoyaaddon.network.ToggleProjectionStylePacket;
import java.util.function.Supplier;
import net.minecraftforge.network.NetworkEvent.Context;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
    value = {ToggleProjectionStylePacket.class},
    remap = false
)
public abstract class ToggleProjectionStylePacketMixin {
    @Inject(
        method = {"handler"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private static void mdvlcraft$naobitoOnly(ToggleProjectionStylePacket packet, Supplier<Context> context, CallbackInfo ci) {
        context.get().setPacketHandled(true);
        ci.cancel();
    }
}
