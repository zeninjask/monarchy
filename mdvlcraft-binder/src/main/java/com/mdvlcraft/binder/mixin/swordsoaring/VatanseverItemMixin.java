package com.mdvlcraft.binder.mixin.swordsoaring;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.p1nero.ss.item.VatanseverItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
    value = {VatanseverItem.class},
    remap = false
)
public abstract class VatanseverItemMixin {
    // Item#use: "use" in the dev environment, its SRG name in the released jar (remap is off for this class)
    @Inject(
        method = {"use", "m_7203_"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private void mdvlcraft$noTakeOff(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        cir.setReturnValue(InteractionResultHolder.m_19098_(player.m_21120_(hand)));
    }

    @Inject(
        method = {"elytraFlightTick"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private void mdvlcraft$noFlight(ItemStack stack, LivingEntity entity, int flightTicks, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
