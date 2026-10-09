package com.mdvlcraft.binder.compat.villagerecruits;

import com.mdvlcraft.binder.config.BinderConfig;
import java.util.List;
import java.util.Set;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Recruits and Village Recruits hand their soldiers buffs of their own: Strength and Resistance at
 * high morale, permanent Strength and Fire Resistance for elites, Speed, Resistance, Regeneration and
 * Health Boost auras from leaders, Speed while travelling. Refuse any beneficial effect those mods
 * apply to a non-player. Debuffs (low morale) and highlight glowing stay, and effects from anything
 * else (potions, spells, beacons) still work on recruits.
 */
@EventBusSubscriber(
    modid = "mdvlcraft"
)
public final class RecruitBuffs {
    private static final List<String> SOURCES = List.of("com.talhanation.recruits.", "com.example.villagerecruits.");
    private static final StackWalker WALKER = StackWalker.getInstance();
    /** LivingEntity.addEffect, by its dev and production (SRG) names. */
    private static final Set<String> ADD_EFFECT = Set.of("addEffect", "m_7292_", "m_147207_");
    /** Elite buffs last 999999 ticks; anything this long on a recruit came from those mods. */
    private static final int PERMANENT_TICKS = 20 * 60 * 60;

    private RecruitBuffs() {
    }

    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        LivingEntity entity = event.getEntity();
        if (BinderConfig.stripRecruitBuffs()
            && !(entity instanceof Player)
            && !entity.level().isClientSide()
            && event.getEffectInstance().getEffect().getCategory() == MobEffectCategory.BENEFICIAL
            && fromRecruitMods()) {
            event.setResult(Result.DENY);
        }
    }

    /** Recruits saved with an elite's permanent buffs lose them when they next load. */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide()
            && BinderConfig.stripRecruitBuffs()
            && event.getEntity() instanceof LivingEntity living
            && !(living instanceof Player)
            && isRecruit(living)) {
            List<MobEffectInstance> permanent = living.getActiveEffects()
                .stream()
                .filter(effect -> effect.getEffect().getCategory() == MobEffectCategory.BENEFICIAL)
                .filter(effect -> effect.isInfiniteDuration() || effect.getDuration() >= PERMANENT_TICKS)
                .toList();
            permanent.forEach(effect -> living.removeEffect(effect.getEffect()));
        }
    }

    private static boolean isRecruit(LivingEntity living) {
        var id = ForgeRegistries.ENTITY_TYPES.getKey(living.getType());
        return id != null && (id.getNamespace().equals("recruits") || id.getNamespace().equals("village_recruits"));
    }

    /**
     * Whether the code that called addEffect belongs to one of the recruit mods. Only the direct
     * caller counts: a recruit drinking a potion goes through PotionItem, which is allowed.
     */
    private static boolean fromRecruitMods() {
        return WALKER.walk(frames -> {
            String caller = null;
            boolean inAddEffect = false;
            for (StackWalker.StackFrame frame : (Iterable<StackWalker.StackFrame>)frames.limit(48)::iterator) {
                if (ADD_EFFECT.contains(frame.getMethodName())) {
                    inAddEffect = true;
                } else if (inAddEffect) {
                    caller = frame.getClassName();
                    break;
                }
            }
            String found = caller;
            return found != null && SOURCES.stream().anyMatch(found::startsWith);
        });
    }
}
