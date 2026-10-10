package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.ability.Ability;
import com.mdvlcraft.binder.ability.SpellAbility;
import com.mdvlcraft.binder.network.AssignSlotPacket;
import com.mdvlcraft.binder.network.BinderNetwork;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public final class AbilityScreen extends Screen {
    static final int PAD = 10;
    static final int SLOT = 20;
    static final int CELL = 22;
    private static final int COLUMNS = 8;
    private static final int LIST_ROWS = 3;
    private static final int PANEL_WIDTH = 194;
    private static final int GAP = 12;
    private static final int LIST_OFFSET = 28;
    private static final int SLOT_OFFSET = 112;
    private static final int HINT_OFFSET = 152;
    private Ability picked;
    private CombatSkillsPanel combatSkills;

    public AbilityScreen() {
        super(Component.translatable("screen.mdvlcraft.abilities"));
    }

    protected void init() {
        this.combatSkills = new CombatSkillsPanel(this.font);
    }

    public boolean isPauseScreen() {
        return false;
    }

    private List<Ability> abilities() {
        return new ArrayList<>(ClientAbilities.granted().keySet());
    }

    private int left() {
        return (this.width - 194 - 12 - 150) / 2;
    }

    private int panelHeight() {
        int hintLines = this.font.split(Component.translatable("screen.mdvlcraft.abilities.hint_pick"), 174).size();
        return Math.max(152 + hintLines * 10 + 6, this.combatSkills.height());
    }

    private int top() {
        return (this.height - this.panelHeight()) / 2;
    }

    private int hintTop() {
        return this.top() + 152;
    }

    private void placeCombatSkills() {
        this.combatSkills.place(this.left() + 194 + 12, this.top());
    }

    private int listTop() {
        return this.top() + 28;
    }

    private int abilityX(int index) {
        return this.left() + 10 + index % 8 * 22;
    }

    private int abilityY(int index) {
        return this.listTop() + index / 8 * 22;
    }

    private int slotX(int slot) {
        return this.left() + 10 + slot * 22;
    }

    private int slotY() {
        return this.top() + 112;
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        Component hint = Component.translatable(this.picked == null ? "screen.mdvlcraft.abilities.hint_pick" : "screen.mdvlcraft.abilities.hint_place");
        List<FormattedCharSequence> hintLines = this.font.split(hint, 174);
        this.placeCombatSkills();
        int panelHeight = this.panelHeight();
        int textX = this.left() + 10;
        SlateGui.panel(graphics, this.font, this.title, this.left(), this.top(), 194, panelHeight);
        List<Ability> abilities = this.abilities();
        if (abilities.isEmpty()) {
            List<FormattedCharSequence> none = this.font.split(Component.translatable("screen.mdvlcraft.abilities.none"), 174);

            for (int line = 0; line < none.size(); line++) {
                graphics.drawString(this.font, none.get(line), textX, this.listTop() + 4 + line * 10, -7826784, false);
            }
        }

        for (int i = 0; i < abilities.size(); i++) {
            Ability ability = abilities.get(i);
            int x = this.abilityX(i);
            int y = this.abilityY(i);
            SlateGui.slot(graphics, x, y, 20, ability == this.picked ? SlateGui.Slot.PICKED : SlateGui.Slot.FILLED);
            AbilityDisplay.drawIcon(graphics, ability, x + 2, y + 2);
        }

        graphics.drawString(this.font, Component.translatable("screen.mdvlcraft.abilities.wheel"), textX, this.slotY() - 12, -7826784, false);

        for (int slot = 0; slot < 8; slot++) {
            int x = this.slotX(slot);
            SlateGui.slot(graphics, x, this.slotY(), 20, ClientAbilities.slot(slot).isPresent() ? SlateGui.Slot.FILLED : SlateGui.Slot.EMPTY);
            ClientAbilities.slot(slot).ifPresent(ability -> AbilityDisplay.drawIcon(graphics, ability, x + 2, this.slotY() + 2));
            String number = String.valueOf(slot + 1);
            graphics.drawString(this.font, number, x + (20 - this.font.width(number)) / 2 + 1, this.slotY() + 20 + 3, -7826784, false);
        }

        SlateGui.rule(graphics, textX, this.left() + 194 - 10, this.hintTop() - 6);

        for (int line = 0; line < hintLines.size(); line++) {
            graphics.drawString(this.font, hintLines.get(line), textX, this.hintTop() + line * 10, -7826784, false);
        }

        this.combatSkills.render(graphics, panelHeight);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.hovered(mouseX, mouseY).ifPresent(ability -> {
            List<Component> lines = AbilityDisplay.tooltip(ability, ClientAbilities.level(ability));
            if (ability instanceof SpellAbility spell) {
                SpellPreviews.hover(spell.spell().getSpellResource(), ClientAbilities.level(ability));
                lines.add(1, SpellPreviews.prompt().copy().withStyle(ChatFormatting.DARK_GRAY));
            }

            graphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
        });
        this.combatSkills.tooltip(mouseX, mouseY).ifPresent(lines -> graphics.renderTooltip(this.font, lines, mouseX, mouseY));
    }

    private Optional<Ability> hovered(double mouseX, double mouseY) {
        List<Ability> abilities = this.abilities();

        for (int i = 0; i < abilities.size(); i++) {
            if (inside(mouseX, mouseY, this.abilityX(i), this.abilityY(i))) {
                return Optional.of(abilities.get(i));
            }
        }

        for (int slot = 0; slot < 8; slot++) {
            if (inside(mouseX, mouseY, this.slotX(slot), this.slotY())) {
                return ClientAbilities.slot(slot).filter(ability -> ClientAbilities.level(ability) > 0);
            }
        }

        return Optional.empty();
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + 20 && mouseY >= y && mouseY < y + 20;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        List<Ability> abilities = this.abilities();

        for (int i = 0; i < abilities.size(); i++) {
            if (inside(mouseX, mouseY, this.abilityX(i), this.abilityY(i))) {
                this.picked = abilities.get(i) == this.picked ? null : abilities.get(i);
                return true;
            }
        }

        for (int slot = 0; slot < 8; slot++) {
            if (inside(mouseX, mouseY, this.slotX(slot), this.slotY())) {
                if (button == 1) {
                    BinderNetwork.sendToServer(new AssignSlotPacket(slot, Optional.empty()));
                } else if (this.picked != null) {
                    BinderNetwork.sendToServer(new AssignSlotPacket(slot, Optional.of(this.picked.id())));
                    this.picked = null;
                }

                return true;
            }
        }

        this.placeCombatSkills();
        return this.combatSkills.mouseClicked(mouseX, mouseY, button) ? true : super.mouseClicked(mouseX, mouseY, button);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (AbilityKeys.MENU.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        } else {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }
}
