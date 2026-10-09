package com.mdvlcraft.binder.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class BinderConfig {
    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.BooleanValue STOP_AI_VILLAGE_BUILDING;
    private static final ForgeConfigSpec.BooleanValue STRIP_RECRUIT_BUFFS;
    private static final ForgeConfigSpec.IntValue INVERTED_SPEAR_SEAL_SECONDS;
    private static final ForgeConfigSpec.DoubleValue TECHNIQUE_REFERENCE_DAMAGE;
    private static final ForgeConfigSpec.DoubleValue TECHNIQUE_MINIMUM_FACTOR;
    private static final ForgeConfigSpec.BooleanValue MOB_GEAR;
    private static final ForgeConfigSpec.DoubleValue MOB_WEAPON_CHANCE;
    private static final ForgeConfigSpec.DoubleValue MOB_ARMOR_CHANCE;
    private static final ForgeConfigSpec.DoubleValue MOB_ARMOR_PIECE_CHANCE;
    private static final ForgeConfigSpec.BooleanValue MOB_WEAPONS_UP_TO_IRON;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("villageRecruits");
        STOP_AI_VILLAGE_BUILDING = builder
            .comment(
                "Stop AI factions from Village Recruits from building their villages up into cities.",
                "Player factions still build when their leader allows it."
            )
            .define("stopAiVillageBuilding", true);
        STRIP_RECRUIT_BUFFS = builder
            .comment(
                "Stop Recruits and Village Recruits from giving their soldiers beneficial effects of their own",
                "(morale, elite, leader and travel buffs). Potions, spells and beacons still work on them."
            )
            .define("stripRecruitBuffs", true);
        builder.pop();

        builder.push("combat");
        INVERTED_SPEAR_SEAL_SECONDS = builder
            .comment("How long a hit with the Inverted Spear of Heaven stops any effect being applied to the target.")
            .defineInRange("invertedSpearSealSeconds", 4, 0, 60);
        TECHNIQUE_REFERENCE_DAMAGE = builder
            .comment(
                "Cursed Fate technique damage is multiplied by (caster's melee damage / this value).",
                "6 is an iron sword. 0 turns the scaling off."
            )
            .defineInRange("techniqueReferenceDamage", 6.0, 0.0, 1000.0);
        TECHNIQUE_MINIMUM_FACTOR = builder
            .comment("Lowest multiplier technique damage can get, e.g. when cast bare-handed.")
            .defineInRange("techniqueMinimumFactor", 0.5, 0.0, 1.0);
        builder.pop();

        builder.push("mobGear");
        MOB_GEAR = builder
            .comment(
                "Give hostile mobs (entity type tags mdvlcraft:armed_mobs and mdvlcraft:armoured_mobs) weapons from",
                "Epic Fight and Epic Knights and armour no better than iron, including Epic Knights armour."
            )
            .define("enabled", true);
        MOB_WEAPON_CHANCE = builder
            .comment("Chance for a mob in mdvlcraft:armed_mobs to spawn with a melee weapon.")
            .defineInRange("weaponChance", 0.4, 0.0, 1.0);
        MOB_ARMOR_CHANCE = builder
            .comment("Chance for a mob in mdvlcraft:armoured_mobs to spawn with armour.")
            .defineInRange("armorChance", 0.4, 0.0, 1.0);
        MOB_ARMOR_PIECE_CHANCE = builder
            .comment("For a mob that gets armour, the chance for each slot to get a piece.")
            .defineInRange("armorPieceChance", 0.6, 0.0, 1.0);
        MOB_WEAPONS_UP_TO_IRON = builder
            .comment("Only give mobs weapons made of iron or weaker materials (no diamond, netherite, steel and so on).")
            .define("weaponsUpToIron", true);
        builder.pop();
        SPEC = builder.build();
    }

    private BinderConfig() {
    }

    public static boolean stopAiVillageBuilding() {
        return !SPEC.isLoaded() || STOP_AI_VILLAGE_BUILDING.get();
    }

    public static boolean stripRecruitBuffs() {
        return !SPEC.isLoaded() || STRIP_RECRUIT_BUFFS.get();
    }

    public static int invertedSpearSealTicks() {
        return (SPEC.isLoaded() ? INVERTED_SPEAR_SEAL_SECONDS.get() : 4) * 20;
    }

    public static double techniqueReferenceDamage() {
        return SPEC.isLoaded() ? TECHNIQUE_REFERENCE_DAMAGE.get() : 6.0;
    }

    public static double techniqueMinimumFactor() {
        return SPEC.isLoaded() ? TECHNIQUE_MINIMUM_FACTOR.get() : 0.5;
    }

    public static boolean mobGear() {
        return !SPEC.isLoaded() || MOB_GEAR.get();
    }

    public static double mobWeaponChance() {
        return SPEC.isLoaded() ? MOB_WEAPON_CHANCE.get() : 0.4;
    }

    public static double mobArmorChance() {
        return SPEC.isLoaded() ? MOB_ARMOR_CHANCE.get() : 0.4;
    }

    public static double mobArmorPieceChance() {
        return SPEC.isLoaded() ? MOB_ARMOR_PIECE_CHANCE.get() : 0.6;
    }

    public static boolean mobWeaponsUpToIron() {
        return !SPEC.isLoaded() || MOB_WEAPONS_UP_TO_IRON.get();
    }
}
