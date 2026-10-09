package com.mdvlcraft.binder.mixin;

import com.mdvlcraft.binder.content.DisabledContent;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraftforge.common.ForgeHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
    value = {ForgeHooks.class},
    remap = false
)
public abstract class ForgeHooksMixin {
    @Inject(
        method = {"modifyLoot(Lnet/minecraft/resources/ResourceLocation;Lit/unimi/dsi/fastutil/objects/ObjectArrayList;Lnet/minecraft/world/level/storage/loot/LootContext;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;"},
        at = {@At("RETURN")}
    )
    private static void mdvlcraft$stripForbiddenLoot(
        ResourceLocation lootTableId, ObjectArrayList<ItemStack> generatedLoot, LootContext context, CallbackInfoReturnable<ObjectArrayList<ItemStack>> cir
    ) {
        ((ObjectArrayList)cir.getReturnValue()).removeIf(DisabledContent::isForbidden);
    }
}
