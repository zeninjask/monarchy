package com.mdvlcraft.binder.mixin.efcompat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import yesman.epicfight.api.utils.math.OpenMatrix4f;

/**
 * Epic Fight Compat (client only) inverts the player's Root joint pose to pose vanilla-model layers.
 * It expects {@code OpenMatrix4f.invert(m, null)} to return null for a matrix that cannot be
 * inverted, but Epic Fight then writes into the null destination and throws. A Root scaled to zero
 * (Antitheus's skills shrink the player out of sight) crashed the game while rendering the player.
 * Hand it null for a singular matrix, which it already treats as "nothing to pose".
 */
@Pseudo
@Mixin(
    targets = {"com.efcompat.client.ArmatureBinder"},
    remap = false
)
public abstract class ArmatureBinderMixin {
    @Redirect(
        method = {"bind"},
        at = @At(
            value = "INVOKE",
            target = "Lyesman/epicfight/api/utils/math/OpenMatrix4f;invert(Lyesman/epicfight/api/utils/math/OpenMatrix4f;Lyesman/epicfight/api/utils/math/OpenMatrix4f;)Lyesman/epicfight/api/utils/math/OpenMatrix4f;"
        ),
        require = 0
    )
    private static OpenMatrix4f mdvlcraft$invertOrNull(OpenMatrix4f src, OpenMatrix4f dest) {
        return src.determinant() == 0.0F ? null : OpenMatrix4f.invert(src, dest);
    }
}
