package com.mdvlcraft.binder.combat;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class BinderEffects {
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, "mdvlcraft");
    /** No other effect can be applied while this lasts (Inverted Spear of Heaven). */
    public static final RegistryObject<MobEffect> SEALED = EFFECTS.register("sealed", () -> new MobEffect(MobEffectCategory.HARMFUL, 0x4A2C6E) {
    });

    private BinderEffects() {
    }

    public static void register(IEventBus modBus) {
        EFFECTS.register(modBus);
    }
}
