package com.mdvlcraft.binder.mixin.puffish;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.gui.GuiGraphics;
import net.puffish.skillsmod.client.rendering.ConnectionBatchedRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
    value = {ConnectionBatchedRenderer.class},
    remap = false
)
public abstract class ConnectionBatchedRendererMixin {
    private static final float STROKE = 3.0F;
    private static final float FILL = 1.0F;
    private static final float SEGMENT_LENGTH = 6.0F;
    @Shadow
    @Final
    private Int2ObjectMap<?> strokeBatch;
    @Shadow
    @Final
    private Int2ObjectMap<?> fillBatch;

    @Shadow
    private void emitLine(Int2ObjectMap<?> batch, Matrix4f matrix, int color, float startX, float startY, float endX, float endY, float thickness) {
        throw new AssertionError();
    }

    @Inject(
        method = {"emitConnection"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private void mdvlcraft$curve(
        GuiGraphics context, float startX, float startY, float endX, float endY, boolean bidirectional, int fillColor, int strokeColor, CallbackInfo ci
    ) {
        if (bidirectional) {
            float dx = endX - startX;
            float dy = endY - startY;
            float length = (float)Math.sqrt(dx * dx + dy * dy);
            int hash = 31 * (31 * (31 * Float.floatToIntBits(startX) + Float.floatToIntBits(startY)) + Float.floatToIntBits(endX)) + Float.floatToIntBits(endY);
            float bend = (0.1F + 0.1F * ((hash >>> 1) % 1000) / 1000.0F) * ((hash & 1) == 0 ? 1 : -1);
            float cx = (startX + endX) / 2.0F - dy * bend;
            float cy = (startY + endY) / 2.0F + dx * bend;
            int segments = Math.max(4, Math.round(length / 6.0F));
            Matrix4f matrix = context.m_280168_().m_85850_().m_252922_();
            float px = startX;
            float py = startY;

            for (int i = 1; i <= segments; i++) {
                float t = (float)i / segments;
                float u = 1.0F - t;
                float x = u * u * startX + 2.0F * u * t * cx + t * t * endX;
                float y = u * u * startY + 2.0F * u * t * cy + t * t * endY;
                this.emitLine(this.strokeBatch, matrix, strokeColor, px, py, x, y, 3.0F);
                this.emitLine(this.fillBatch, matrix, fillColor, px, py, x, y, 1.0F);
                px = x;
                py = y;
            }

            ci.cancel();
        }
    }
}
