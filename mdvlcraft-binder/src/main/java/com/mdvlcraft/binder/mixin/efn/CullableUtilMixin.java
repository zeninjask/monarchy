package com.mdvlcraft.binder.mixin.efn;

import com.hm.efn.util.CullableUtil;
import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Nightfall talks to Entity Culling through {@code dev.tr7zw.entityculling.versionless.access.Cullable},
 * which Entity Culling 1.11 moved. With 1.11 installed, Nightfall's fire wind and summoned swords
 * crashed the game (NoClassDefFoundError while ticking). Only treat Entity Culling as present when
 * the interface Nightfall was built against exists.
 */
@Mixin(
    value = {CullableUtil.class},
    remap = false
)
public abstract class CullableUtilMixin {
    private static final String CULLABLE = "dev/tr7zw/entityculling/versionless/access/Cullable.class";

    @Redirect(
        method = {"<clinit>"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraftforge/fml/ModList;isLoaded(Ljava/lang/String;)Z"
        )
    )
    private static boolean mdvlcraft$compatibleEntityCulling(ModList mods, String modId) {
        return mods.isLoaded(modId) && CullableUtil.class.getClassLoader().getResource(CULLABLE) != null;
    }
}
