package com.mdvlcraft.binder.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RenderTooltipEvent.Color;
import net.puffish.skillsmod.client.gui.SkillsScreen;

final class SlateGui {
    static final int TEXT = -1512206;
    static final int MUTED = -7826784;
    static final int ACCENT = -1944512;
    static final int MANA = -9537281;
    static final int CAST = -998328;
    static final int UNAVAILABLE = -1337978864;
    private static final int TOOLTIP = -133032935;
    private static final int TOOLTIP_BORDER = -12828082;
    private static final int PANEL = -300607458;
    private static final int BORDER = -13486014;
    private static final int TRACK = -14144202;
    private static final int SLOT = -14933977;
    private static final int SLOT_PICKED = -11063250;
    private static final int SLOT_HOVERED = -14144202;
    private static final int SLOT_BORDER = -12959670;
    private static final int SLOT_EMPTY_BORDER = -13881031;

    private SlateGui() {
    }

    static void onTooltipColour(Color event) {
        Screen screen = Minecraft.getInstance().screen;
        if (screen instanceof SkillsScreen || screen instanceof AbilityScreen || screen != null && SlateRecruits.isRecruits(screen)) {
            event.setBackground(-133032935);
            event.setBorderStart(-12828082);
            event.setBorderEnd(-12828082);
        }
    }

    static void panel(GuiGraphics graphics, int x, int y, int width, int height) {
        box(graphics, x, y, width, height, -300607458, -13486014);
    }

    static void panel(GuiGraphics graphics, Font font, Component title, int x, int y, int width, int height) {
        panel(graphics, x, y, width, height);
        graphics.drawString(font, title, x + 10, y + 9, -1512206, false);
        rule(graphics, x + 10, x + width - 10, y + 21);
    }

    static void rule(GuiGraphics graphics, int x0, int x1, int y) {
        graphics.fill(x0, y, x1, y + 1, -13486014);
    }

    static void slot(GuiGraphics graphics, int x, int y, int size, SlateGui.Slot state) {
        int fill = state == SlateGui.Slot.PICKED ? -11063250 : -14933977;

        int border = switch (state) {
            case EMPTY -> -13881031;
            case FILLED -> -12959670;
            case PICKED -> -1944512;
        };
        box(graphics, x, y, size, size, fill, border);
    }

    static void button(GuiGraphics graphics, int x, int y, int width, int height, SlateGui.Button state) {
        switch (state) {
            case NORMAL:
                box(graphics, x, y, width, height, -14933977, -12959670);
                break;
            case HOVERED:
                box(graphics, x, y, width, height, -14144202, -1944512);
                break;
            case INACTIVE:
                box(graphics, x, y, width, height, -300607458, -13881031);
        }
    }

    static void bar(GuiGraphics graphics, int x, int y, int width, int height, float fraction, int colour) {
        graphics.fill(x, y, x + width, y + height, -14144202);
        graphics.fill(x, y, x + Math.round(width * fraction), y + height, colour);
    }

    static void disc(GuiGraphics graphics, int centerX, int centerY, int radius, boolean accent) {
        int ring = accent ? -1944512 : -13486014;

        for (int dy = -radius; dy < radius; dy++) {
            double row = dy + 0.5;
            int outer = (int)Math.round(Math.sqrt(radius * radius - row * row));
            int inner = Math.abs(row) < radius - 1 ? (int)Math.round(Math.sqrt((radius - 1) * (radius - 1) - row * row)) : 0;
            int y = centerY + dy;
            if (inner == 0) {
                graphics.fill(centerX - outer, y, centerX + outer, y + 1, ring);
            } else {
                graphics.fill(centerX - outer, y, centerX - inner, y + 1, ring);
                graphics.fill(centerX - inner, y, centerX + inner, y + 1, -300607458);
                graphics.fill(centerX + inner, y, centerX + outer, y + 1, ring);
            }
        }
    }

    private static void box(GuiGraphics graphics, int x, int y, int width, int height, int fill, int border) {
        int right = x + width;
        int bottom = y + height;
        graphics.fill(x + 2, y, right - 2, y + 1, border);
        graphics.fill(x + 2, bottom - 1, right - 2, bottom, border);
        graphics.fill(x, y + 2, x + 1, bottom - 2, border);
        graphics.fill(right - 1, y + 2, right, bottom - 2, border);
        graphics.fill(x + 1, y + 1, x + 2, y + 2, border);
        graphics.fill(right - 2, y + 1, right - 1, y + 2, border);
        graphics.fill(x + 1, bottom - 2, x + 2, bottom - 1, border);
        graphics.fill(right - 2, bottom - 2, right - 1, bottom - 1, border);
        graphics.fill(x + 2, y + 1, right - 2, y + 2, fill);
        graphics.fill(x + 1, y + 2, right - 1, bottom - 2, fill);
        graphics.fill(x + 2, bottom - 2, right - 2, bottom - 1, fill);
    }

    static enum Button {
        NORMAL,
        HOVERED,
        INACTIVE;
    }

    static enum Slot {
        EMPTY,
        FILLED,
        PICKED;
    }
}
