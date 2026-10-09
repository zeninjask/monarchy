package com.mdvlcraft.binder.mixin.efn;

import com.hm.efn.client.input.keymapping.EFNKeyMappings;
import com.mdvlcraft.binder.client.ForeignKeyMappings;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(
    value = {EFNKeyMappings.class},
    remap = false
)
public abstract class EFNKeyMappingsMixin {
    @Redirect(
        method = {"registerKeys"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraftforge/client/event/RegisterKeyMappingsEvent;register(Lnet/minecraft/client/KeyMapping;)V"
        )
    )
    private static void mdvlcraft$skipArtsKey(RegisterKeyMappingsEvent event, KeyMapping mapping) {
        if (mapping == EFNKeyMappings.EFN_ARTS) {
            ForeignKeyMappings.unbind(List.of(mapping));
        } else {
            event.register(mapping);
        }
    }
}
