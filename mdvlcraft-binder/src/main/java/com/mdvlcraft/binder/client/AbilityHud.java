package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.ability.Ability;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

final class AbilityHud implements IGuiOverlay {
    static final AbilityHud INSTANCE = new AbilityHud();
    private static final int MIN_WIDTH = 96;
    private static final int MAX_WIDTH = 180;

    private AbilityHud() {
    }

    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.options.hideGui && minecraft.player != null && !ClientAbilities.granted().isEmpty()) {
            Optional<Ability> ability = ClientAbilities.slot(ClientAbilities.selected());
            String number = String.valueOf(ClientAbilities.selected() + 1);
            String name = ability.<String>map(a -> a.displayName().getString()).orElse("");
            int width = Math.max(96, Math.min(180, 28 + minecraft.font.width(name) + 8 + minecraft.font.width(number) + 6));
            int x = screenWidth - width - 4;
            int y = screenHeight - 30;
            SlateGui.panel(graphics, x, y, width, 26);
            SlateGui.slot(graphics, x + 3, y + 3, 20, SlateGui.Slot.FILLED);
            graphics.drawString(minecraft.font, number, x + width - 6 - minecraft.font.width(number), y + 5, -7826784, false);
            ability.ifPresent(selected -> {
                AbilityDisplay.drawIcon(graphics, selected, x + 5, y + 5);
                if (ClientAbilities.level(selected) == 0) {
                    graphics.fill(x + 5, y + 5, x + 21, y + 21, -1337978864);
                }

                int nameWidth = width - 28 - 14 - minecraft.font.width(number);
                graphics.drawString(minecraft.font, minecraft.font.plainSubstrByWidth(name, nameWidth), x + 28, y + 5, -1512206, false);
            });
            double maxMana = minecraft.player.getAttributeValue((Attribute)AttributeRegistry.MAX_MANA.get());
            float mana = maxMana <= 0.0 ? 0.0F : (float)Math.min(1.0, ClientMagicData.getPlayerMana() / maxMana);
            SlateGui.bar(graphics, x + 28, y + 16, width - 34, 3, mana, -9537281);
            if (ClientMagicData.isCasting()) {
                SlateGui.bar(graphics, x + 28, y + 20, width - 34, 2, ClientMagicData.getCastCompletionPercent(), -998328);
            }
        }
    }
}
