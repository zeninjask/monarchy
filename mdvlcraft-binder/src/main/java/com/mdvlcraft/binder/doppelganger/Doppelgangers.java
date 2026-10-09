package com.mdvlcraft.binder.doppelganger;

import com.hm.efn.client.sound.EFNSounds;
import com.hm.efn.registries.EFNMobEffectRegistry;
import com.mdvlcraft.binder.util.Teleports;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener.EventType;

/**
 * The Doppelganger ability. Casting it summons the double (1 mana per second while it is out);
 * casting it again while looking at a creature teleports the caster behind that creature, like
 * Yamato's Trick; sneak-casting dismisses the double.
 */
@EventBusSubscriber(
    modid = "mdvlcraft"
)
public final class Doppelgangers {
    private static final UUID MIRROR_LISTENER = UUID.fromString("2f6d1c84-93b0-4c57-a7a2-58e1f0c3d9a4");
    private static final int UPKEEP_INTERVAL_TICKS = 20;
    private static final int UPKEEP_MANA = 1;
    private static final double TELEPORT_RANGE = 24.0;
    private static final double TELEPORT_BEHIND = 1.0;
    private static final Map<UUID, DoppelgangerEntity> ACTIVE = new HashMap<>();

    private Doppelgangers() {
    }

    public static void press(ServerPlayer player) {
        DoppelgangerEntity clone = active(player);
        if (player.isShiftKeyDown()) {
            if (clone != null) {
                dismiss(player, clone);
            }
        } else if (clone == null) {
            summon(player);
        } else {
            LivingEntity target = lookedAt(player);
            if (target == null) {
                actionBar(player, Component.translatable("ability.mdvlcraft.doppelganger.no_target"));
            } else {
                teleportBehind(player, target);
            }
        }
    }

    @Nullable
    private static DoppelgangerEntity active(ServerPlayer player) {
        DoppelgangerEntity clone = ACTIVE.get(player.getUUID());
        if (clone != null && clone.isRemoved()) {
            ACTIVE.remove(player.getUUID());
            stopMirroring(player);
            return null;
        }
        return clone;
    }

    private static void summon(ServerPlayer player) {
        if (!player.isCreative() && MagicData.getPlayerMagicData(player).getMana() < UPKEEP_MANA) {
            actionBar(player, Component.translatable("ui.irons_spellbooks.cast_error_mana", Component.translatable("ability.mdvlcraft.doppelganger")));
            return;
        }
        DoppelgangerEntity clone = DoppelgangerSetup.DOPPELGANGER.get().create(player.level());
        if (clone == null) {
            return;
        }
        clone.setOwner(player);
        clone.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        clone.setYHeadRot(player.getYHeadRot());
        player.level().addFreshEntity(clone);
        ACTIVE.put(player.getUUID(), clone);
        startMirroring(player, clone);
        effects(player.serverLevel(), clone.position(), EFNSounds.DOPPELGANGER_OPEN.get());
    }

    private static void dismiss(ServerPlayer player, DoppelgangerEntity clone) {
        ACTIVE.remove(player.getUUID());
        stopMirroring(player);
        if (!clone.isRemoved()) {
            effects(player.serverLevel(), clone.position(), EFNSounds.DOPPELGANGER_CLOSE.get());
            clone.discard();
        }
    }

    /** The double repeats every Epic Fight action (attacks, skills, dodges) its owner starts. */
    private static void startMirroring(ServerPlayer player, DoppelgangerEntity clone) {
        ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (patch == null) {
            return;
        }
        patch.getEventListener().addEventListener(EventType.ACTION_EVENT_SERVER, MIRROR_LISTENER, event -> {
            DoppelgangerPatch doublePatch = EpicFightCapabilities.getEntityPatch(clone, DoppelgangerPatch.class);
            if (doublePatch != null && !clone.isRemoved()) {
                LivingEntity target = doublePatch.getTarget();
                if (target != null) {
                    clone.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
                }
                doublePatch.playAnimationSynchronized(event.getAnimation(), 0.0F);
            }
        });
    }

    private static void stopMirroring(ServerPlayer player) {
        ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (patch != null) {
            patch.getEventListener().removeListener(EventType.ACTION_EVENT_SERVER, MIRROR_LISTENER);
        }
    }

    /** The living creature under the crosshair, up to 24 blocks away. */
    @Nullable
    private static LivingEntity lookedAt(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 reach = player.getViewVector(1.0F).scale(TELEPORT_RANGE);
        Vec3 end = eye.add(reach);
        // stop at the first block in the way
        BlockHitResult blockHit = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (blockHit.getType() != HitResult.Type.MISS) {
            end = blockHit.getLocation();
        }
        AABB area = player.getBoundingBox().expandTowards(reach).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
            player, eye, end, area,
            entity -> entity instanceof LivingEntity living && living.isAlive() && !(entity instanceof DoppelgangerEntity) && !entity.isSpectator() && entity.isPickable(),
            eye.distanceToSqr(end)
        );
        return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
    }

    private static void teleportBehind(ServerPlayer player, LivingEntity target) {
        Vec3 spot = Teleports.spotBehind(player, target, TELEPORT_BEHIND);
        if (spot == null) {
            actionBar(player, Component.translatable("ability.mdvlcraft.doppelganger.no_room"));
            return;
        }
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 0.5, player.getZ(), 15, 0.3, 0.3, 0.3, 0.1);
        float yaw = target.getYHeadRot();
        player.teleportTo(level, spot.x, spot.y, spot.z, yaw, player.getXRot());
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, spot.x, spot.y + 0.5, spot.z, 15, 0.3, 0.3, 0.3, 0.1);
        level.playSound(null, spot.x, spot.y, spot.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7F, 1.2F);
        // hang in the air for a moment, like Yamato's Trick
        player.addEffect(new MobEffectInstance(EFNMobEffectRegistry.VERTICALSTOP.get(), 10, 1, false, false, false));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent event) {
        if (event.phase != Phase.END || ACTIVE.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
        boolean upkeep = server.getTickCount() % UPKEEP_INTERVAL_TICKS == 0;
        Iterator<Entry<UUID, DoppelgangerEntity>> entries = ACTIVE.entrySet().iterator();
        while (entries.hasNext()) {
            Entry<UUID, DoppelgangerEntity> entry = entries.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            DoppelgangerEntity clone = entry.getValue();
            if (player == null || clone.isRemoved()) {
                entries.remove();
                if (player != null) {
                    stopMirroring(player);
                }
                if (!clone.isRemoved()) {
                    clone.discard();
                }
            } else if (upkeep && !player.isCreative() && !payUpkeep(player)) {
                entries.remove();
                stopMirroring(player);
                effects(player.serverLevel(), clone.position(), EFNSounds.DOPPELGANGER_CLOSE.get());
                clone.discard();
                actionBar(player, Component.translatable("ability.mdvlcraft.doppelganger.out_of_mana"));
            }
        }
    }

    private static boolean payUpkeep(ServerPlayer player) {
        MagicData magic = MagicData.getPlayerMagicData(player);
        if (magic.getMana() < UPKEEP_MANA) {
            return false;
        }
        magic.setMana(magic.getMana() - UPKEEP_MANA);
        PacketDistributor.sendToPlayer(player, new SyncManaPacket(magic));
        return true;
    }

    @SubscribeEvent
    public static void onLogout(PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DoppelgangerEntity clone = ACTIVE.get(player.getUUID());
            if (clone != null) {
                dismiss(player, clone);
            }
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DoppelgangerEntity clone = ACTIVE.get(player.getUUID());
            if (clone != null) {
                dismiss(player, clone);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        ACTIVE.values().forEach(Entity::discard);
        ACTIVE.clear();
    }

    private static void effects(ServerLevel level, Vec3 at, SoundEvent sound) {
        level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y + 1.0, at.z, 40, 0.4, 0.6, 0.4, 0.05);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y + 1.0, at.z, 20, 0.3, 0.5, 0.3, 0.2);
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static void actionBar(ServerPlayer player, Component message) {
        player.connection.send(new ClientboundSetActionBarTextPacket(message.copy().withStyle(ChatFormatting.RED)));
    }
}
