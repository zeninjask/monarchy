package com.mdvlcraft.binder.client.ponder;

import java.util.HashSet;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Function;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import yesman.epicfight.world.capabilities.item.WeaponTypeReloadListener;
import yesman.epicfight.world.capabilities.item.CapabilityItem.Builder;

public final class WeaponPonderPlugin implements PonderPlugin {
    private static final Set<String> EPIC_FIGHT_PONDER_TYPES = Set.of("sword", "dagger", "spear", "axe", "tachi", "uchigatana", "longsword", "greatsword");
    private static final ResourceLocation STRUCTURE = ResourceLocation.fromNamespaceAndPath("epic_fight_ponder", "epicfight_showcase");

    public String getModId() {
        return "mdvlcraft";
    }

    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        Set<String> covered = new HashSet<>(EPIC_FIGHT_PONDER_TYPES);

        for (Entry<ResourceLocation, Function<Item, Builder>> entry : WeaponTypeReloadListener.entries()) {
            ResourceLocation type = entry.getKey();
            if (covered.add(type.m_135815_())) {
                helper.withKeyFunction(WeaponPonderPlugin::sceneKey)
                    .forComponents(new ResourceLocation[]{type})
                    .addStoryBoard(STRUCTURE, (scene, util) -> WeaponPonderScenes.showcase(scene, util, type));
            }
        }
    }

    private static ResourceLocation sceneKey(ResourceLocation weaponType) {
        return ResourceLocation.fromNamespaceAndPath("epic_fight_ponder", "weapon_" + weaponType.m_135815_());
    }
}
