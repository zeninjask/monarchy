package com.mdvlcraft.binder.mixin;

import com.mdvlcraft.binder.client.SpellPreviews;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.puffish.skillsmod.api.Skill.State;
import net.puffish.skillsmod.client.config.ClientFrameConfig;
import net.puffish.skillsmod.client.config.skill.ClientSkillConfig;
import net.puffish.skillsmod.client.config.skill.ClientSkillDefinitionConfig;
import net.puffish.skillsmod.client.data.ClientCategoryData;
import net.puffish.skillsmod.client.gui.SkillsScreen;
import net.puffish.skillsmod.client.rendering.TextureBatchedRenderer;
import org.joml.Vector2i;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
    value = {SkillsScreen.class},
    remap = false
)
public abstract class SkillsScreenMixin {
    @Unique
    private static final Vector4fc mdvlcraft$LOCKED = new Vector4f(0.4F, 0.42F, 0.46F, 1.0F);
    @Unique
    private static final Vector4fc mdvlcraft$AVAILABLE = new Vector4f(0.7F, 0.72F, 0.76F, 1.0F);
    @Unique
    private static final Vector4fc mdvlcraft$AFFORDABLE = new Vector4f(0.92F, 0.93F, 0.95F, 1.0F);
    @Unique
    private static final Vector4fc mdvlcraft$EXCLUDED = new Vector4f(0.26F, 0.28F, 0.32F, 1.0F);
    @Unique
    private static final int mdvlcraft$TITLE_TEXT = -1512206;
    @Shadow
    @Final
    @Mutable
    private static ResourceLocation WINDOW_TEXTURE;
    @Shadow
    @Final
    @Mutable
    private static ResourceLocation TABS_TEXTURE;
    @Shadow
    private Optional<ClientCategoryData> optActiveCategoryData;
    @Unique
    @Nullable
    private State mdvlcraft$frameState;
    @Unique
    @Nullable
    private State mdvlcraft$iconState;

    @Inject(
        method = {"<clinit>"},
        at = {@At("TAIL")}
    )
    private static void mdvlcraft$slateTextures(CallbackInfo ci) {
        WINDOW_TEXTURE = ResourceLocation.fromNamespaceAndPath("mdvlcraft", "textures/gui/skills/window.png");
        TABS_TEXTURE = ResourceLocation.fromNamespaceAndPath("mdvlcraft", "textures/gui/skills/tabs.png");
    }

    @Inject(
        method = {"drawFrame"},
        at = {@At("HEAD")}
    )
    private void mdvlcraft$rememberState(
        GuiGraphics context, TextureBatchedRenderer renderer, ClientFrameConfig frame, float sizeScale, int x, int y, State state, CallbackInfo ci
    ) {
        this.mdvlcraft$frameState = state;
    }

    @Inject(
        method = {"drawIcon"},
        at = {@At("HEAD")}
    )
    private void mdvlcraft$takeState(CallbackInfo ci) {
        this.mdvlcraft$iconState = this.mdvlcraft$frameState;
        this.mdvlcraft$frameState = null;
    }

    @ModifyArg(
        method = {"drawIcon"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/puffish/skillsmod/client/rendering/TextureBatchedRenderer;emitTexture(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/resources/ResourceLocation;IIIILorg/joml/Vector4fc;)V"
        ),
        index = 6
    )
    private Vector4fc mdvlcraft$tintIcon(Vector4fc colour) {
        if (this.mdvlcraft$iconState == null) {
            return colour;
        } else {
            return switch (this.mdvlcraft$iconState) {
                case LOCKED -> mdvlcraft$LOCKED;
                case AVAILABLE -> mdvlcraft$AVAILABLE;
                case AFFORDABLE -> mdvlcraft$AFFORDABLE;
                case UNLOCKED -> colour;
                case EXCLUDED -> mdvlcraft$EXCLUDED;
                default -> throw new IncompatibleClassChangeError();
            };
        }
    }

    @Inject(
        method = {"isInsideSkill"},
        at = {@At("RETURN")}
    )
    private void mdvlcraft$reportHover(Vector2i mouse, ClientSkillConfig skill, ClientSkillDefinitionConfig definition, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            this.optActiveCategoryData.ifPresent(category -> SpellPreviews.hoverSkill(category.getConfig().id(), skill.id()));
        }
    }

    @ModifyConstant(
        method = {"drawWindow", "drawWindowWithCategory"},
        constant = {@Constant(
            intValue = -12566464
        )}
    )
    private int mdvlcraft$titleText(int colour) {
        return -1512206;
    }
}
