package com.mdvlcraft.binder.monk;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.CapabilityItem.WeaponCategories;

/**
 * What counts as fighting with fists for the Monk. Unarmed: an empty main hand, an item that is no weapon at all (a
 * block, food, a torch: you still punch with it), or a weapon worn as part of the hand (#mdvlcraft:unarmed_weapons,
 * Nightfall's Feral Claws); these get the flat unarmed damage. Fisted: unarmed, or a glove (#mdvlcraft:gloves, or any
 * Epic Fight fist weapon); these get fist mastery.
 */
public final class Fists {
    public static final TagKey<Item> GLOVES = ItemTags.create(ResourceLocation.fromNamespaceAndPath("mdvlcraft", "gloves"));
    public static final TagKey<Item> UNARMED_WEAPONS = ItemTags.create(ResourceLocation.fromNamespaceAndPath("mdvlcraft", "unarmed_weapons"));

    private Fists() {
    }

    public static boolean unarmed(Player player) {
        ItemStack main = player.getMainHandItem();
        return main.isEmpty() || main.is(UNARMED_WEAPONS) || notAWeapon(main);
    }

    /** No Epic Fight weapon capability (Epic Fight then treats it as a fist) and no attack damage of its own. */
    private static boolean notAWeapon(ItemStack stack) {
        return EpicFightCapabilities.getItemStackCapability(stack) == CapabilityItem.EMPTY
            && stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE).isEmpty();
    }

    public static boolean fisted(Player player) {
        ItemStack main = player.getMainHandItem();
        if (unarmed(player) || main.is(GLOVES)) {
            return true;
        }
        // Epic Fight gives every item without a weapon capability (blocks, food...) the empty, fist-category capability
        CapabilityItem capability = EpicFightCapabilities.getItemStackCapability(main);
        return capability != CapabilityItem.EMPTY && capability.getWeaponCategory() == WeaponCategories.FIST;
    }
}
