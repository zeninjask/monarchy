package com.mdvlcraft.binder.mixin;

import com.google.gson.JsonElement;
import com.mdvlcraft.binder.content.DisabledContent;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ServerAdvancementManager.class})
public abstract class ServerAdvancementManagerMixin {
    @Inject(
        method = {"apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V"},
        at = {@At("HEAD")}
    )
    private void mdvlcraft$hideDisabledAdvancements(
        Map<ResourceLocation, JsonElement> advancements, ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfo ci
    ) {
        advancements.forEach((id, json) -> {
            if (DisabledContent.isDisabled(id) && json.isJsonObject()) {
                json.getAsJsonObject().remove("display");
            }
        });
    }
}
