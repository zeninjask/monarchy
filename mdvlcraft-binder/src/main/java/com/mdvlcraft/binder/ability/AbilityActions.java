package com.mdvlcraft.binder.ability;

import com.mdvlcraft.binder.doppelganger.Doppelgangers;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.util.Utils;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class AbilityActions {
    private AbilityActions() {
    }

    public static boolean assign(ServerPlayer player, int slot, Optional<ResourceLocation> abilityId) {
        if (abilityId.isPresent() && abilityId.flatMap(Ability::byId).map(a -> AbilityGrants.level(player, a)).orElse(0) == 0) {
            return false;
        } else {
            Loadout.set(player, slot, abilityId);
            return true;
        }
    }

    public static void press(ServerPlayer player, int slot) {
        Optional<Ability> ability = Loadout.get(player).get(slot).flatMap(Ability::byId);
        if (!ability.isEmpty()) {
            int level = AbilityGrants.level(player, ability.get());
            if (level == 0) {
                player.connection
                    .send(
                        new ClientboundSetActionBarTextPacket(
                            Component.translatable("ability.mdvlcraft.not_granted", new Object[]{ability.get().displayName()}).withStyle(ChatFormatting.RED)
                        )
                    );
            } else {
                if (ability.get() instanceof SpellAbility spellAbility) {
                    castSpell(player, spellAbility.spell(), level);
                } else if (ability.get() instanceof Technique technique) {
                    TechniqueRunner.press(player, technique);
                } else if (ability.get() instanceof SlotSkillAbility slotSkill) {
                    slotSkill.cast(player);
                } else if (ability.get() instanceof StanceAbility) {
                    Stances.press(player);
                } else if (ability.get() instanceof DoppelgangerAbility) {
                    Doppelgangers.press(player);
                }
            }
        }
    }

    public static void release(ServerPlayer player) {
        TechniqueRunner.release(player);
    }

    private static void castSpell(ServerPlayer player, AbstractSpell spell, int level) {
        MagicData magic = MagicData.getPlayerMagicData(player);
        if (magic.isCasting() && !magic.getCastingSpellId().equals(spell.getSpellId())) {
            Utils.serverSideCancelCast(player, magic.getCastType() != CastType.LONG);
        }

        spell.attemptInitiateCast(
            ItemStack.EMPTY, spell.getLevelFor(level, player), player.level(), player, CastSource.SPELLBOOK, true, SpellSelectionManager.MAINHAND
        );
    }
}
