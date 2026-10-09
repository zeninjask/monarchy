package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.ability.Ability;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class WheelScreen extends Screen {
    private static final int RADIUS = 56;
    private static final int SLOT = 24;
    private static final int DEAD_ZONE = 14;

    public WheelScreen() {
        super(Component.m_237115_("screen.mdvlcraft.wheel"));
    }

    public boolean m_7043_() {
        return false;
    }

    private int slotCenterX(int slot) {
        return this.f_96543_ / 2 + (int)Math.round(56.0 * Math.cos(angle(slot)));
    }

    private int slotCenterY(int slot) {
        return this.f_96544_ / 2 + (int)Math.round(56.0 * Math.sin(angle(slot)));
    }

    private static double angle(int slot) {
        return (-Math.PI / 2) + slot * 2 * Math.PI / 8.0;
    }

    private int hovered(double mouseX, double mouseY) {
        double dx = mouseX - this.f_96543_ / 2.0;
        double dy = mouseY - this.f_96544_ / 2.0;
        if (dx * dx + dy * dy < 196.0) {
            return -1;
        } else {
            double turn = (Math.atan2(dy, dx) + (Math.PI / 2)) / (Math.PI * 2);
            return Math.floorMod((int)Math.round(turn * 8.0), 8);
        }
    }

    public void m_88315_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int hovered = this.hovered(mouseX, mouseY);
        int shown = hovered >= 0 ? hovered : ClientAbilities.selected();
        SlateGui.disc(graphics, this.f_96543_ / 2, this.f_96544_ / 2, 76, false);

        for (int slot = 0; slot < 8; slot++) {
            int x = this.slotCenterX(slot) - 12;
            int y = this.slotCenterY(slot) - 12;
            SlateGui.slot(
                graphics,
                x,
                y,
                24,
                slot == shown ? SlateGui.Slot.PICKED : (ClientAbilities.slot(slot).isPresent() ? SlateGui.Slot.FILLED : SlateGui.Slot.EMPTY)
            );
            ClientAbilities.slot(slot).ifPresent(ability -> drawAbility(graphics, ability, x + 4, y + 4));
        }

        SlateGui.disc(graphics, this.f_96543_ / 2, this.f_96544_ / 2, 18, true);
        ClientAbilities.slot(shown).ifPresent(ability -> {
            drawAbility(graphics, ability, this.f_96543_ / 2 - 8, this.f_96544_ / 2 - 8);
            int textWidth = this.f_96547_.m_92852_(ability.displayName());
            int bannerTop = this.f_96544_ / 2 + 56 + 12 + 14;
            SlateGui.panel(graphics, this.f_96543_ / 2 - textWidth / 2 - 6, bannerTop, textWidth + 12, 16);
            graphics.m_280614_(this.f_96547_, ability.displayName(), this.f_96543_ / 2 - textWidth / 2, bannerTop + 4, -1512206, false);
        });
        super.m_88315_(graphics, mouseX, mouseY, partialTick);
    }

    private static void drawAbility(GuiGraphics graphics, Ability ability, int x, int y) {
        AbilityDisplay.drawIcon(graphics, ability, x, y);
        if (ClientAbilities.level(ability) == 0) {
            graphics.m_280509_(x, y, x + 16, y + 16, -1337978864);
        }
    }

    private void choose(double mouseX, double mouseY) {
        int hovered = this.hovered(mouseX, mouseY);
        if (hovered >= 0) {
            ClientAbilities.select(hovered);
        }

        this.m_7379_();
    }

    public boolean m_7920_(int keyCode, int scanCode, int modifiers) {
        if (AbilityKeys.WHEEL.m_90832_(keyCode, scanCode)) {
            double scale = (double)this.f_96541_.m_91268_().m_85445_() / this.f_96541_.m_91268_().m_85443_();
            this.choose(this.f_96541_.f_91067_.m_91589_() * scale, this.f_96541_.f_91067_.m_91594_() * scale);
            return true;
        } else {
            return super.m_7920_(keyCode, scanCode, modifiers);
        }
    }

    public boolean m_6375_(double mouseX, double mouseY, int button) {
        if (button == 1) {
            this.f_96541_.m_91152_(new AbilityScreen());
        } else {
            this.choose(mouseX, mouseY);
        }

        return true;
    }
}
