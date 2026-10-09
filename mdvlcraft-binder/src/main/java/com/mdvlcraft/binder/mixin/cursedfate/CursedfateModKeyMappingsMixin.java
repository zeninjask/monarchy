package com.mdvlcraft.binder.mixin.cursedfate;

import com.mdvlcraft.binder.client.ForeignKeyMappings;
import cursedfate.init.CursedfateModKeyMappings;
import java.util.List;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
    value = {CursedfateModKeyMappings.class},
    remap = false
)
public abstract class CursedfateModKeyMappingsMixin {
    @Inject(
        method = {"registerKeyMappings"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private static void mdvlcraft$dropKeys(RegisterKeyMappingsEvent event, CallbackInfo ci) {
        ForeignKeyMappings.unbind(
            List.of(
                CursedfateModKeyMappings.COMBAT_MODE,
                CursedfateModKeyMappings.PRESET_SWICH,
                CursedfateModKeyMappings.ABILITY_1,
                CursedfateModKeyMappings.ABILITY_2,
                CursedfateModKeyMappings.ABILITY_3,
                CursedfateModKeyMappings.ABILITY_4,
                CursedfateModKeyMappings.ABILITY_5,
                CursedfateModKeyMappings.ABILITY_6,
                CursedfateModKeyMappings.ABILITY_7,
                CursedfateModKeyMappings.MENU,
                CursedfateModKeyMappings.OUTPUT,
                CursedfateModKeyMappings.ABILITY_USE_FOR_COMBAT_TYPE_2,
                CursedfateModKeyMappings.QUICK_ABILITY_1,
                CursedfateModKeyMappings.QUICK_ABILITY_2,
                CursedfateModKeyMappings.QUICK_ABILITY_3,
                CursedfateModKeyMappings.QUICK_ABILITY_4
            )
        );
        ci.cancel();
    }
}
