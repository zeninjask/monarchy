package com.mdvlcraft.binder.mixin;

import com.mdvlcraft.binder.client.SlateRecruits;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraftforge.client.gui.widget.ExtendedButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({AbstractButton.class, ExtendedButton.class})
public abstract class AbstractButtonMixin {
    @Inject(
        method = {"renderWidget"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private void mdvlcraft$slateButton(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (SlateRecruits.styled()) {
            AbstractButton button = (AbstractButton)(Object)this;
            SlateRecruits.drawButton(graphics, button.getX(), button.getY(), button.getWidth(), button.getHeight(), button.isHoveredOrFocused(), button.active);
            button.renderString(graphics, Minecraft.getInstance().font, SlateRecruits.buttonText(button.active));
            ci.cancel();
        }
    }
}
