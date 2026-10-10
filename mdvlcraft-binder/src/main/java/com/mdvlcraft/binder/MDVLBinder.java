package com.mdvlcraft.binder;

import com.mdvlcraft.binder.ability.AbilityReward;
import com.mdvlcraft.binder.attribute.BinderAttributes;
import com.mdvlcraft.binder.client.ClientAbilitySetup;
import com.mdvlcraft.binder.client.ClientContentSetup;
import com.mdvlcraft.binder.client.HerobrineRender;
import com.mdvlcraft.binder.combat.BinderEffects;
import com.mdvlcraft.binder.compat.issponder.PreviewCasts;
import com.mdvlcraft.binder.config.BinderConfig;
import com.mdvlcraft.binder.content.DisabledContentSetup;
import com.mdvlcraft.binder.content.OriginIcon;
import com.mdvlcraft.binder.doppelganger.DoppelgangerSetup;
import com.mdvlcraft.binder.epicskill.EpicSkillReward;
import com.mdvlcraft.binder.integration.CataclysmIntegration;
import com.mdvlcraft.binder.integration.EpicFightIntegration;
import com.mdvlcraft.binder.network.BinderNetwork;
import com.mdvlcraft.binder.skills.ArchetypeTabs;
import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod("mdvlcraft")
public final class MDVLBinder {
    public static final String MOD_ID = "mdvlcraft";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MDVLBinder(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        IEventBus forgeBus = MinecraftForge.EVENT_BUS;
        context.registerConfig(Type.COMMON, BinderConfig.SPEC);
        DisabledContentSetup.register(modBus);
        OriginIcon.register(modBus);
        BinderAttributes.register(modBus);
        BinderEffects.register(modBus);
        DoppelgangerSetup.register(modBus);
        modBus.addListener(MDVLBinder::onCommonSetup);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientContentSetup.register(modBus);
            ClientAbilitySetup.register(modBus);
            HerobrineRender.register(modBus);
        }

        EpicFightIntegration.register(modBus, forgeBus);
        PreviewCasts.register();
        CataclysmIntegration.register(modBus, forgeBus);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        BinderNetwork.register();
        event.enqueueWork(() -> {
            AbilityReward.register();
            EpicSkillReward.register();
            ArchetypeTabs.register();
        });
    }
}
