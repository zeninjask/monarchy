package com.mdvlcraft.binder.mixin.villagerecruits;

import com.example.villagerecruits.structures.TowerFootprint;
import com.example.villagerecruits.structures.cityplan.CityPlan;
import com.example.villagerecruits.structures.cityplan.Plot;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Every 5 seconds Village Recruits checks each city plan for plots that overlap the village's tower and, if any
 * do, generates the whole plan again. The new plan can overlap the tower again (and a plot that is already built
 * always does), so some villages were re-planned every 5 seconds forever: a full plan generation, a save and a
 * log line each time (2,000 lines an hour on the live server). Built plots no longer count as overlapping, and
 * unbuilt plots that still overlap after a re-plan are dropped, so a village is re-planned at most once.
 */
@Pseudo
@Mixin(targets = "com.example.villagerecruits.structures.cityplan.CityPlanManager", remap = false)
public abstract class CityPlanManagerMixin {
    @Redirect(
        method = "reconcileWithTower",
        at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 0),
        require = 0
    )
    private static boolean mdvlcraft$ignoreBuiltPlots(List<Object> intruding, Object plot) {
        return plot instanceof Plot p && !p.built && intruding.add(plot);
    }

    @Inject(method = "reconcileWithTower", at = @At("RETURN"), require = 0)
    private static void mdvlcraft$dropStillOverlapping(ServerLevel level, CityPlan plan, CallbackInfoReturnable<CityPlan> cir) {
        CityPlan fixed = cir.getReturnValue();
        if (fixed == null || fixed == plan || fixed.center == null) {
            return;
        }
        try {
            fixed.plots.removeIf(p -> !p.built && p.anchor != null && TowerFootprint.overlapsAnyTowerOfVillage(
                fixed.factionId, fixed.center, p.anchor.getX(), p.anchor.getZ(), Math.max(p.width, p.depth) / 2, 2));
        } catch (Throwable ignored) {
            // same as Village Recruits: leave the plan as it is
        }
    }
}
