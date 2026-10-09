package com.mdvlcraft.binder.doppelganger;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import yesman.epicfight.api.forgeevent.EntityPatchRegistryEvent;
import yesman.epicfight.gameasset.Armatures;

public final class DoppelgangerSetup {
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "mdvlcraft");
    public static final RegistryObject<EntityType<DoppelgangerEntity>> DOPPELGANGER = ENTITY_TYPES.register(
        "doppelganger",
        () -> EntityType.Builder.<DoppelgangerEntity>of(DoppelgangerEntity::new, MobCategory.MISC)
            .sized(0.6F, 1.8F)
            .clientTrackingRange(10)
            .noSummon()
            .build("doppelganger")
    );

    private DoppelgangerSetup() {
    }

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
        modBus.addListener(DoppelgangerSetup::onAttributes);
        modBus.addListener(DoppelgangerSetup::onEntityPatches);
        modBus.addListener(DoppelgangerSetup::onCommonSetup);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(DOPPELGANGER.get(), DoppelgangerEntity.createAttributes().build());
    }

    private static void onEntityPatches(EntityPatchRegistryEvent event) {
        event.getTypeEntry().put(DOPPELGANGER.get(), entity -> DoppelgangerPatch::new);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> Armatures.registerEntityTypeArmature(DOPPELGANGER.get(), Armatures.BIPED));
    }
}
