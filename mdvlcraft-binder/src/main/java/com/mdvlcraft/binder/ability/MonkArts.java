package com.mdvlcraft.binder.ability;

import com.mdvlcraft.binder.attribute.BinderAttributes;
import com.mdvlcraft.binder.monk.Fists;
import cursedfate.init.CursedfateModMobEffects;
import cursedfate.init.CursedfateModParticleTypes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

/**
 * The Monk's own arts.
 * <ul>
 * <li><b>Focus</b>: the next strike within {@link #FOCUS_TICKS} (a punch, a kick, or one of the Monk's Cursed Fate
 * melee arts) is a guaranteed Black Flash: {@link #BLACK_FLASH_MULTIPLIER} times the damage, a short stun, and Cursed
 * Fate's Black Flash sound and sparks.</li>
 * <li><b>Knockout</b>: a straight punch at the creature in reach. What it hits takes {@link #KNOCKOUT_MULTIPLIER}
 * times the damage and Blackout (T.O Magic 'n Extras: its magic is suppressed) for {@link #BLACKOUT_TICKS}.</li>
 * </ul>
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class MonkArts {
    static final int FOCUS_TICKS = 200;
    static final float BLACK_FLASH_MULTIPLIER = 2.5F;
    static final int KNOCKOUT_WINDOW_TICKS = 12;
    static final float KNOCKOUT_MULTIPLIER = 1.5F;
    static final int BLACKOUT_TICKS = 120;
    private static final double REACH = 4.0;
    private static final ResourceLocation BLACKOUT = ResourceLocation.fromNamespaceAndPath("traveloptics", "blackout");
    private static final ResourceLocation CURSED_FATE_MELEE = ResourceLocation.fromNamespaceAndPath("cursedfate", "normal_damage");
    private static final Map<UUID, Long> FOCUSED = new HashMap<>();
    /** The Monk art (Cursed Fate melee) each player cast last, and until when its hits count as that art. */
    private static final Map<UUID, Art> ARTS = new HashMap<>();

    private record Art(Technique technique, long until) {
    }
    private static final Map<UUID, Knockout> KNOCKOUTS = new HashMap<>();

    private static final class Knockout {
        final long until;
        final int targetId;
        boolean landed;
        boolean forced;

        Knockout(long until, int targetId) {
            this.until = until;
            this.targetId = targetId;
        }
    }

    private MonkArts() {
    }

    /** Runs a Monk art; false when it cannot be used now (nothing is spent then). */
    static boolean cast(ServerPlayer player, Technique technique) {
        return switch (technique) {
            case FOCUS -> focus(player);
            case KNOCKOUT -> knockout(player);
            default -> false;
        };
    }

    /** How hard each Monk art hits, as a multiple of the Monk's punch; and how long after the cast its hits land. */
    private static float artMultiplier(Technique technique) {
        return switch (technique) {
            case HEAVY_BLOW, LEAPING_CRUSH -> 2.0F;
            case UPPERCUT, FOLLOW_UP_PUNCH -> 1.5F;
            case BARRAGE -> 0.35F;  // per punch of the flurry
            default -> 0.0F;
        };
    }

    private static int artWindow(Technique technique) {
        return technique == Technique.BARRAGE || technique == Technique.LEAPING_CRUSH ? 80 : 40;
    }

    /** Called when a Cursed Fate technique was cast; the Monk's arts then hit as hard as the Monk punches. */
    static void noteCast(ServerPlayer player, Technique technique) {
        if (artMultiplier(technique) > 0.0F) {
            ARTS.put(player.getUUID(), new Art(technique, player.level().getGameTime() + artWindow(technique)));
        }
    }

    /** True while the player's last Monk art is landing; Cursed Fate's own technique scaling then stays out of it. */
    public static boolean artActive(ServerPlayer player) {
        Art art = ARTS.get(player.getUUID());
        return art != null && player.level().getGameTime() <= art.until();
    }

    /** One of the Monk's punches: attack damage, plus unarmed damage with bare hands or claws, times fist mastery. */
    private static float punch(ServerPlayer player) {
        double damage = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        if (Fists.unarmed(player)) {
            damage += player.getAttributeValue(BinderAttributes.UNARMED_DAMAGE.get());
        }
        if (Fists.fisted(player)) {
            damage *= 1.0 + player.getAttributeValue(BinderAttributes.FIST_MASTERY.get());
        }
        return (float)damage;
    }

    /** Before Focus (LOW) multiplies it, so a Black Flash Heavy Blow is 2.5 times the art. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onArtHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player
            && event.getSource().getDirectEntity() == player
            && event.getSource().typeHolder().unwrapKey().map(key -> key.location().getNamespace().equals("cursedfate")).orElse(false)
            && artActive(player)) {
            event.setAmount(punch(player) * artMultiplier(ARTS.get(player.getUUID()).technique()));
        }
    }

    private static boolean focus(ServerPlayer player) {
        FOCUSED.put(player.getUUID(), player.level().getGameTime() + FOCUS_TICKS);
        play(player, sound("cursedfate", "blackflash", SoundEvents.BEACON_ACTIVATE), 0.6F, 2.0F);
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(CursedfateModParticleTypes.BLACK_FLASH_SPARK.get(), player.getX(), player.getY() + 1.2, player.getZ(), 8, 0.4, 0.5, 0.4, 0.0);
        }
        actionBar(player, Component.translatable("ability.mdvlcraft.focus.ready").withStyle(ChatFormatting.DARK_RED));
        return true;
    }

    private static boolean knockout(ServerPlayer player) {
        LivingEntity target = lookTarget(player);
        if (target == null) {
            actionBar(player, Component.translatable("ability.mdvlcraft.knockout.no_target").withStyle(ChatFormatting.RED));
            return false;
        }
        KNOCKOUTS.put(player.getUUID(), new Knockout(player.level().getGameTime() + KNOCKOUT_WINDOW_TICKS, target.getId()));
        ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (patch != null && patch.isEpicFightMode()) {
            patch.playAnimationSynchronized(Animations.FIST_AUTO3, 0.0F);  // the punch lands through Epic Fight's attack
        } else {
            land(player, target);  // vanilla mode: no animation to land it, so hit straight away
        }
        play(player, SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 0.8F);
        return true;
    }

    /** Hits the Knockout target directly, when the punch animation missed or could not play. */
    private static void land(ServerPlayer player, LivingEntity target) {
        float damage = (float)player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        target.invulnerableTime = 0;
        target.hurt(player.damageSources().playerAttack(player), damage);  // the Knockout bonus is added in onHurt
    }

    private static LivingEntity lookTarget(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 reach = eye.add(player.getViewVector(1.0F).scale(REACH));
        AABB box = player.getBoundingBox().expandTowards(player.getViewVector(1.0F).scale(REACH)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, reach, box,
            e -> e instanceof LivingEntity living && living.isAlive() && !e.isSpectator() && e != player, REACH * REACH);
        return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
    }

    private static boolean isStrike(DamageSource source, ServerPlayer player) {
        if (source.getDirectEntity() != player) {
            return false;
        }
        return source.is(DamageTypes.PLAYER_ATTACK)
            || source.typeHolder().unwrapKey().map(key -> key.location().equals(CURSED_FATE_MELEE)).orElse(false);
    }

    /** After the attribute bonuses (normal priority), so Black Flash and Knockout multiply the whole hit. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || !isStrike(event.getSource(), player)) {
            return;
        }
        LivingEntity target = event.getEntity();
        long now = player.level().getGameTime();
        Knockout knockout = KNOCKOUTS.get(player.getUUID());
        if (knockout != null && (knockout.forced || now <= knockout.until) && event.getSource().is(DamageTypes.PLAYER_ATTACK)) {
            knockout.landed = true;
            event.setAmount(event.getAmount() * KNOCKOUT_MULTIPLIER);
            MobEffect blackout = ForgeRegistries.MOB_EFFECTS.getValue(BLACKOUT);
            if (blackout != null) {
                target.addEffect(new MobEffectInstance(blackout, BLACKOUT_TICKS, 0), player);
            }
            play(target, SoundEvents.ANVIL_LAND, 0.4F, 1.6F);
        }
        Long focused = FOCUSED.get(player.getUUID());
        if (focused != null) {
            FOCUSED.remove(player.getUUID());
            if (now <= focused) {
                blackFlash(player, target, event);
            }
        }
    }

    private static void blackFlash(ServerPlayer player, LivingEntity target, LivingHurtEvent event) {
        event.setAmount(event.getAmount() * BLACK_FLASH_MULTIPLIER);
        target.addEffect(new MobEffectInstance(CursedfateModMobEffects.MOVEMENT_STUN.get(), 23, 1, false, false), player);
        play(target, sound("cursedfate", "yutablackflash", SoundEvents.GENERIC_EXPLODE), 1.2F, 1.0F);
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(CursedfateModParticleTypes.BLACK_FLASH_SPARK.get(), target.getX(), target.getY() + target.getBbHeight() / 2.0, target.getZ(),
                24, 0.5, 0.5, 0.5, 0.2);
            MinecraftServer server = level.getServer();
            server.getCommands().performPrefixedCommand(player.createCommandSourceStack().withSuppressedOutput().withPermission(4),
                "photon fx photon:blackflashfinalspark entity @s 0 1.6 0 0 0 0 1 1 1 0 true true look");
        }
        actionBar(player, Component.translatable("ability.mdvlcraft.focus.black_flash").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent event) {
        if (event.phase != Phase.END) {
            return;
        }
        MinecraftServer server = event.getServer();
        for (Entry<UUID, Knockout> entry : List.copyOf(KNOCKOUTS.entrySet())) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            Knockout knockout = entry.getValue();
            if (player == null) {
                KNOCKOUTS.remove(entry.getKey());
            } else if (player.level().getGameTime() > knockout.until) {
                if (!knockout.landed) {
                    // the punch missed (the target stepped aside or the swing was cancelled): land it if the target is still in reach
                    Entity target = player.level().getEntity(knockout.targetId);
                    if (target instanceof LivingEntity living && living.isAlive() && living.distanceTo(player) <= REACH + 1.0) {
                        knockout.forced = true;
                        land(player, living);
                    }
                }
                KNOCKOUTS.remove(entry.getKey());
            }
        }
        FOCUSED.entrySet().removeIf(e -> server.getPlayerList().getPlayer(e.getKey()) == null);
    }

    @SubscribeEvent
    public static void onLogout(PlayerLoggedOutEvent event) {
        FOCUSED.remove(event.getEntity().getUUID());
        KNOCKOUTS.remove(event.getEntity().getUUID());
        ARTS.remove(event.getEntity().getUUID());
    }

    private static SoundEvent sound(String namespace, String path, SoundEvent fallback) {
        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.fromNamespaceAndPath(namespace, path));
        return sound != null ? sound : fallback;
    }

    private static void play(Entity at, SoundEvent sound, float volume, float pitch) {
        at.level().playSound(null, at.getX(), at.getY(), at.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    private static void actionBar(ServerPlayer player, Component message) {
        player.connection.send(new ClientboundSetActionBarTextPacket(message));
    }
}
