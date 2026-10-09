package com.mdvlcraft.binder.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.p1nero.iss_ponder.client.ClientPreviewController;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.createmod.ponder.enums.PonderKeybinds;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;

public final class SpellPreviews {
    private static final int HOLD_TICKS = 12;
    private static final long HOVER_MILLIS = 200L;
    private static final ResourceLocation SKILL_SPELLS = ResourceLocation.fromNamespaceAndPath("mdvlcraft", "spell_previews.json");
    @Nullable
    private static Map<String, SpellPreviews.Spell> skillSpells;
    @Nullable
    private static SpellPreviews.Spell hovered;
    private static long hoveredAt;
    private static int held;

    private SpellPreviews() {
    }

    static void hover(ResourceLocation spell, int level) {
        SpellPreviews.Spell now = new SpellPreviews.Spell(spell, level);
        if (!now.equals(hovered)) {
            held = 0;
        }

        hovered = now;
        hoveredAt = Util.getMillis();
    }

    public static void hoverSkill(ResourceLocation category, String skillId) {
        SpellPreviews.Spell spell = skillSpells().get(category + "/" + skillId);
        if (spell != null) {
            hover(spell.id(), spell.level());
        }
    }

    static Component prompt() {
        return Component.translatable("tooltip.iss_ponder.hold_to_preview", new Object[]{PonderKeybinds.PONDER.message()});
    }

    static void onClientTick(ClientTickEvent event) {
        if (event.phase == Phase.END) {
            boolean hovering = hovered != null && Util.getMillis() - hoveredAt < 200L;
            if (hovering && PonderKeybinds.PONDER.isDown() && !ClientPreviewController.isPending() && !ClientPreviewController.isPreviewOpen()) {
                if (++held >= 12) {
                    ClientPreviewController.request(hovered.id(), hovered.level());
                    hovered = null;
                    held = 0;
                }
            } else {
                held = 0;
            }
        }
    }

    private static Map<String, SpellPreviews.Spell> skillSpells() {
        if (skillSpells == null) {
            Map<String, SpellPreviews.Spell> spells = new HashMap<>();

            try (Reader reader = Minecraft.getInstance().getResourceManager().getResourceOrThrow(SKILL_SPELLS).openAsReader()) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                json.entrySet()
                    .forEach(
                        entry -> {
                            JsonObject spell = ((JsonElement)entry.getValue()).getAsJsonObject();
                            spells.put(
                                (String)entry.getKey(),
                                new SpellPreviews.Spell(ResourceLocation.parse(spell.get("spell").getAsString()), spell.get("level").getAsInt())
                            );
                        }
                    );
            } catch (IOException var6) {
                throw new IllegalStateException("Cannot read " + SKILL_SPELLS, var6);
            }

            skillSpells = spells;
        }

        return skillSpells;
    }

    private record Spell(ResourceLocation id, int level) {
    }
}
