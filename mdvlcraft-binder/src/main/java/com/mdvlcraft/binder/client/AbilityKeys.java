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
    public static final KeyMapping OPEN_ABILITIES = key("open_abilities", 91);
    public static final KeyMapping WHEEL = key("ability_wheel", 96);
    public static final KeyMapping CAST = key("cast_ability", 280);
    private static final List<KeyMapping> ALL = List.of(OPEN_ABILITIES, WHEEL, CAST);
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
            Arrays.stream(minecraft.f_91066_.f_92059_)
                .filter(other -> other != mine && other.m_90850_(mine))
                .forEach(
                    other -> MDVLBinder.LOGGER
                        .warn("Key {} ({}) is also bound to {}", new Object[]{mine.m_90860_(), mine.getKey().m_84874_(), other.m_90860_()})
                );
        }
    }

    static void onClientTick(ClientTickEvent event) {
        Minecraft minecraft = Minecraft.m_91087_();
        if (event.phase == Phase.END && minecraft.f_91074_ != null) {
            while (OPEN_ABILITIES.m_90859_()) {
                minecraft.m_91152_(new AbilityScreen());
            }

            while (WHEEL.m_90859_()) {
                minecraft.m_91152_(new WheelScreen());
            }

            boolean tapped = false;

            while (CAST.m_90859_()) {
                tapped = true;
            }

            boolean down = CAST.m_90857_() && minecraft.f_91080_ == null;
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
