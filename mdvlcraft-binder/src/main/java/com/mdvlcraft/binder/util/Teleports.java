package com.mdvlcraft.binder.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class Teleports {
    private static final double STEP = 0.5;

    private Teleports() {
    }

    /**
     * The first place along the line from {@code behind} blocks behind the target (by the way it
     * faces) to as far in front of it where the mover fits, at the target's height or one block up;
     * null if there is none. Only loaded chunks inside the world border are considered.
     */
    @Nullable
    public static Vec3 spotBehind(Entity mover, LivingEntity target, double behind) {
        Level level = target.level();
        double yaw = Math.toRadians(target.yHeadRot);
        double dx = Math.sin(yaw);
        double dz = -Math.cos(yaw);
        for (double offset = behind; offset >= -behind; offset -= STEP) {
            for (double rise = 0.0; rise <= 1.0; rise += 1.0) {
                Vec3 spot = new Vec3(target.getX() + offset * dx, target.getY() + rise, target.getZ() + offset * dz);
                BlockPos pos = BlockPos.containing(spot);
                if (!level.isInWorldBounds(pos) || !level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)) {
                    continue;
                }
                AABB box = mover.getDimensions(mover.getPose()).makeBoundingBox(spot);
                if (level.noCollision(mover, box)) {
                    return spot;
                }
            }
        }
        return null;
    }
}
