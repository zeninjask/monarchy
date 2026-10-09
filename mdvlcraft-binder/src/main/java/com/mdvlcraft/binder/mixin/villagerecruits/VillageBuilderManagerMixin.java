package com.mdvlcraft.binder.mixin.villagerecruits;

import com.example.villagerecruits.faction.VillageFaction;
import com.example.villagerecruits.faction.VillageFactionManager;
import com.mdvlcraft.binder.compat.villagerecruits.AiVillageBuilding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** The builder worker: an AI faction's village gets no builder and finishes no construction. */
@Pseudo
@Mixin(targets = "com.example.villagerecruits.structures.workers.VillageBuilderManager", remap = false)
public abstract class VillageBuilderManagerMixin {
    @Redirect(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lcom/example/villagerecruits/faction/VillageFactionManager;autoBuildsCity(Lcom/example/villagerecruits/faction/VillageFaction;)Z"
        ),
        require = 0
    )
    private static boolean mdvlcraft$aiFactionsDoNotBuild(VillageFaction faction) {
        return VillageFactionManager.autoBuildsCity(faction) && AiVillageBuilding.allowed(faction);
    }
}
