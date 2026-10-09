package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.ability.Ability;
import com.mdvlcraft.binder.ability.SlotSkillAbility;
import com.mdvlcraft.binder.ability.SpellAbility;
import com.mdvlcraft.binder.ability.StanceAbility;
import com.mdvlcraft.binder.ability.StanceElement;
import com.mdvlcraft.binder.ability.Technique;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import io.redspace.ironsspellbooks.util.TooltipsUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

final class AbilityDisplay {
    private AbilityDisplay() {
    }

    static void drawIcon(GuiGraphics graphics, Ability ability, int x, int y) {
        if (ability instanceof StanceAbility) {
            int self = Minecraft.m_91087_().f_91074_.m_19879_();
            graphics.m_280163_(StanceAbility.icon(ClientStances.element(self)), x, y, 0.0F, 0.0F, 16, 16, 16, 16);
            if (ClientStances.active(self).isEmpty()) {
                graphics.m_280509_(x, y, x + 16, y + 16, -1879048192);
            }
        } else {
            graphics.m_280163_(ability.icon(), x, y, 0.0F, 0.0F, 16, 16, 16, 16);
            float cooldown = ability instanceof SpellAbility spell
                ? ClientMagicData.getCooldownPercent(spell.spell())
                : (ability instanceof Technique technique ? ClientTechniqueCooldowns.percent(technique) : 0.0F);
            if (cooldown > 0.0F) {
                int height = Math.round(16.0F * cooldown);
                graphics.m_280509_(x, y + 16 - height, x + 16, y + 16, -1610612736);
            }
        }
    }

    static List<Component> tooltip(Ability ability, int level) {
        Minecraft minecraft = Minecraft.m_91087_();
        List<Component> lines = new ArrayList<>();
        if (ability instanceof SpellAbility spell) {
            lines.addAll(
                TooltipsUtils.formatActiveSpellTooltip(ItemStack.f_41583_, new SpellData(spell.spell(), level), CastSource.SPELLBOOK, minecraft.f_91074_)
            );
            lines.remove(0);
        } else if (ability instanceof Technique technique) {
            lines.add(technique.displayName().m_6881_().m_130940_(ChatFormatting.DARK_RED));
            lines.add(Component.m_237115_("ability.mdvlcraft." + technique.id().m_135815_() + ".desc").m_130940_(ChatFormatting.GRAY));
            lines.add(Component.m_237110_("ui.irons_spellbooks.mana_cost", new Object[]{technique.manaCost(level)}).m_130940_(ChatFormatting.BLUE));
            lines.add(
                (technique.cooldownTicks() == 0
                        ? Component.m_237115_("ability.mdvlcraft.no_cooldown")
                        : Component.m_237110_("ability.mdvlcraft.cooldown", new Object[]{technique.cooldownTicks() / 20}))
                    .m_130940_(ChatFormatting.BLUE)
            );
        } else if (ability instanceof SlotSkillAbility slotSkill) {
            lines.add(slotSkill.displayName().m_6881_().m_130940_(ChatFormatting.GOLD));
            lines.add(Component.m_237115_("ability.mdvlcraft." + slotSkill.id().m_135815_() + ".desc").m_130940_(ChatFormatting.GRAY));
            lines.add(Component.m_237110_("ui.irons_spellbooks.mana_cost", new Object[]{slotSkill.manaCost(level)}).m_130940_(ChatFormatting.BLUE));
        } else if (ability instanceof StanceAbility stance) {
            int self = minecraft.f_91074_.m_19879_();
            StanceElement element = ClientStances.element(self);
            lines.add(stance.displayName().m_6881_().m_130940_(ChatFormatting.GOLD));
            lines.add(Component.m_237115_("ability.mdvlcraft.stance.desc").m_130940_(ChatFormatting.GRAY));
            lines.add(
                Component.m_237110_(
                    ClientStances.active(self).isPresent() ? "stance.mdvlcraft.tooltip_on" : "stance.mdvlcraft.tooltip_off",
                    new Object[]{element.displayName(), element.effect()}
                )
            );
        }

        return lines;
    }
}
