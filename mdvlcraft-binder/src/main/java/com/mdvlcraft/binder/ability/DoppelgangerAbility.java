package com.mdvlcraft.binder.ability;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** The Phantom's Doppelganger; see {@link com.mdvlcraft.binder.doppelganger.Doppelgangers}. */
public enum DoppelgangerAbility implements Ability {
    INSTANCE;

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("mdvlcraft", "doppelganger");
    private static final ResourceLocation ICON = ResourceLocation.fromNamespaceAndPath("mdvlcraft", "textures/gui/icons/ability/doppelganger.png");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public Component displayName() {
        return Component.translatable("ability.mdvlcraft.doppelganger");
    }

    @Override
    public ResourceLocation icon() {
        return ICON;
    }

    /** Mana per second while the double is out. */
    @Override
    public int manaCost(int level) {
        return 1;
    }
}
