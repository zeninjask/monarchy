package com.mdvlcraft.binder.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.network.chat.Component;
import net.puffish.skillsmod.client.SkillsClientMod;

/**
 * The MDVLCraft menu (one key): the doors to every screen that used to have its own key. Recruits and Map lead
 * to small pages of their own; every page, and every screen opened from here, has a Back button.
 */
public class MenuScreen extends Screen {
    public enum Page { MAIN, RECRUITS, MAP }

    private static final int BUTTON_W = 150;
    private static final int BUTTON_H = 20;
    private final Page page;
    private final Screen parent;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;

    public MenuScreen() {
        this(Page.MAIN, null);
    }

    public MenuScreen(Page page, Screen parent) {
        super(Component.translatable("screen.mdvlcraft.menu." + page.name().toLowerCase(java.util.Locale.ROOT)));
        this.page = page;
        this.parent = parent;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record Door(String key, Runnable open, boolean available) {
    }

    private List<Door> doors() {
        Minecraft mc = Minecraft.getInstance();
        List<Door> doors = new ArrayList<>();
        switch (this.page) {
            case MAIN -> {
                doors.add(new Door("status", () -> mc.setScreen(new CharacterScreen(this)), true));
                doors.add(new Door("abilities", () -> MenuNav.open(this, AbilityScreen::new), true));
                doors.add(new Door("skills", () -> MenuNav.openWith(this, () -> SkillsClientMod.getInstance().openScreen(Optional.empty())), true));
                doors.add(new Door("respec", () -> mc.setScreen(new RespecScreen(this)), true));
                doors.add(new Door("advancements", () -> MenuNav.open(this,
                    () -> new AdvancementsScreen(mc.player.connection.getAdvancements())), mc.player != null));
                doors.add(new Door("origin", () -> MenuNav.open(this, MenuNav.make("io.github.apace100.origins.screen.ViewOriginScreen")),
                    MenuNav.exists("io.github.apace100.origins.screen.ViewOriginScreen")));
                doors.add(new Door("brewing", () -> MenuNav.open(this,
                    MenuNav.make("dev.obscuria.elixirum.client.screen.alchemy.pages.collection.CollectionScreen")),
                    MenuNav.exists("dev.obscuria.elixirum.client.screen.alchemy.pages.collection.CollectionScreen")));
                doors.add(new Door("recruits", () -> mc.setScreen(new MenuScreen(Page.RECRUITS, this)), true));
                doors.add(new Door("voice_chat", () -> MenuNav.open(this, MenuNav.make("de.maxhenkel.voicechat.gui.VoiceChatScreen")),
                    MenuNav.exists("de.maxhenkel.voicechat.gui.VoiceChatScreen")));
                doors.add(new Door("map", () -> mc.setScreen(new MenuScreen(Page.MAP, this)), true));
            }
            case RECRUITS -> {
                doors.add(new Door("claim_map", () -> MenuNav.open(this,
                    MenuNav.make("com.talhanation.recruits.client.gui.worldmap.WorldMapScreen")),
                    MenuNav.exists("com.talhanation.recruits.client.gui.worldmap.WorldMapScreen")
                        && mc.level != null && mc.level.dimension() == net.minecraft.world.level.Level.OVERWORLD));
                doors.add(new Door("faction", () -> MenuNav.open(this,
                    MenuNav.makeWithPlayer("com.talhanation.recruits.client.gui.faction.FactionMainScreen")),
                    MenuNav.exists("com.talhanation.recruits.client.gui.faction.FactionMainScreen")));
            }
            case MAP -> {
                doors.add(new Door("world_map_settings", () -> MenuNav.openOwnBack(
                    MenuNav.makeWithParent("xaero.map.gui.GuiWorldMapSettings", this)),
                    MenuNav.exists("xaero.map.gui.GuiWorldMapSettings")));
                doors.add(new Door("minimap_settings", () -> MenuNav.openOwnBack(
                    MenuNav.makeWithParent("xaero.common.gui.GuiMinimapMain", this)),
                    MenuNav.exists("xaero.common.gui.GuiMinimapMain")));
            }
        }
        return doors;
    }

    @Override
    protected void init() {
        List<Door> doors = this.doors();
        int columns = this.page == Page.MAIN ? 2 : 1;
        int rows = (doors.size() + columns - 1) / columns;
        this.panelW = columns * BUTTON_W + (columns - 1) * 10 + 40;
        this.panelH = 44 + rows * (BUTTON_H + 6) + 34;
        this.panelX = (this.width - this.panelW) / 2;
        this.panelY = (this.height - this.panelH) / 2;
        for (int i = 0; i < doors.size(); i++) {
            Door door = doors.get(i);
            int x = this.panelX + 20 + (i % columns) * (BUTTON_W + 10);
            if (columns > 1 && i == doors.size() - 1 && i % columns == 0) {
                x = this.panelX + (this.panelW - BUTTON_W) / 2;  // a lone last button sits in the middle
            }
            int y = this.panelY + 40 + (i / columns) * (BUTTON_H + 6);
            String more = door.key().equals("recruits") || door.key().equals("map") ? "  ›" : "";
            AstroGui.Button button = new AstroGui.Button(x, y, BUTTON_W, BUTTON_H,
                Component.literal(Component.translatable("screen.mdvlcraft.menu." + door.key()).getString() + more), door.open());
            button.active = door.available();
            this.addRenderableWidget(button);
        }
        this.addRenderableWidget(new AstroGui.Button(this.panelX + (this.panelW - 80) / 2, this.panelY + this.panelH - 28, 80, 18,
            Component.translatable(this.parent == null ? "screen.mdvlcraft.menu.close" : "screen.mdvlcraft.menu.back"),
            () -> Minecraft.getInstance().setScreen(this.parent)));
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
        g.drawString(this.font, "✦ " + this.title.getString(), this.panelX + 12, this.panelY + 12, AstroGui.HEADING, false);
        g.fill(this.panelX + 10, this.panelY + 28, this.panelX + this.panelW - 10, this.panelY + 29, AstroGui.GOLD_FAINT);
        super.render(g, mouseX, mouseY, partialTick);
    }
}
