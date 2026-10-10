package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.network.BinderNetwork;
import com.mdvlcraft.binder.network.RespecPacket;
import com.mdvlcraft.binder.skills.Respec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.puffish.skillsmod.client.data.ClientCategoryData;

/**
 * The menu's Respec page: reset one of your archetypes' trees (its points come back to spend), or change one of
 * your archetypes for another (the new one starts at the old one's level less a quarter). Every choice ends on a
 * confirmation; the server does the work ({@link Respec}) and then opens the tree to spend the points on.
 */
public class RespecScreen extends Screen {
    private enum Step { CHOOSE, RESET, CHANGE_FROM, CHANGE_TO, CONFIRM }

    private static final int GAP = 8;
    private static final int ROW = 24;
    private final Screen parent;
    private Step step = Step.CHOOSE;
    private String from;
    private int fromLevel;
    private String to;
    private List<FormattedCharSequence> lines = List.of();
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;

    public RespecScreen(Screen parent) {
        super(Component.translatable("screen.mdvlcraft.menu.respec"));
        this.parent = parent;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void go(Step step) {
        this.step = step;
        this.rebuildWidgets();
    }

    private record Choice(Component label, String icon, Runnable action, boolean active) {
    }

    @Override
    protected void init() {
        List<ClientCategoryData> picked = CharacterScreen.pickedArchetypes();
        List<Choice> choices = new ArrayList<>();
        Component text;
        int columns = 1;
        int buttonW = 170;
        switch (this.step) {
            case CHOOSE -> {
                text = Component.translatable(picked.isEmpty() ? "screen.mdvlcraft.respec.desc.none" : "screen.mdvlcraft.respec.desc.choose");
                choices.add(new Choice(Component.translatable("screen.mdvlcraft.respec.reset"), null, () -> this.go(Step.RESET), !picked.isEmpty()));
                choices.add(new Choice(Component.translatable("screen.mdvlcraft.respec.change"), null, () -> this.go(Step.CHANGE_FROM), !picked.isEmpty()));
            }
            case RESET, CHANGE_FROM -> {
                boolean reset = this.step == Step.RESET;
                text = Component.translatable(reset ? "screen.mdvlcraft.respec.desc.reset" : "screen.mdvlcraft.respec.desc.change_from");
                for (ClientCategoryData c : picked) {
                    String id = c.getConfig().id().getPath();
                    int level = c.getCurrentLevel();
                    choices.add(new Choice(Component.translatable("screen.mdvlcraft.respec.level", title(id), level), id, () -> {
                        this.from = id;
                        this.fromLevel = level;
                        this.to = null;
                        this.go(reset ? Step.CONFIRM : Step.CHANGE_TO);
                    }, true));
                }
            }
            case CHANGE_TO -> {
                text = Component.translatable("screen.mdvlcraft.respec.desc.change_to", Respec.keptLevel(this.fromLevel), title(this.from), this.fromLevel);
                List<String> taken = picked.stream().map(c -> c.getConfig().id().getPath()).toList();
                for (String id : Respec.ARCHETYPES) {
                    if (!taken.contains(id)) {
                        choices.add(new Choice(Component.literal(title(id)), id, () -> {
                            this.to = id;
                            this.go(Step.CONFIRM);
                        }, true));
                    }
                }
                columns = 3;
                buttonW = 100;
            }
            default -> {
                text = this.to == null
                    ? Component.translatable("screen.mdvlcraft.respec.confirm.reset", title(this.from))
                    : Component.translatable("screen.mdvlcraft.respec.confirm.change", title(this.from), this.fromLevel, title(this.to),
                        Respec.keptLevel(this.fromLevel), title(this.from));
                choices.add(new Choice(Component.translatable("screen.mdvlcraft.respec.confirm"), null, () -> {
                    BinderNetwork.sendToServer(new RespecPacket(this.from, this.to == null ? "" : this.to));
                    Minecraft.getInstance().setScreen(null);  // the server opens the tree once it is done
                }, true));
            }
        }

        int rows = (choices.size() + columns - 1) / columns;
        this.panelW = Math.max(240, columns * buttonW + (columns - 1) * GAP + 40);
        this.lines = this.font.split(text, this.panelW - 32);
        int textH = this.lines.size() * 10;
        this.panelH = 40 + textH + 8 + rows * ROW + 34;
        this.panelX = (this.width - this.panelW) / 2;
        this.panelY = (this.height - this.panelH) / 2;
        int gridW = columns * buttonW + (columns - 1) * GAP;
        for (int i = 0; i < choices.size(); i++) {
            Choice choice = choices.get(i);
            int x = this.panelX + (this.panelW - gridW) / 2 + (i % columns) * (buttonW + GAP);
            int y = this.panelY + 40 + textH + 8 + (i / columns) * ROW;
            AstroGui.Button button = new IconButton(x, y, buttonW, 20, choice.label(), choice.icon(), choice.action());
            button.active = choice.active();
            this.addRenderableWidget(button);
        }
        this.addRenderableWidget(new AstroGui.Button(this.panelX + (this.panelW - 80) / 2, this.panelY + this.panelH - 28, 80, 18,
            Component.translatable("screen.mdvlcraft.menu.back"), this::back));
    }

    private void back() {
        switch (this.step) {
            case CHOOSE -> Minecraft.getInstance().setScreen(this.parent);
            case RESET, CHANGE_FROM -> this.go(Step.CHOOSE);
            case CHANGE_TO -> this.go(Step.CHANGE_FROM);
            case CONFIRM -> this.go(this.to == null ? Step.RESET : Step.CHANGE_TO);
        }
    }

    private static String title(String archetype) {
        return Character.toUpperCase(archetype.charAt(0)) + archetype.substring(1);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (AbilityKeys.MENU.matches(key, scan)) {
            Minecraft.getInstance().setScreen(null);
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        AstroGui.panel(g, this.panelX, this.panelY, this.panelW, this.panelH);
        Component heading = Component.translatable("screen.mdvlcraft.respec.title." + this.step.name().toLowerCase(java.util.Locale.ROOT));
        g.drawString(this.font, "✦ " + heading.getString(), this.panelX + 12, this.panelY + 12, AstroGui.HEADING, false);
        g.fill(this.panelX + 10, this.panelY + 28, this.panelX + this.panelW - 10, this.panelY + 29, AstroGui.GOLD_FAINT);
        for (int i = 0; i < this.lines.size(); i++) {
            g.drawString(this.font, this.lines.get(i), this.panelX + 16, this.panelY + 38 + i * 10, AstroGui.LABEL, false);
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    /** A menu button with the archetype's tab icon at its left. */
    private static class IconButton extends AstroGui.Button {
        private final ResourceLocation icon;

        IconButton(int x, int y, int w, int h, Component label, String archetype, Runnable action) {
            super(x, y, w, h, label, action);
            this.icon = archetype == null ? null
                : ResourceLocation.fromNamespaceAndPath("mdvlcraft", "textures/gui/astro/tab/" + archetype + ".png");
            this.labelOffset = archetype == null ? 0 : 8;
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(g, mouseX, mouseY, partialTick);
            if (this.icon != null) {
                g.blit(this.icon, this.getX() + 3, this.getY() + (this.height - 16) / 2, 0, 0, 16, 16, 16, 16);
            }
        }
    }
}
