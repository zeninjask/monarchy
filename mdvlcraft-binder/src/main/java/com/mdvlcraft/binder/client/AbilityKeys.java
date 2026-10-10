package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.MDVLBinder;
import com.mdvlcraft.binder.network.BinderNetwork;
import com.mdvlcraft.binder.network.CastPacket;
import com.mojang.blaze3d.platform.InputConstants.Type;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;

public final class AbilityKeys {
    private static final String CATEGORY = "key.categories.mdvlcraft";
    public static final KeyMapping MENU = key("menu", 77);
    public static final KeyMapping WHEEL = key("ability_wheel", 96);
    public static final KeyMapping CAST = key("cast_ability", 280);
    private static final List<KeyMapping> ALL = List.of(MENU, WHEEL, CAST);
    private static boolean castDown;

    private AbilityKeys() {
    }

    private static KeyMapping key(String name, int defaultKey) {
        return new KeyMapping("key.mdvlcraft." + name, KeyConflictContext.IN_GAME, Type.KEYSYM, defaultKey, "key.categories.mdvlcraft");
    }

    static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        ALL.forEach(event::register);
    }

    static void logConflicts(Minecraft minecraft) {
        for (KeyMapping mine : ALL) {
            Arrays.stream(minecraft.options.keyMappings)
                .filter(other -> other != mine && other.same(mine))
                .forEach(
                    other -> MDVLBinder.LOGGER
                        .warn("Key {} ({}) is also bound to {}", new Object[]{mine.getName(), mine.getKey().getName(), other.getName()})
                );
        }
    }

    static void onClientTick(ClientTickEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.phase == Phase.END && minecraft.player != null) {
            while (MENU.consumeClick()) {
                minecraft.setScreen(new MenuScreen());
            }

            while (WHEEL.consumeClick()) {
                minecraft.setScreen(new WheelScreen());
            }

            boolean tapped = false;

            while (CAST.consumeClick()) {
                tapped = true;
            }

            boolean down = CAST.isDown() && minecraft.screen == null;
            if (tapped && !down && !castDown) {
                BinderNetwork.sendToServer(new CastPacket(true, ClientAbilities.selected()));
                BinderNetwork.sendToServer(new CastPacket(false, ClientAbilities.selected()));
            } else if (down != castDown) {
                castDown = down;
                BinderNetwork.sendToServer(new CastPacket(down, ClientAbilities.selected()));
            }
        }
    }
}
