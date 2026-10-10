package com.mdvlcraft.binder.mixin.origins;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * The race and class pickers list origins by impact, then by each mod's own order, so the list jumps between mods.
 * List them alphabetically by their shown name instead.
 */
@Pseudo
@Mixin(targets = "io.github.apace100.origins.screen.ChooseOriginScreen", remap = false)
public abstract class ChooseOriginScreenMixin {
    @Redirect(
        method = "<init>",
        at = @At(value = "INVOKE", target = "Ljava/util/List;sort(Ljava/util/Comparator;)V"),
        require = 0
    )
    private void mdvlcraft$alphabetical(List<Object> origins, Comparator<Object> byImpact) {
        origins.sort(Comparator.comparing(ChooseOriginScreenMixin::mdvlcraft$name));
    }

    private static String mdvlcraft$name(Object holder) {
        try {
            Object origin = ((Holder<?>) holder).value();
            Method getName = origin.getClass().getMethod("getName");
            return ((Component) getName.invoke(origin)).getString().toLowerCase(Locale.ROOT);
        } catch (ReflectiveOperationException | ClassCastException e) {
            return String.valueOf(holder);
        }
    }
}
