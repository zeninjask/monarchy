package com.mdvlcraft.binder.mixin.epicfight;

import org.arc.epic_ponder.entity.DummyPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import yesman.epicfight.api.animation.property.AnimationEvent;
import yesman.epicfight.api.animation.property.AnimationParameters;
import yesman.epicfight.api.animation.property.AnimationEvent.Event;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

@Mixin(
    value = {AnimationEvent.class},
    remap = false
)
public abstract class AnimationEventMixin {
    @Redirect(
        method = {"execute", "executeWithNewParams"},
        at = @At(
            value = "INVOKE",
            target = "Lyesman/epicfight/api/animation/property/AnimationEvent$Event;fire(Lyesman/epicfight/world/capabilities/entitypatch/LivingEntityPatch;Lyesman/epicfight/api/asset/AssetAccessor;Lyesman/epicfight/api/animation/property/AnimationParameters;)V"
        )
    )
    private void mdvlcraft$skipPlayerOnlyEventsInPonder(
        Event event, LivingEntityPatch<?> patch, AssetAccessor<? extends StaticAnimation> animation, AnimationParameters params
    ) {
        if (!(patch.getOriginal() instanceof DummyPlayerEntity)) {
            event.fire(patch, animation, params);
        } else {
            try {
                event.fire(patch, animation, params);
            } catch (ClassCastException var6) {
            }
        }
    }
}
