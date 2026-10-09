package com.mdvlcraft.binder.client.ponder;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

public final class ShipPonderPlugin implements PonderPlugin {
    public String getModId() {
        return "mdvlcraft";
    }

    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.forComponents(
                new ResourceLocation[]{ShipPonderScenes.item("watercraft_frame_angled"), ShipPonderScenes.item("oarlock"), ShipPonderScenes.item("oar")}
            )
            .addStoryBoard(ResourceLocation.fromNamespaceAndPath("mdvlcraft", "rowboat_slipway"), ShipPonderScenes::rowboat);
        helper.forComponents(
                new ResourceLocation[]{
                    ShipPonderScenes.item("watercraft_frame_angled"),
                    ShipPonderScenes.item("watercraft_frame_flat"),
                    ShipPonderScenes.item("cleat"),
                    ShipPonderScenes.item("anchor")
                }
            )
            .addStoryBoard(ResourceLocation.fromNamespaceAndPath("mdvlcraft", "sloop_slipway"), ShipPonderScenes::sloop);
    }
}
