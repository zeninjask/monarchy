package com.mdvlcraft.binder.client;

import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * True Herobrine draws Herobrine with the plain humanoid model, which has no jacket, sleeve or trouser layers.
 * Swap in the full (wide-arm) player model with the pack's own Herobrine skin, keeping the mod's glowing eyes
 * (the skin's eyes sit on the same pixels). The Binder loads after True Herobrine, so this registration wins.
 */
public final class HerobrineRender {
    private static final ResourceLocation ENTITY = new ResourceLocation("true_herobrine", "herobrine");
    private static final ResourceLocation SKIN = new ResourceLocation("mdvlcraft", "textures/entity/herobrine.png");
    private static final ResourceLocation GLOW = new ResourceLocation("true_herobrine", "textures/entities/herobrineglow.png");

    private HerobrineRender() {
    }

    public static void register(IEventBus modBus) {
        if (ModList.get().isLoaded(ENTITY.getNamespace())) {
            modBus.addListener(EventPriority.LOWEST, HerobrineRender::onRegisterRenderers);
        }
    }

    @SuppressWarnings("unchecked")
    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        EntityType<? extends Mob> type = (EntityType<? extends Mob>) ForgeRegistries.ENTITY_TYPES.getValue(ENTITY);
        if (type != null && type != EntityType.PIG) {
            event.registerEntityRenderer(type, Renderer::new);
        }
    }

    private static final class Renderer extends HumanoidMobRenderer<Mob, PlayerModel<Mob>> {
        Renderer(Context context) {
            super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
            this.addLayer(new HumanoidArmorLayer<>(
                this,
                new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()
            ));
            this.addLayer(new EyesLayer<>(this) {
                @Override
                public RenderType renderType() {
                    return RenderType.eyes(GLOW);
                }
            });
        }

        @Override
        public ResourceLocation getTextureLocation(Mob entity) {
            return SKIN;
        }
    }
}
