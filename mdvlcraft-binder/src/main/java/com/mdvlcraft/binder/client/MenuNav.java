package com.mdvlcraft.binder.client;

import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.ScreenEvent;
import com.mdvlcraft.binder.MDVLBinder;

/**
 * Opens other mods' screens from the MDVLCraft menu and gives each one a Back button that returns to the menu
 * page it was opened from. Screens are looked up by name, so a mod that is missing only greys out its button.
 */
public final class MenuNav {
    private static Screen pendingParent;
    private static Screen target;
    private static Screen backTo;

    private MenuNav() {
    }

    public static void open(Screen parent, Supplier<Screen> screen) {
        openWith(parent, () -> {
            Screen s = screen.get();
            if (s != null) {
                Minecraft.getInstance().setScreen(s);
            }
        });
    }

    /** Runs code that opens a screen itself (e.g. the skill trees); the screen it opens gets the Back button. */
    public static void openWith(Screen parent, Runnable opener) {
        pendingParent = parent;
        try {
            opener.run();
        } catch (RuntimeException e) {
            MDVLBinder.LOGGER.warn("MDVLCraft menu could not open a screen", e);
        } finally {
            pendingParent = null;
        }
    }

    public static boolean exists(String className) {
        try {
            Class.forName(className, false, MenuNav.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static Supplier<Screen> make(String className) {
        return () -> construct(className, new Class<?>[0]);
    }

    public static Supplier<Screen> makeWithParent(String className, Screen parent) {
        return () -> construct(className, new Class<?>[]{Screen.class}, parent);
    }

    public static Supplier<Screen> makeWithPlayer(String className) {
        return () -> construct(className, new Class<?>[]{Player.class}, Minecraft.getInstance().player);
    }

    private static Screen construct(String className, Class<?>[] types, Object... args) {
        try {
            return (Screen) Class.forName(className).getConstructor(types).newInstance(args);
        } catch (ReflectiveOperationException | ClassCastException e) {
            MDVLBinder.LOGGER.warn("MDVLCraft menu could not open {}", className, e);
            return null;
        }
    }

    /** Adds the Back button to a screen opened from the menu (also after a resize, which rebuilds its widgets). */
    static void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        if (screen instanceof MenuScreen) {
            target = null;
            backTo = null;
        }
        if (pendingParent != null && !(screen instanceof MenuScreen) && !(screen instanceof CharacterScreen)) {
            target = screen;
            backTo = pendingParent;
        }
        if (screen == target && backTo != null) {
            Screen parent = backTo;
            event.addListener(new AstroGui.Button(6, screen.height - 24, 64, 18, Component.translatable("screen.mdvlcraft.menu.back"),
                () -> Minecraft.getInstance().setScreen(parent)));
        }
    }
}
