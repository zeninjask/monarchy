package com.mdvlcraft.binder.ability;

import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public enum StanceAbility implements Ability {
    INSTANCE;

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("mdvlcraft", "stance");

    public static Optional<StanceAbility> byId(ResourceLocation id) {
        return ID.equals(id) ? Optional.of(INSTANCE) : Optional.empty();
    }

    public static ResourceLocation icon(StanceElement element) {
        return ResourceLocation.fromNamespaceAndPath("mdvlcraft", "textures/gui/stance_" + element.name().toLowerCase() + ".png");
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public Component displayName() {
        return Component.m_237115_("ability.mdvlcraft.stance");
    }

    @Override
    public ResourceLocation icon() {
        return ResourceLocation.fromNamespaceAndPath("mdvlcraft", "textures/gui/stance.png");
    }

    @Override
    public int manaCost(int level) {
        return 0;
    }
}
