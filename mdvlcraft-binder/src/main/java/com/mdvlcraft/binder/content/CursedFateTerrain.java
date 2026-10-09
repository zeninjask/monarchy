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
        if (!level.f_46443_ && newState.m_60795_()) {
            BlockState old = level.m_8055_(pos);
            if (old.m_60795_()) {
                return false;
            } else {
                ResourceLocation id = ForgeRegistries.BLOCKS.getKey(old.m_60734_());
                return id != null && id.m_135827_().equals("cursedfate")
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
