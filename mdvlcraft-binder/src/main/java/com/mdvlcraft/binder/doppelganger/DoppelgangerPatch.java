package com.mdvlcraft.binder.doppelganger;

import javax.annotation.Nullable;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.world.capabilities.entitypatch.Factions;
import yesman.epicfight.world.capabilities.entitypatch.HumanoidMobPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;

/**
 * Epic Fight side of the double. Its hits are the owner's: the damage source and the attack itself
 * go through the owner's patch, so the owner's weapon, attributes and skills decide the damage.
 */
public class DoppelgangerPatch extends HumanoidMobPatch<DoppelgangerEntity> {
    /** Without an Epic Fight lock-on, the double goes after whatever the owner hit in the last 5 s. */
    private static final int RECENT_HIT_TICKS = 100;

    public DoppelgangerPatch() {
        super(Factions.NEUTRAL);
    }

    @Override
    public void initAnimator(Animator animator) {
        super.initAnimator(animator);
        this.commonAggresiveMobAnimatorInit(animator);
    }

    @Override
    public void updateMotion(boolean considerInaction) {
        this.commonAggressiveRangedMobUpdateMotion(considerInaction);
    }

    @Override
    public void tick(LivingTickEvent event) {
        super.tick(event);
        PlayerPatch<?> owner = this.getOriginal().ownerPatch();
        if (owner != null && !this.isLogicalClient()) {
            LivingEntity target = this.getOriginal().sentTarget();
            if (target == null) {
                target = owner.getTarget();
            }
            LivingEntity self = owner.getOriginal();
            if (target == null && self.tickCount - self.getLastHurtMobTimestamp() < RECENT_HIT_TICKS) {
                target = self.getLastHurtMob();
            }
            LivingEntity current = this.getTarget();
            LivingEntity wanted = target != null && target.isAlive() && target != owner.getOriginal() ? target : null;
            if (current != wanted) {
                this.setAttakTargetSync(wanted);
            }
        }
    }

    @Override
    public EpicFightDamageSource getDamageSource(AnimationAccessor<? extends StaticAnimation> animation, InteractionHand hand) {
        PlayerPatch<?> owner = this.getOriginal().ownerPatch();
        return owner != null ? owner.getDamageSource(animation, hand) : super.getDamageSource(animation, hand);
    }

    /**
     * The double copies every swing its owner makes, so it would hit whatever stands next to it. It only hurts its
     * target, monsters, and creatures that are after its owner; never animals, villagers, recruits, pets or players.
     */
    private boolean mayHit(Entity target, @Nullable PlayerPatch<?> owner) {
        if (target == this.getTarget()) {
            return true;
        }
        if (target instanceof Enemy) {
            return true;
        }
        return owner != null && target instanceof Mob mob && mob.getTarget() == owner.getOriginal();
    }

    @Override
    public AttackResult attack(EpicFightDamageSource damageSource, Entity target, InteractionHand hand) {
        PlayerPatch<?> owner = this.getOriginal().ownerPatch();
        if (target == this.getOriginal() || owner != null && target == owner.getOriginal() || target instanceof DoppelgangerEntity
            || !this.mayHit(target, owner)) {
            return AttackResult.missed(0.0F);
        }
        return owner != null ? owner.attack(damageSource, target, hand) : super.attack(damageSource, target, hand);
    }
}
