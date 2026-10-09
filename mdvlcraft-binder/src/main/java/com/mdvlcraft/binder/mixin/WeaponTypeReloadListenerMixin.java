package com.mdvlcraft.binder.mixin;

import net.createmod.ponder.foundation.PonderIndex;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.network.server.SPDatapackSync;
import yesman.epicfight.network.server.SPDatapackSync.Type;
import yesman.epicfight.world.capabilities.item.WeaponTypeReloadListener;

@Mixin(
    value = {WeaponTypeReloadListener.class},
    remap = false
)
public abstract class WeaponTypeReloadListenerMixin {
    @Inject(
        method = {"processServerPacket"},
        at = {@At("RETURN")}
    )
    private static void mdvlcraft$reloadPonder(SPDatapackSync packet, CallbackInfo ci) {
        if (packet.getType() == Type.WEAPON_TYPE) {
            PonderIndex.reload();
        }
    }
}
