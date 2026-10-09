package com.mdvlcraft.binder.mixin.irons;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.SelectionOption;
import java.util.List;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
    value = {SpellSelectionManager.class},
    remap = false
)
public abstract class SpellSelectionManagerMixin {
    @Shadow
    @Final
    private List<SelectionOption> selectionOptionList;
    @Shadow
    private int selectionIndex;

    @Inject(
        method = {"init"},
        at = {@At("TAIL")}
    )
    private void mdvlcraft$noItemSpells(Player player, CallbackInfo ci) {
        this.selectionOptionList.clear();
        this.selectionIndex = -1;
    }
}
