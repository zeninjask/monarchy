package com.mdvlcraft.binder.mixin.villagerecruits;

import com.example.villagerecruits.faction.VillageFaction;
import com.example.villagerecruits.faction.VillageFactionManager;
import com.mdvlcraft.binder.compat.villagerecruits.AiVillageBuilding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** The city planner: an AI faction never picks a plot to build. */
@Pseudo
@Mixin(targets = "com.example.villagerecruits.structures.VillageAutoBuilder", remap = false)
public abstract class VillageAutoBuilderMixin {
    @Redirect(
        method = "onServerTick",
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
