package com.mdvlcraft.binder.client;

import java.nio.file.Path;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.Pack.Position;
import net.minecraftforge.client.event.ScreenEvent.Render.Post;
import net.minecraftforge.client.event.ScreenEvent.Render.Pre;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.fml.ModList;

public final class SlateRecruits {
    private static final int VANILLA_LABEL = 4210752;
    private static final int SLATE_LABEL = 15265010;
    private static final Map<Integer, Integer> FILLS = Map.of(657930, 921620, 3947580, 1843239, 5263440, 2237998, 6579300, 2896185, 9145227, 1843239);
    private static boolean recruitsScreen;

    private SlateRecruits() {
    }

    static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() == PackType.CLIENT_RESOURCES) {
            Path root = ModList.get().getModFileById("mdvlcraft").getFile().findResource(new String[]{"resourcepacks", "slate"});
            Pack pack = Pack.m_245429_(
                "mdvlcraft:slate",
                Component.m_237113_("MDVLCraft Slate UI"),
                true,
                id -> new PathPackResources(id, root, true),
                PackType.CLIENT_RESOURCES,
                Position.TOP,
                PackSource.f_10528_
            );
            if (pack == null) {
                throw new IllegalStateException("MDVLCraft Slate resource pack is missing its pack.mcmeta");
            } else {
                event.addRepositorySource(consumer -> consumer.accept(pack));
            }
        }
    }

    static void onRenderPre(Pre event) {
        recruitsScreen = isRecruits(event.getScreen());
    }

    static void onRenderPost(Post event) {
        recruitsScreen = false;
    }

    static boolean isRecruits(Screen screen) {
        String name = screen.getClass().getName();
        return name.startsWith("com.talhanation.recruits.") || name.startsWith("com.example.villagerecruits.");
    }

    public static boolean styled() {
        return recruitsScreen;
    }

    public static void drawButton(GuiGraphics graphics, int x, int y, int width, int height, boolean hovered, boolean active) {
        SlateGui.button(graphics, x, y, width, height, !active ? SlateGui.Button.INACTIVE : (hovered ? SlateGui.Button.HOVERED : SlateGui.Button.NORMAL));
    }

    public static int buttonText(boolean active) {
        return active ? -1512206 : -7826784;
    }

    public static int fillColour(int colour) {
        if (!recruitsScreen) {
            return colour;
        } else {
            Integer slate = FILLS.get(colour & 16777215);
            return slate == null ? colour : colour & 0xFF000000 | slate;
        }
    }

    public static int textColour(int colour) {
        return recruitsScreen && (colour & 16777215) == 4210752 ? colour & 0xFF000000 | 15265010 : colour;
    }
}
