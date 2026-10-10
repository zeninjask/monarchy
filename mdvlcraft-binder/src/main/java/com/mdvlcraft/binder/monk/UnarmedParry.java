package com.mdvlcraft.binder.monk;

import com.hm.efn.gameasset.EFNSkills;
import com.hm.efn.gameasset.animations.EFNSkillAnimations;
import com.mdvlcraft.binder.MDVLBinder;
import com.mdvlcraft.binder.attribute.BinderAttributes;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.gameasset.EpicFightSkills;
import yesman.epicfight.skill.guard.GuardSkill;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.CapabilityItem.WeaponCategories;
import yesman.epicfight.world.capabilities.item.WeaponCategory;

/**
 * The Monk's parry: the guard skills every class has (Nightfall's Parry - Absolute Deflection, Epic Fight's Guard
 * and Parrying) also work with an empty hand, claws or a glove for players with {@code mdvlcraft:unarmed_parry} (a
 * Monk node). Their guard and parry motions are listed per weapon type and fists had none, so with nothing in hand
 * they could not be raised. The fist motions are only given out to those players; for everyone else they are
 * missing as before. Attributes are synced, so the client agrees.
 */
public final class UnarmedParry {
    private UnarmedParry() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(UnarmedParry::onLoadComplete);
    }

    private static void onLoadComplete(FMLLoadCompleteEvent event) {
        event.enqueueWork(UnarmedParry::addFists);
    }

    private static void addFists() {
        GuardSkill deflection = (GuardSkill)EFNSkills.EFN_PARRY;
        if (deflection == null) {
            MDVLBinder.LOGGER.warn("Nightfall's Parry is not registered; the Monk cannot use it unarmed");
        } else {
            add(deflection, List.of(EFNSkillAnimations.EFN_GUARD_ACTIVE_HIT1, EFNSkillAnimations.EFN_GUARD_ACTIVE_HIT2, EFNSkillAnimations.EFN_GUARD_ACTIVE_HIT3));
        }
        add((GuardSkill)EpicFightSkills.PARRYING, List.of(Animations.SWORD_GUARD_ACTIVE_HIT1, Animations.SWORD_GUARD_ACTIVE_HIT2));
        add((GuardSkill)EpicFightSkills.GUARD, null);
    }

    private static void add(GuardSkill skill, List<?> parryMotions) {
        try {
            motions(skill, "guardMotions").put(WeaponCategories.FIST, (item, player) -> monk(player) ? Animations.SWORD_GUARD_HIT : null);
            motions(skill, "guardBreakMotions").put(WeaponCategories.FIST, (item, player) -> monk(player) ? Animations.BIPED_COMMON_NEUTRALIZED : null);
            if (parryMotions != null) {
                motions(skill, "advancedGuardMotions").put(WeaponCategories.FIST, (item, player) -> monk(player) ? parryMotions : null);
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            MDVLBinder.LOGGER.warn("Could not give {} an unarmed form", skill, e);
        }
    }

    private static boolean monk(PlayerPatch<?> patch) {
        AttributeInstance unarmed = patch.getOriginal().getAttribute(BinderAttributes.UNARMED_PARRY.get());
        return unarmed != null && unarmed.getValue() >= 1.0;
    }

    @SuppressWarnings("unchecked")
    private static Map<WeaponCategory, BiFunction<CapabilityItem, PlayerPatch<?>, ?>> motions(GuardSkill skill, String field)
        throws ReflectiveOperationException {
        Field f = GuardSkill.class.getDeclaredField(field);
        f.setAccessible(true);
        return (Map<WeaponCategory, BiFunction<CapabilityItem, PlayerPatch<?>, ?>>)f.get(skill);
    }
}
