package com.mdvlcraft.binder.mixin.epicfight;

import com.mdvlcraft.binder.client.ClientStances;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.animation.property.TrailInfo;
import yesman.epicfight.client.particle.AnimationTrailParticle;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

@Mixin(
    value = {AnimationTrailParticle.class},
    remap = false
)
public abstract class AnimationTrailParticleMixin extends TextureSheetParticle {
    private AnimationTrailParticleMixin(ClientLevel level) {
        super(level, 0.0, 0.0, 0.0);
    }

    @Inject(
        method = {"<init>(Lnet/minecraft/client/multiplayer/ClientLevel;Lyesman/epicfight/world/capabilities/entitypatch/LivingEntityPatch;Lyesman/epicfight/api/animation/Joint;Lyesman/epicfight/api/asset/AssetAccessor;Lyesman/epicfight/api/client/animation/property/TrailInfo;)V"},
        at = {@At("RETURN")}
    )
    private void mdvlcraft$stanceColour(
        ClientLevel level, LivingEntityPatch<?> owner, Joint joint, AssetAccessor<? extends StaticAnimation> animation, TrailInfo trailInfo, CallbackInfo ci
    ) {
        ClientStances.active(((LivingEntity)owner.getOriginal()).getId()).ifPresent(element -> {
            this.rCol = (element.colour >> 16 & 0xFF) / 255.0F;
            this.gCol = (element.colour >> 8 & 0xFF) / 255.0F;
            this.bCol = (element.colour & 0xFF) / 255.0F;
        });
    }
}
