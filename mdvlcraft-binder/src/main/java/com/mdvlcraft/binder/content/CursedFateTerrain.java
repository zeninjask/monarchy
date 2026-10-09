package com.mdvlcraft.binder.content;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

public final class CursedFateTerrain {
    private static final StackWalker STACK = StackWalker.getInstance();

    private CursedFateTerrain() {
    }

    public static boolean refuses(Level level, BlockPos pos, BlockState newState) {
        if (!level.isClientSide && newState.isAir()) {
            BlockState old = level.getBlockState(pos);
            if (old.isAir()) {
                return false;
            } else {
                ResourceLocation id = ForgeRegistries.BLOCKS.getKey(old.getBlock());
                return id != null && id.getNamespace().equals("cursedfate")
                    ? false
                    : STACK.walk(frames -> frames.anyMatch(frame -> isCursedFate(frame.getClassName())));
            }
        } else {
            return false;
        }
    }

    private static boolean isCursedFate(String className) {
        return className.startsWith("cursedfate.") || className.startsWith("cursefate.");
    }
}
