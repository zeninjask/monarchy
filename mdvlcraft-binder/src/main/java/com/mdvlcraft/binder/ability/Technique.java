package com.mdvlcraft.binder.ability;

import java.util.Arrays;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public enum Technique implements Ability {
    DISMANTLE("dismantle", 26, 12, 1, true, "minecraft:textures/item/shears.png"),
    CLEAVE("cleave", 27, 20, 3, true, "minecraft:textures/item/iron_axe.png"),
    MALEVOLENT_SHRINE("malevolent_shrine", 31, 100, 120, false, "cursedfate:textures/screens/closedshrinevarient.png"),
    OPEN_MALEVOLENT_SHRINE("open_malevolent_shrine", 31, 100, 120, false, "cursedfate:textures/screens/openshrinevarient.png"),
    BOOGIE_WOOGIE("boogie_woogie", 32, 10, 2, false, "minecraft:textures/item/ender_pearl.png"),
    BLOCK_CLAP("block_clap", 33, 10, 2, false, "minecraft:textures/item/chorus_fruit.png"),
    BLITZ("blitz", 60, 15, 8, false, "cursedfate:textures/mob_effect/blitz_effect.png"),
    SURPRISE_ATTACK("surprise_attack", 66, 15, 15, false, "mdvlcraft:textures/gui/icons/tab/assassin.png"),
    FOLLOW_UP_KICK("follow_up_kick", 114, 15, 6, false, "naoyaaddon:textures/screens/naoyaicon.png"),
    PHANTOM_MOVEMENT("phantom_movement", 118, 20, 0, false, "mdvlcraft:textures/gui/icons/ability/phantom_movement.png");

    private final ResourceLocation id;
    final int abilityId;
    private final int manaCost;
    private final int cooldownTicks;
    final boolean held;
    private final ResourceLocation icon;

    private Technique(String name, int abilityId, int manaCost, int cooldownSeconds, boolean held, String icon) {
        this.id = ResourceLocation.fromNamespaceAndPath("mdvlcraft", name);
        this.abilityId = abilityId;
        this.manaCost = manaCost;
        this.cooldownTicks = cooldownSeconds * 20;
        this.held = held;
        this.icon = ResourceLocation.parse(icon);
    }

    public int cooldownTicks() {
        return this.cooldownTicks;
    }

    boolean isShrine() {
        return this == MALEVOLENT_SHRINE || this == OPEN_MALEVOLENT_SHRINE;
    }

    int domainVariant() {
        return switch (this) {
            case MALEVOLENT_SHRINE -> 0;
            case OPEN_MALEVOLENT_SHRINE -> 1;
            default -> throw new IllegalStateException(this + " is not a domain");
        };
    }

    boolean weaponFree() {
        return this == BLITZ || this == SURPRISE_ATTACK;
    }

    boolean projection() {
        return this == FOLLOW_UP_KICK || this == PHANTOM_MOVEMENT;
    }

    boolean isSlash() {
        return this == DISMANTLE || this == CLEAVE;
    }

    public static Optional<Technique> byId(ResourceLocation id) {
        return Arrays.stream(values()).filter(t -> t.id.equals(id)).findFirst();
    }

    @Override
    public ResourceLocation id() {
        return this.id;
    }

    @Override
    public Component displayName() {
        return Component.m_237115_("ability.mdvlcraft." + this.id.m_135815_());
    }

    @Override
    public ResourceLocation icon() {
        return this.icon;
    }

    @Override
    public int manaCost(int level) {
        return this.manaCost;
    }
}
