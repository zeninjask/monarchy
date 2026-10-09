package com.mdvlcraft.binder.content;

import com.mojang.serialization.Codec;
import cursedfate.procedures.OnEntityDeathMProcedure;
import cursedfate.procedures.OnEntityDeathProcedure;
import cursedfate.procedures.OpenChestProcedure;
import cursedfate.procedures.PlayerTakesDamageProcedure;
import cursedfate.procedures.QuestEntityKilledProcedure;
import cursedfate.procedures.QuestRightClickedProcedure;
import cursedfate.procedures.ReturnAttackProcedure;
import cursedfate.procedures.SetTechniqueOnJoinProcedure;
import java.util.List;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries.Keys;

public final class DisabledContentSetup {
    private static final DeferredRegister<Codec<? extends BiomeModifier>> BIOME_MODIFIERS = DeferredRegister.create(
        Keys.BIOME_MODIFIER_SERIALIZERS, "mdvlcraft"
    );
    private static final List<Class<?>> CURSED_FATE_LISTENERS = List.of(
        SetTechniqueOnJoinProcedure.class,
        OnEntityDeathProcedure.class,
        OnEntityDeathMProcedure.class,
        OpenChestProcedure.class,
        QuestEntityKilledProcedure.class,
        QuestRightClickedProcedure.class,
        PlayerTakesDamageProcedure.class,
        ReturnAttackProcedure.class
    );

    private DisabledContentSetup() {
    }

    public static void register(IEventBus modBus) {
        BIOME_MODIFIERS.register(modBus);
        modBus.addListener(DisabledContentSetup::onCommonSetup);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> CURSED_FATE_LISTENERS.forEach(MinecraftForge.EVENT_BUS::unregister));
    }

    static {
        BIOME_MODIFIERS.register("strip_disabled", () -> StripDisabledBiomeModifier.CODEC);
    }
}
