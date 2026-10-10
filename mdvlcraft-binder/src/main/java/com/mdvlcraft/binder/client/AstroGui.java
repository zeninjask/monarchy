package com.mdvlcraft.binder.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Drawing shared by the MDVLCraft screens in the skill trees' Astrologer look: night-sky panels, gold frames. */
public final class AstroGui {
    public static final ResourceLocation SKY = ResourceLocation.fromNamespaceAndPath("mdvlcraft", "textures/gui/skills/astro_sky.png");
    public static final int NAVY = 0xF0121734;
    public static final int GOLD = 0xFFAA8C50;
    public static final int GOLD_FAINT = 0x60AA8C50;
    public static final int HEADING = 0xFFE0C27A;
    public static final int LABEL = 0xFFD6E4FF;
    public static final int VALUE = 0xFFFFFFFF;
    public static final int MUTED = 0xFF8C9AC4;

    private AstroGui() {
    }

    /** A night-sky panel with a gold frame and a faint inner line. */
    public static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, NAVY);
        for (int tx = 0; tx < w; tx += 128) {
            for (int ty = 0; ty < h; ty += 128) {
                g.blit(SKY, x + tx, y + ty, 0, 0, Math.min(128, w - tx), Math.min(128, h - ty), 128, 128);
            }
        }
        frame(g, x, y, w, h, GOLD);
        frame(g, x + 3, y + 3, w - 6, h - 6, GOLD_FAINT);
    }

    public static void frame(GuiGraphics g, int x, int y, int w, int h, int colour) {
        g.fill(x, y, x + w, y + 1, colour);
        g.fill(x, y + h - 1, x + w, y + h, colour);
        g.fill(x, y, x + 1, y + h, colour);
        g.fill(x + w - 1, y, x + w, y + h, colour);
    }

    /** A star-chart button: a navy plate with a gold rim that brightens on hover. */
    public static class Button extends AbstractButton {
        private final Runnable action;

        public Button(int x, int y, int w, int h, Component label, Runnable action) {
            super(x, y, w, h, label);
            this.action = action;
        }

        @Override
        public void onPress() {
            this.action.run();
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            boolean hot = this.isHoveredOrFocused() && this.active;
            g.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, hot ? 0xF0263064 : 0xF0141A3C);
            frame(g, this.getX(), this.getY(), this.width, this.height, hot ? HEADING : GOLD);
            int colour = !this.active ? MUTED : hot ? HEADING : LABEL;
            g.drawCenteredString(Minecraft.getInstance().font, this.getMessage(), this.getX() + this.width / 2,
                this.getY() + (this.height - 8) / 2, colour);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }
    }
}
