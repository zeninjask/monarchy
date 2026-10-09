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
        super(Component.translatable("screen.mdvlcraft.wheel"));
    }

    public boolean isPauseScreen() {
        return false;
    }

    private int slotCenterX(int slot) {
        return this.width / 2 + (int)Math.round(56.0 * Math.cos(angle(slot)));
    }

    private int slotCenterY(int slot) {
        return this.height / 2 + (int)Math.round(56.0 * Math.sin(angle(slot)));
    }

    private static double angle(int slot) {
        return (-Math.PI / 2) + slot * 2 * Math.PI / 8.0;
    }

    private int hovered(double mouseX, double mouseY) {
        double dx = mouseX - this.width / 2.0;
        double dy = mouseY - this.height / 2.0;
        if (dx * dx + dy * dy < 196.0) {
            return -1;
        } else {
            double turn = (Math.atan2(dy, dx) + (Math.PI / 2)) / (Math.PI * 2);
            return Math.floorMod((int)Math.round(turn * 8.0), 8);
        }
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int hovered = this.hovered(mouseX, mouseY);
        int shown = hovered >= 0 ? hovered : ClientAbilities.selected();
        SlateGui.disc(graphics, this.width / 2, this.height / 2, 76, false);

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

        SlateGui.disc(graphics, this.width / 2, this.height / 2, 18, true);
        ClientAbilities.slot(shown).ifPresent(ability -> {
            drawAbility(graphics, ability, this.width / 2 - 8, this.height / 2 - 8);
            int textWidth = this.font.width(ability.displayName());
            int bannerTop = this.height / 2 + 56 + 12 + 14;
            SlateGui.panel(graphics, this.width / 2 - textWidth / 2 - 6, bannerTop, textWidth + 12, 16);
            graphics.drawString(this.font, ability.displayName(), this.width / 2 - textWidth / 2, bannerTop + 4, -1512206, false);
        });
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private static void drawAbility(GuiGraphics graphics, Ability ability, int x, int y) {
        AbilityDisplay.drawIcon(graphics, ability, x, y);
        if (ClientAbilities.level(ability) == 0) {
            graphics.fill(x, y, x + 16, y + 16, -1337978864);
        }
    }

    private void choose(double mouseX, double mouseY) {
        int hovered = this.hovered(mouseX, mouseY);
        if (hovered >= 0) {
            ClientAbilities.select(hovered);
        }

        this.onClose();
    }

    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (AbilityKeys.WHEEL.matches(keyCode, scanCode)) {
            double scale = (double)this.minecraft.getWindow().getGuiScaledWidth() / this.minecraft.getWindow().getScreenWidth();
            this.choose(this.minecraft.mouseHandler.xpos() * scale, this.minecraft.mouseHandler.ypos() * scale);
            return true;
        } else {
            return super.keyReleased(keyCode, scanCode, modifiers);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1) {
            this.minecraft.setScreen(new AbilityScreen());
        } else {
            this.choose(mouseX, mouseY);
        }

        return true;
    }
}
