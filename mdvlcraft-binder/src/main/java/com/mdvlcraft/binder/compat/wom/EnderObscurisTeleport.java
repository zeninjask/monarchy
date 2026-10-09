package com.mdvlcraft.binder.compat.wom;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import reascer.wom.gameasset.WOMSkills;
import reascer.wom.skill.WOMSkillDataKeys;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

/**
 * Replacement for Weapons of Miracles' Ender Obscuris teleport events. WoM looks for a free block
 * behind the target by stepping along a line with no limit, treating anything that is not air
 * (water, slabs, path blocks, farmland, the void) as blocked, so a target standing on or in one of
 * those sends the server walking and generating chunks until it hangs.
 */
public final class EnderObscurisTeleport {
    private static final double BEHIND = 2.0;
    private static final double STEP = 0.5;

    private EnderObscurisTeleport() {
    }

    /** The player skill: teleport behind the last creature the player hit. */
    public static void player(LivingEntityPatch<?> patch) {
        if (patch.isLogicalClient() || !(patch instanceof ServerPlayerPatch playerPatch) || !(patch.getOriginal() instanceof ServerPlayer player)) {
            return;
        }
        playerPatch.getSkillContainerFor(WOMSkills.ENDEROBSCURIS).ifPresent(container -> {
            Integer targetId = container.getDataManager().getDataValue(WOMSkillDataKeys.TARGET_ID.get());
            if (targetId != null && player.level().getEntity(targetId) instanceof LivingEntity target) {
                Vec3 spot = spotBehind(player, target);
                if (spot != null) {
                    player.teleportTo(player.serverLevel(), spot.x, spot.y, spot.z, target.yHeadRot, player.getXRot());
                    player.setDeltaMovement(target.getDeltaMovement());
                }
            }
        });
        effects(player);
    }

    /** The mob version: teleport behind the creature the mob last hit. */
    public static void mob(LivingEntityPatch<?> patch) {
        if (patch.isLogicalClient()) {
            return;
        }
        LivingEntity entity = patch.getOriginal();
        LivingEntity target = entity.getLastHurtMob();
        if (target != null) {
            Vec3 spot = spotBehind(entity, target);
            if (spot != null) {
                entity.teleportTo(spot.x, spot.y, spot.z);
                entity.setDeltaMovement(target.getDeltaMovement());
                entity.lookAt(EntityAnchorArgument.Anchor.EYES, target.position());
            }
        }
        effects(entity);
    }

    /**
     * The first place along the line from 2 blocks behind the target to 2 blocks in front of it
     * where the entity fits, at the target's height or one block up; null if there is none.
     */
    @Nullable
    private static Vec3 spotBehind(Entity mover, LivingEntity target) {
        Level level = target.level();
        double yaw = Math.toRadians(target.yHeadRot);
        double dx = Math.sin(yaw);
        double dz = -Math.cos(yaw);
        for (double offset = BEHIND; offset >= -BEHIND; offset -= STEP) {
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

    private static void effects(LivingEntity entity) {
        ((ServerLevel)entity.level())
            .sendParticles(ParticleTypes.REVERSE_PORTAL, entity.getX(), entity.getY() + 1.0, entity.getZ(), 60, 0.05, 0.05, 0.05, 0.5);
        entity.level()
            .playSound(
                null,
                entity.xo,
                entity.yo + 1.0,
                entity.zo,
                SoundEvents.ENDERMAN_TELEPORT,
                entity.getSoundSource(),
                2.0F,
                1.0F - (entity.getRandom().nextFloat() - 0.5F) * 0.2F
            );
    }
}
