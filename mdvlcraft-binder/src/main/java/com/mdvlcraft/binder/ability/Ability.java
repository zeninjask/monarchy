package com.mdvlcraft.binder.ability;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public sealed interface Ability permits SpellAbility, Technique, SlotSkillAbility, StanceAbility {
    ResourceLocation id();

    Component displayName();

    ResourceLocation icon();

    int manaCost(int var1);

    static Optional<Ability> byId(ResourceLocation id) {
        Optional<Ability> technique = Technique.byId(id).map(t -> (Ability)t);
        if (technique.isPresent()) {
            return technique;
        } else {
            Optional<Ability> slotSkill = SlotSkillAbility.byId(id).map(s -> (Ability)s);
            if (slotSkill.isPresent()) {
                return slotSkill;
            } else {
                Optional<Ability> stance = StanceAbility.byId(id).map(s -> (Ability)s);
                if (stance.isPresent()) {
                    return stance;
                } else {
                    AbstractSpell spell = SpellRegistry.getSpell(id);
                    return spell == SpellRegistry.none() ? Optional.empty() : Optional.of(new SpellAbility(spell));
                }
            }
        }
    }
}
