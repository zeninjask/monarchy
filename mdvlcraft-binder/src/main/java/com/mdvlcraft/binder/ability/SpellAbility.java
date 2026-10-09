package com.mdvlcraft.binder.ability;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public record SpellAbility(AbstractSpell spell) implements Ability {
    @Override
    public ResourceLocation id() {
        return this.spell.getSpellResource();
    }

    @Override
    public Component displayName() {
        return Component.m_237115_(this.spell.getComponentId());
    }

    @Override
    public ResourceLocation icon() {
        return this.spell.getSpellIconResource();
    }

    @Override
    public int manaCost(int level) {
        return this.spell.getManaCost(level);
    }
}
