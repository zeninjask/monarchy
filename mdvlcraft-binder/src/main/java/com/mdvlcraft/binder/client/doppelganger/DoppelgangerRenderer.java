package com.mdvlcraft.binder.client.doppelganger;

import com.mdvlcraft.binder.doppelganger.DoppelgangerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.resources.ResourceLocation;

/** Draws the double exactly like its owner: the owner's skin, arm width, armour and held items. */
public class DoppelgangerRenderer extends LivingEntityRenderer<DoppelgangerEntity, PlayerModel<DoppelgangerEntity>> {
    private final PlayerModel<DoppelgangerEntity> wide;
    private final PlayerModel<DoppelgangerEntity> slim;

    public DoppelgangerRenderer(Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.wide = this.model;
        this.slim = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        this.addLayer(new HumanoidArmorLayer<>(
            this,
            new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
            new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
            context.getModelManager()
        ));
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
        this.addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getItemInHandRenderer()));
        this.addLayer(new ElytraLayer<>(this, context.getModelSet()));
    }

    @Override
    public void render(DoppelgangerEntity entity, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int light) {
        this.model = DoppelgangerSkin.slim(entity) ? this.slim : this.wide;
        this.model.crouching = entity.isCrouching();
        this.model.rightArmPose = entity.getMainHandItem().isEmpty() ? ArmPose.EMPTY : ArmPose.ITEM;
        this.model.leftArmPose = entity.getOffhandItem().isEmpty() ? ArmPose.EMPTY : ArmPose.ITEM;
        super.render(entity, yaw, partialTicks, poseStack, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(DoppelgangerEntity entity) {
        return DoppelgangerSkin.texture(entity);
    }

    @Override
    protected void scale(DoppelgangerEntity entity, PoseStack poseStack, float partialTicks) {
        // same 0.9375 scale the player renderer uses
        poseStack.scale(0.9375F, 0.9375F, 0.9375F);
    }

    @Override
    protected boolean shouldShowName(DoppelgangerEntity entity) {
        return false;
    }
}
