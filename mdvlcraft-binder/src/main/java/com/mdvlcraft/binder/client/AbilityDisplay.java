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
            int self = Minecraft.getInstance().player.getId();
            graphics.blit(StanceAbility.icon(ClientStances.element(self)), x, y, 0.0F, 0.0F, 16, 16, 16, 16);
            if (ClientStances.active(self).isEmpty()) {
                graphics.fill(x, y, x + 16, y + 16, -1879048192);
            }
        } else {
            graphics.blit(ability.icon(), x, y, 0.0F, 0.0F, 16, 16, 16, 16);
            float cooldown = ability instanceof SpellAbility spell
                ? ClientMagicData.getCooldownPercent(spell.spell())
                : (ability instanceof Technique technique ? ClientTechniqueCooldowns.percent(technique) : 0.0F);
            if (cooldown > 0.0F) {
                int height = Math.round(16.0F * cooldown);
                graphics.fill(x, y + 16 - height, x + 16, y + 16, -1610612736);
            }
        }
    }

    static List<Component> tooltip(Ability ability, int level) {
        Minecraft minecraft = Minecraft.getInstance();
        List<Component> lines = new ArrayList<>();
        if (ability instanceof SpellAbility spell) {
            lines.addAll(
                TooltipsUtils.formatActiveSpellTooltip(ItemStack.EMPTY, new SpellData(spell.spell(), level), CastSource.SPELLBOOK, minecraft.player)
            );
            lines.remove(0);
        } else if (ability instanceof Technique technique) {
            lines.add(technique.displayName().copy().withStyle(ChatFormatting.DARK_RED));
            lines.add(Component.translatable("ability.mdvlcraft." + technique.id().getPath() + ".desc").withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("ui.irons_spellbooks.mana_cost", new Object[]{technique.manaCost(level)}).withStyle(ChatFormatting.BLUE));
            lines.add(
                (technique.cooldownTicks() == 0
                        ? Component.translatable("ability.mdvlcraft.no_cooldown")
                        : Component.translatable("ability.mdvlcraft.cooldown", new Object[]{technique.cooldownTicks() / 20}))
                    .withStyle(ChatFormatting.BLUE)
            );
        } else if (ability instanceof SlotSkillAbility slotSkill) {
            lines.add(slotSkill.displayName().copy().withStyle(ChatFormatting.GOLD));
            lines.add(Component.translatable("ability.mdvlcraft." + slotSkill.id().getPath() + ".desc").withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("ui.irons_spellbooks.mana_cost", new Object[]{slotSkill.manaCost(level)}).withStyle(ChatFormatting.BLUE));
        } else if (ability instanceof StanceAbility stance) {
            int self = minecraft.player.getId();
            StanceElement element = ClientStances.element(self);
            lines.add(stance.displayName().copy().withStyle(ChatFormatting.GOLD));
            lines.add(Component.translatable("ability.mdvlcraft.stance.desc").withStyle(ChatFormatting.GRAY));
            lines.add(
                Component.translatable(
                    ClientStances.active(self).isPresent() ? "stance.mdvlcraft.tooltip_on" : "stance.mdvlcraft.tooltip_off",
                    new Object[]{element.displayName(), element.effect()}
                )
            );
        }

        return lines;
    }
}
