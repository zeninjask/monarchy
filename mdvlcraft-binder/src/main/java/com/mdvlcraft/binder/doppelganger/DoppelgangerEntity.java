package com.mdvlcraft.binder.doppelganger;

import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.entity.ai.attribute.EpicFightAttributes;

/**
 * The Phantom's double: looks like its owner (skin, armour, held items), stays at the owner's side,
 * flanks whatever the owner is fighting and repeats the owner's Epic Fight attacks, dealing the
 * owner's damage. It cannot be hurt and is never saved with the world; {@link Doppelgangers} keeps
 * track of it.
 */
public class DoppelgangerEntity extends PathfinderMob {
    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(DoppelgangerEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final double SIDE_DISTANCE = 1.25;
    private static final double FLANK_DISTANCE = 2.0;
    private static final double CATCH_UP_DISTANCE = 15.0;
    private static final EquipmentSlot[] SLOTS = EquipmentSlot.values();
    private int side = 1;
    private int sideCheck;

    public DoppelgangerEntity(EntityType<? extends DoppelgangerEntity> type, Level level) {
        super(type, level);
        this.setInvulnerable(true);
        // never drop the copied gear
        for (EquipmentSlot slot : SLOTS) {
            this.setDropChance(slot, 0.0F);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.ATTACK_DAMAGE, 1.0)
            .add(Attributes.ATTACK_SPEED, 4.0)
            .add(EpicFightAttributes.WEIGHT.get())
            .add(EpicFightAttributes.ARMOR_NEGATION.get())
            .add(EpicFightAttributes.IMPACT.get())
            .add(EpicFightAttributes.MAX_STRIKES.get())
            .add(EpicFightAttributes.STUN_ARMOR.get())
            .add(EpicFightAttributes.OFFHAND_ATTACK_SPEED.get())
            .add(EpicFightAttributes.OFFHAND_MAX_STRIKES.get())
            .add(EpicFightAttributes.OFFHAND_ARMOR_NEGATION.get())
            .add(EpicFightAttributes.OFFHAND_IMPACT.get());
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER, Optional.empty());
    }

    void setOwner(Player owner) {
        this.entityData.set(OWNER, Optional.of(owner.getUUID()));
        this.side = this.random.nextBoolean() ? 1 : -1;
        this.copyGear(owner);
    }

    public Optional<UUID> ownerId() {
        return this.entityData.get(OWNER);
    }

    @Nullable
    public Player getOwner() {
        return this.ownerId().map(id -> this.level().getPlayerByUUID(id)).orElse(null);
    }

    @Nullable
    PlayerPatch<?> ownerPatch() {
        Player owner = this.getOwner();
        return owner == null ? null : EpicFightCapabilities.getEntityPatch(owner, PlayerPatch.class);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        Player owner = this.getOwner();
        if (owner == null || !owner.isAlive() || owner.level() != this.level()) {
            this.discard();
            return;
        }
        this.copyGear(owner);
        this.follow(owner);
    }

    private void copyGear(Player owner) {
        for (EquipmentSlot slot : SLOTS) {
            ItemStack theirs = owner.getItemBySlot(slot);
            if (!ItemStack.matches(theirs, this.getItemBySlot(slot))) {
                this.setItemSlot(slot, theirs.copy());
            }
        }
    }

    private void follow(Player owner) {
        if (--this.sideCheck <= 0) {
            this.sideCheck = 10;
            if (this.blocked(owner.position().add(this.sideOffset(owner, this.side)))) {
                this.side = -this.side;
            }
        }

        DoppelgangerPatch patch = EpicFightCapabilities.getEntityPatch(this, DoppelgangerPatch.class);
        boolean busy = patch != null && patch.getEntityState().inaction();
        LivingEntity target = this.getTarget();
        if (!busy) {
            Vec3 goal = owner.position().add(this.sideOffset(owner, this.side));
            if (target != null && target.isAlive()) {
                // Flank: get round to the far side of the target from the owner
                Vec3 toTarget = target.position().subtract(owner.position()).multiply(1.0, 0.0, 1.0);
                if (toTarget.lengthSqr() > 1.0E-4) {
                    Vec3 dir = toTarget.normalize();
                    Vec3 across = new Vec3(-dir.z, 0.0, dir.x).scale(this.side * 0.8);
                    goal = target.position().add(dir.scale(FLANK_DISTANCE)).add(across);
                }
            }
            if (this.blocked(goal)) {
                goal = owner.position();
            }
            this.setPos(Mth.lerp(0.6, this.getX(), goal.x), Mth.lerp(0.6, this.getY(), goal.y), Mth.lerp(0.6, this.getZ(), goal.z));
            this.setDeltaMovement(Vec3.ZERO);
        }

        boolean turningLocked = patch != null && patch.getEntityState().turningLocked();
        if (!turningLocked) {
            if (target != null && target.isAlive()) {
                this.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
                this.yBodyRot = this.getYRot();
                this.yHeadRot = this.getYRot();
            } else {
                this.setYRot(owner.getYRot());
                this.setXRot(owner.getXRot());
                this.setYHeadRot(owner.getYHeadRot());
                this.yBodyRot = owner.yBodyRot;
            }
        }

        this.setShiftKeyDown(owner.isShiftKeyDown());
        this.setSprinting(owner.isSprinting());
        this.setNoGravity(busy);
        if (this.distanceTo(owner) > CATCH_UP_DISTANCE) {
            this.teleportTo(owner.getX(), owner.getY(), owner.getZ());
        }
    }

    private Vec3 sideOffset(Player owner, int side) {
        double yaw = Math.toRadians(owner.getYRot()) + side * Math.PI / 2.0;
        return new Vec3(-Math.sin(yaw) * SIDE_DISTANCE, 0.0, Math.cos(yaw) * SIDE_DISTANCE);
    }

    private boolean blocked(Vec3 position) {
        return !this.level().noCollision(this, this.getDimensions(this.getPose()).makeBoundingBox(position));
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target == null || this.ownerId().map(id -> !id.equals(target.getUUID())).orElse(true) && !(target instanceof DoppelgangerEntity)) {
            super.setTarget(target);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean canBeLeashed(Player player) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean shouldShowName() {
        return false;
    }

    @Override
    protected Component getTypeName() {
        Player owner = this.getOwner();
        return owner != null ? owner.getName() : super.getTypeName();
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }
}
