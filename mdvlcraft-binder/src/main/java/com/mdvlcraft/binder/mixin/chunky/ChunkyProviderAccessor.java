package com.mdvlcraft.binder.mixin.chunky;

import org.popcraft.chunky.Chunky;
import org.popcraft.chunky.ChunkyProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(
    value = {ChunkyProvider.class},
    remap = false
)
public interface ChunkyProviderAccessor {
    @Accessor("instance")
    static Chunky mdvlcraft$instance() {
        throw new AssertionError();
    }
}
