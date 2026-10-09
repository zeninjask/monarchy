package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.epicskill.EquipSkillPacket;
import com.mdvlcraft.binder.network.BinderNetwork;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlot;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.skill.CapabilitySkill;

final class CombatSkillsPanel {
    private static final int PAD = 10;
    private static final int SLOT = 20;
    private static final int CELL = 22;
    private static final int COLUMNS = 6;
    static final int WIDTH = 150;
    private static final int ROW = 22;
    private static final List<SkillSlot> SLOTS = Arrays.stream(SkillSlots.values())
        .filter(slot -> slot.category().learnable())
        .map(SkillSlot.class::cast)
        .toList();
    private final Font font;
    private int left;
    private int top;
    @Nullable
    private SkillSlot picked;

    CombatSkillsPanel(Font font) {
        this.font = font;
    }

    void place(int left, int top) {
        this.left = left;
        this.top = top;
    }

    private static CapabilitySkill skills() {
        return EpicFightCapabilities.getLocalPlayerPatch(Minecraft.m_91087_().f_91074_).getSkillCapability();
    }

    private List<Skill> choices() {
        return this.picked == null ? List.of() : skills().listAcquiredSkills().filter(skill -> skill.getCategory() == this.picked.category()).toList();
    }

    private List<FormattedCharSequence> hint() {
        return this.font.m_92923_(Component.m_237115_("screen.mdvlcraft.skills.hint"), 130);
    }

    private int slotY(int index) {
        return this.top + 28 + index * 22;
    }

    private int choicesTop() {
        return this.slotY(SLOTS.size()) + 20;
    }

    private int choiceX(int index) {
        return this.left + 10 + index % 6 * 22;
    }

    private int choiceY(int index) {
        return this.choicesTop() + index / 6 * 22;
    }

    private int hintTop() {
        int rows = Math.max(1, (this.choices().size() + 6 - 1) / 6);
        return this.choicesTop() + rows * 22 + 8;
    }

    int height() {
        return this.hintTop() + this.hint().size() * 10 + 6 - this.top;
    }

    void render(GuiGraphics graphics, int height) {
        SlateGui.panel(graphics, this.font, Component.m_237115_("screen.mdvlcraft.skills"), this.left, this.top, 150, height);
        CapabilitySkill skills = skills();

        for (int i = 0; i < SLOTS.size(); i++) {
            SkillSlot slot = SLOTS.get(i);
            SkillContainer container = skills.getSkillContainerFor(slot);
            Skill skill = container.getSkill();
            int x = this.left + 10;
            int y = this.slotY(i);
            SlateGui.slot(graphics, x, y, 20, slot == this.picked ? SlateGui.Slot.PICKED : (skill != null ? SlateGui.Slot.FILLED : SlateGui.Slot.EMPTY));
            if (skill != null) {
                drawIcon(graphics, skill, x + 2, y + 2);
            }

            graphics.m_280614_(this.font, Component.m_237113_(SkillSlot.ENUM_MANAGER.toTranslated(slot)), x + 20 + 6, y + 1, -7826784, false);
            Component name = skill != null ? Component.m_237115_(skill.getTranslationKey()) : Component.m_237115_("screen.mdvlcraft.skills.empty");
            graphics.m_280614_(this.font, name, x + 20 + 6, y + 11, skill != null ? -1512206 : -7826784, false);
            if (container.onReplaceCooldown()) {
                String seconds = (container.getReplaceCooldown() + 19) / 20 + "s";
                graphics.m_280056_(this.font, seconds, this.left + 150 - 10 - this.font.m_92895_(seconds), y + 1, -1944512, false);
            }
        }

        SlateGui.rule(graphics, this.left + 10, this.left + 150 - 10, this.choicesTop() - 18);
        graphics.m_280614_(
            this.font,
            this.picked == null
                ? Component.m_237115_("screen.mdvlcraft.skills.learned")
                : Component.m_237110_(
                    "screen.mdvlcraft.skills.learned_for", new Object[]{Component.m_237113_(SkillSlot.ENUM_MANAGER.toTranslated(this.picked))}
                ),
            this.left + 10,
            this.choicesTop() - 11,
            -7826784,
            false
        );
        List<Skill> choices = this.choices();
        if (this.picked != null && choices.isEmpty()) {
            graphics.m_280614_(this.font, Component.m_237115_("screen.mdvlcraft.skills.none"), this.left + 10, this.choicesTop() + 5, -7826784, false);
        }

        for (int i = 0; i < choices.size(); i++) {
            Skill skillx = choices.get(i);
            SlateGui.slot(graphics, this.choiceX(i), this.choiceY(i), 20, skills.isEquipping(skillx) ? SlateGui.Slot.PICKED : SlateGui.Slot.FILLED);
            drawIcon(graphics, skillx, this.choiceX(i) + 2, this.choiceY(i) + 2);
        }

        List<FormattedCharSequence> hint = this.hint();

        for (int line = 0; line < hint.size(); line++) {
            graphics.m_280649_(this.font, hint.get(line), this.left + 10, this.hintTop() + line * 10, -7826784, false);
        }
    }

    private static void drawIcon(GuiGraphics graphics, Skill skill, int x, int y) {
        RenderSystem.enableBlend();
        graphics.m_280411_(skill.getSkillTexture(), x, y, 16, 16, 0.0F, 0.0F, 128, 128, 128, 128);
    }

    Optional<List<FormattedCharSequence>> tooltip(double mouseX, double mouseY) {
        return this.hovered(mouseX, mouseY).map(this::tooltip);
    }

    private List<FormattedCharSequence> tooltip(Skill skill) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.add(Component.m_237115_(skill.getTranslationKey()).m_130940_(ChatFormatting.GOLD).m_7532_());
        lines.addAll(
            this.font
                .m_92923_(
                    Component.m_237110_(skill.getTranslationKey() + ".tooltip", skill.getTooltipArgsOfScreen(new ArrayList()).toArray())
                        .m_130940_(ChatFormatting.GRAY),
                    220
                )
        );
        return lines;
    }

    private Optional<Skill> hovered(double mouseX, double mouseY) {
        for (int i = 0; i < SLOTS.size(); i++) {
            if (inside(mouseX, mouseY, this.left + 10, this.slotY(i))) {
                return Optional.ofNullable(skills().getSkillContainerFor(SLOTS.get(i)).getSkill());
            }
        }

        List<Skill> choices = this.choices();

        for (int ix = 0; ix < choices.size(); ix++) {
            if (inside(mouseX, mouseY, this.choiceX(ix), this.choiceY(ix))) {
                return Optional.of(choices.get(ix));
            }
        }

        return Optional.empty();
    }

    boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int i = 0; i < SLOTS.size(); i++) {
            SkillSlot slot = SLOTS.get(i);
            if (inside(mouseX, mouseY, this.left + 10, this.slotY(i))) {
                if (button == 1) {
                    BinderNetwork.sendToServer(new EquipSkillPacket(slot.universalOrdinal(), Optional.empty()));
                } else {
                    this.picked = slot == this.picked ? null : slot;
                }

                return true;
            }
        }

        List<Skill> choices = this.choices();

        for (int ix = 0; ix < choices.size(); ix++) {
            if (inside(mouseX, mouseY, this.choiceX(ix), this.choiceY(ix))) {
                BinderNetwork.sendToServer(new EquipSkillPacket(this.picked.universalOrdinal(), Optional.of(choices.get(ix).getRegistryName())));
                this.picked = null;
                return true;
            }
        }

        return false;
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + 20 && mouseY >= y && mouseY < y + 20;
    }
}
