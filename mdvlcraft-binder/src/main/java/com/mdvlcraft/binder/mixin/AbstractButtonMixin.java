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
            AbstractButton button = (AbstractButton)this;
            SlateRecruits.drawButton(graphics, button.m_252754_(), button.m_252907_(), button.m_5711_(), button.m_93694_(), button.m_198029_(), button.f_93623_);
            button.m_280139_(graphics, Minecraft.m_91087_().f_91062_, SlateRecruits.buttonText(button.f_93623_));
            ci.cancel();
        }
    }
}
