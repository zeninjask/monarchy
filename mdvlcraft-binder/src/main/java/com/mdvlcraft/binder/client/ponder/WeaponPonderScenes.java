package com.mdvlcraft.binder.client.ponder;

import java.util.List;
import java.util.Map;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.arc.epic_ponder.api.ponder.EpicFightSceneBuilder;
import org.arc.epic_ponder.api.ponder.EpicFightSceneBuilder.EpicFightWorldInstructions;
import org.arc.epic_ponder.client.ponder.EFPPonderPlugin;
import org.arc.epic_ponder.client.ponder.EFPSceneUtils;
import org.arc.epic_ponder.mixin.epicfight.WeaponCapabilityAccessor;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.item.Style;
import yesman.epicfight.world.capabilities.item.WeaponCapability;
import yesman.epicfight.world.capabilities.item.WeaponCategory;
import yesman.epicfight.world.capabilities.item.CapabilityItem.Styles;
import yesman.epicfight.world.capabilities.item.CapabilityItem.WeaponCategories;

final class WeaponPonderScenes {
    private static final int SIZE = 11;
    private static final List<Style> STYLE_ORDER = List.of(Styles.TWO_HAND, Styles.ONE_HAND, Styles.COMMON);

    private WeaponPonderScenes() {
    }

    static void showcase(SceneBuilder scene, SceneBuildingUtil util, ResourceLocation weaponType) {
        ItemStack weapon = representative(weaponType);
        WeaponCategory category = EpicFightCapabilities.getItemStackCapability(weapon).getWeaponCategory();
        if (category != WeaponCategories.BOW && category != WeaponCategories.CROSSBOW) {
            EFPSceneUtils.showcaseStandardWeaponCombo(scene, util, 11, "mdvlcraft:weapon_combo", weapon, style(weapon));
        } else {
            ranged(scene, util, weapon, category == WeaponCategories.CROSSBOW);
        }
    }

    private static ItemStack representative(ResourceLocation weaponType) {
        String path = weaponType.m_135815_();
        return ForgeRegistries.ITEMS.getValues().stream().<ItemStack>map(ItemStack::new).filter(stack -> {
            String type = EFPPonderPlugin.getWeaponPresetId(stack);
            return type != null && ResourceLocation.parse(type).m_135815_().equals(path);
        }).findFirst().orElseThrow(() -> new IllegalStateException("No item uses an Epic Fight weapon type named " + path));
    }

    private static Style style(ItemStack weapon) {
        if (EpicFightCapabilities.getItemStackCapability(weapon) instanceof WeaponCapability capability) {
            Map<Style, List<AnimationAccessor<? extends AttackAnimation>>> motions = ((WeaponCapabilityAccessor)capability).getAutoAttackMotions();
            return STYLE_ORDER.stream().filter(motions::containsKey).findFirst().orElse(Styles.COMMON);
        } else {
            return Styles.COMMON;
        }
    }

    private static void ranged(SceneBuilder scene, SceneBuildingUtil util, ItemStack weapon, boolean crossbow) {
        EpicFightSceneBuilder builder = new EpicFightSceneBuilder(scene);
        EpicFightWorldInstructions world = builder.world();
        EFPSceneUtils.setupStandardScene(builder, 11, "weapon_ranged", "mdvlcraft.ponder.weapon_ranged.title");
        ElementLink<EntityElement> actor = EFPSceneUtils.spawnDummyActorWithItem(builder, 5.5, 1.0, 5.5, 180.0F, weapon);
        EFPSceneUtils.showText(builder, util, "mdvlcraft.ponder.weapon_ranged.text_1", 70, 5, 1, 5);
        builder.idle(20);

        for (int shot = 0; shot < 2; shot++) {
            world.playAnimation(actor, crossbow ? Animations.BIPED_CROSSBOW_AIM : Animations.BIPED_BOW_AIM, 0.0F);
            builder.idle(25);
            world.playAnimation(actor, crossbow ? Animations.BIPED_CROSSBOW_SHOT : Animations.BIPED_BOW_SHOT, 0.0F);
            world.waitForInaction(actor);
            if (crossbow) {
                world.playAnimation(actor, Animations.BIPED_CROSSBOW_RELOAD, 0.0F);
                world.waitForInaction(actor);
            }

            builder.idle(10);
        }

        builder.markAsFinished();
    }
}
