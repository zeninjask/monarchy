package com.mdvlcraft.binder.client.doppelganger;

import com.mdvlcraft.binder.doppelganger.DoppelgangerEntity;
import com.mdvlcraft.binder.doppelganger.DoppelgangerPatch;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.world.entity.EntityType;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.client.mesh.HumanoidMesh;
import yesman.epicfight.client.renderer.patched.entity.PHumanoidRenderer;

/** Epic Fight's renderer for the double: the player mesh (Alex for slim skins) with the owner's skin. */
public class PDoppelgangerRenderer
    extends PHumanoidRenderer<DoppelgangerEntity, DoppelgangerPatch, PlayerModel<DoppelgangerEntity>, DoppelgangerRenderer, HumanoidMesh> {
    public PDoppelgangerRenderer(Context context, EntityType<?> entityType) {
        super(Meshes.BIPED, context, entityType);
    }

    @Override
    public AssetAccessor<HumanoidMesh> getMeshProvider(DoppelgangerPatch patch) {
        return DoppelgangerSkin.slim(patch.getOriginal()) ? Meshes.ALEX : Meshes.BIPED;
    }
}
