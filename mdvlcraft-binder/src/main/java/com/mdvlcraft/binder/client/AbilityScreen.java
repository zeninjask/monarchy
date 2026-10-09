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
        super(Component.m_237115_("screen.mdvlcraft.abilities"));
    }

    protected void m_7856_() {
        this.combatSkills = new CombatSkillsPanel(this.f_96547_);
    }

    public boolean m_7043_() {
        return false;
    }

    private List<Ability> abilities() {
        return new ArrayList<>(ClientAbilities.granted().keySet());
    }

    private int left() {
        return (this.f_96543_ - 194 - 12 - 150) / 2;
    }

    private int panelHeight() {
        int hintLines = this.f_96547_.m_92923_(Component.m_237115_("screen.mdvlcraft.abilities.hint_pick"), 174).size();
        return Math.max(152 + hintLines * 10 + 6, this.combatSkills.height());
    }

    private int top() {
        return (this.f_96544_ - this.panelHeight()) / 2;
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

    public void m_88315_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.m_280273_(graphics);
        Component hint = Component.m_237115_(this.picked == null ? "screen.mdvlcraft.abilities.hint_pick" : "screen.mdvlcraft.abilities.hint_place");
        List<FormattedCharSequence> hintLines = this.f_96547_.m_92923_(hint, 174);
        this.placeCombatSkills();
        int panelHeight = this.panelHeight();
        int textX = this.left() + 10;
        SlateGui.panel(graphics, this.f_96547_, this.f_96539_, this.left(), this.top(), 194, panelHeight);
        List<Ability> abilities = this.abilities();
        if (abilities.isEmpty()) {
            List<FormattedCharSequence> none = this.f_96547_.m_92923_(Component.m_237115_("screen.mdvlcraft.abilities.none"), 174);

            for (int line = 0; line < none.size(); line++) {
                graphics.m_280649_(this.f_96547_, none.get(line), textX, this.listTop() + 4 + line * 10, -7826784, false);
            }
        }

        for (int i = 0; i < abilities.size(); i++) {
            Ability ability = abilities.get(i);
            int x = this.abilityX(i);
            int y = this.abilityY(i);
            SlateGui.slot(graphics, x, y, 20, ability == this.picked ? SlateGui.Slot.PICKED : SlateGui.Slot.FILLED);
            AbilityDisplay.drawIcon(graphics, ability, x + 2, y + 2);
        }

        graphics.m_280614_(this.f_96547_, Component.m_237115_("screen.mdvlcraft.abilities.wheel"), textX, this.slotY() - 12, -7826784, false);

        for (int slot = 0; slot < 8; slot++) {
            int x = this.slotX(slot);
            SlateGui.slot(graphics, x, this.slotY(), 20, ClientAbilities.slot(slot).isPresent() ? SlateGui.Slot.FILLED : SlateGui.Slot.EMPTY);
            ClientAbilities.slot(slot).ifPresent(ability -> AbilityDisplay.drawIcon(graphics, ability, x + 2, this.slotY() + 2));
            String number = String.valueOf(slot + 1);
            graphics.m_280056_(this.f_96547_, number, x + (20 - this.f_96547_.m_92895_(number)) / 2 + 1, this.slotY() + 20 + 3, -7826784, false);
        }

        SlateGui.rule(graphics, textX, this.left() + 194 - 10, this.hintTop() - 6);

        for (int line = 0; line < hintLines.size(); line++) {
            graphics.m_280649_(this.f_96547_, hintLines.get(line), textX, this.hintTop() + line * 10, -7826784, false);
        }

        this.combatSkills.render(graphics, panelHeight);
        super.m_88315_(graphics, mouseX, mouseY, partialTick);
        this.hovered(mouseX, mouseY).ifPresent(ability -> {
            List<Component> lines = AbilityDisplay.tooltip(ability, ClientAbilities.level(ability));
            if (ability instanceof SpellAbility spell) {
                SpellPreviews.hover(spell.spell().getSpellResource(), ClientAbilities.level(ability));
                lines.add(1, SpellPreviews.prompt().m_6881_().m_130940_(ChatFormatting.DARK_GRAY));
            }

            graphics.m_280666_(this.f_96547_, lines, mouseX, mouseY);
        });
        this.combatSkills.tooltip(mouseX, mouseY).ifPresent(lines -> graphics.m_280245_(this.f_96547_, lines, mouseX, mouseY));
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

    public boolean m_6375_(double mouseX, double mouseY, int button) {
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
        return this.combatSkills.mouseClicked(mouseX, mouseY, button) ? true : super.m_6375_(mouseX, mouseY, button);
    }

    public boolean m_7933_(int keyCode, int scanCode, int modifiers) {
        if (AbilityKeys.OPEN_ABILITIES.m_90832_(keyCode, scanCode)) {
            this.m_7379_();
            return true;
        } else {
            return super.m_7933_(keyCode, scanCode, modifiers);
        }
    }
}
