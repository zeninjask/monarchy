package com.mdvlcraft.binder.mixin;

import com.mdvlcraft.binder.content.CursedFateTerrain;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Level.class})
public abstract class LevelMixin {
    @Inject(
        method = {"setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private void mdvlcraft$keepTerrain(BlockPos pos, BlockState state, int flags, int recursionLeft, CallbackInfoReturnable<Boolean> cir) {
        if (CursedFateTerrain.refuses((Level)(Object)this, pos, state)) {
            cir.setReturnValue(false);
        }
    }
}
