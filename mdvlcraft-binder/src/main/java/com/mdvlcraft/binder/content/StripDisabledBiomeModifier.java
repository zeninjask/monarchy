package com.mdvlcraft.binder.content;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.BiomeModifier.Phase;
import net.minecraftforge.common.world.ModifiableBiomeInfo.BiomeInfo.Builder;

public final class StripDisabledBiomeModifier implements BiomeModifier {
    public static final StripDisabledBiomeModifier INSTANCE = new StripDisabledBiomeModifier();
    public static final Codec<StripDisabledBiomeModifier> CODEC = Codec.unit(INSTANCE);

    private StripDisabledBiomeModifier() {
    }

    public void modify(Holder<Biome> biome, Phase phase, Builder builder) {
        if (phase == Phase.REMOVE) {
            for (MobCategory category : builder.getMobSpawnSettings().getSpawnerTypes()) {
                builder.getMobSpawnSettings().getSpawner(category).removeIf(spawner -> DisabledContent.isDisabled(spawner.type));
            }
        }
    }

    public Codec<? extends BiomeModifier> codec() {
        return CODEC;
    }
}
