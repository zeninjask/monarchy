package com.mdvlcraft.binder.mixin.epicfight;

import com.mdvlcraft.binder.client.ForeignKeyMappings;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import yesman.epicfight.client.input.EpicFightKeyMappings;

@Mixin(
    value = {EpicFightKeyMappings.class},
    remap = false
)
public abstract class EpicFightKeyMappingsMixin {
    @Redirect(
        method = {"registerKeys"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraftforge/client/event/RegisterKeyMappingsEvent;register(Lnet/minecraft/client/KeyMapping;)V"
        )
    )
    private static void mdvlcraft$skipSkillEdit(RegisterKeyMappingsEvent event, KeyMapping mapping) {
        if (mapping == EpicFightKeyMappings.SKILL_EDIT) {
            ForeignKeyMappings.unbind(List.of(mapping));
        } else {
            event.register(mapping);
        }
    }
}
