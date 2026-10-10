package com.mdvlcraft.binder.mixin.calio;

import com.mdvlcraft.binder.compat.CalioHolders;
import io.github.edwinmindcraft.calio.common.access.MappedRegistryAccess;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** See {@link CalioHolders}. */
@Pseudo
@Mixin(targets = "io.github.edwinmindcraft.calio.api.network.HolderCodec", remap = false)
public abstract class HolderCodecMixin {
    @Redirect(
        method = "decode",
        at = @At(value = "INVOKE",
            target = "Lio/github/edwinmindcraft/calio/common/access/MappedRegistryAccess;calio$getOrCreateHolderOrThrow(Lnet/minecraft/resources/ResourceKey;)Lnet/minecraft/core/Holder;"),
        require = 0
    )
    private Holder<?> mdvlcraft$lockedHolder(MappedRegistryAccess access, ResourceKey key) {
        return CalioHolders.getOrCreate(access, key);
    }
}
