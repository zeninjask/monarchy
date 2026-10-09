package com.mdvlcraft.binder.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class BinderConfig {
    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.BooleanValue STOP_AI_VILLAGE_BUILDING;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("villageRecruits");
        STOP_AI_VILLAGE_BUILDING = builder
            .comment(
                "Stop AI factions from Village Recruits from building their villages up into cities.",
                "Player factions still build when their leader allows it."
            )
            .define("stopAiVillageBuilding", true);
        builder.pop();
        SPEC = builder.build();
    }

    private BinderConfig() {
    }

    public static boolean stopAiVillageBuilding() {
        return !SPEC.isLoaded() || STOP_AI_VILLAGE_BUILDING.get();
    }
}
