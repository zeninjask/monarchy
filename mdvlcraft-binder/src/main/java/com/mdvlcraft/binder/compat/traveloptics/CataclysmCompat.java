package com.mdvlcraft.binder.compat.traveloptics;

import com.github.L_Ender.cataclysm.client.particle.Options.CircleLightningParticleOptions;
import com.github.L_Ender.cataclysm.client.particle.Options.RingParticleOptions;
import com.github.L_Ender.cataclysm.client.particle.RingParticle;

/**
 * Bridges T.O Magic 'n Extras (built for an older L_Ender's Cataclysm) to Cataclysm 3.31. The pack
 * ships a T.O jar rewritten by {@code tools/PatchTravelOptics.java}: classes Cataclysm only moved are
 * renamed in place, and the constructors and config fields that changed shape call these methods.
 */
public final class CataclysmCompat {
    private CataclysmCompat() {
    }

    /** Old {@code CircleLightningParticle.CircleData(r, g, b)}; 3.31 adds a size in front. */
    public static CircleLightningParticleOptions circle(int r, int g, int b) {
        return new CircleLightningParticleOptions(1.0F, r, g, b);
    }

    /** Old {@code RingParticle.RingData}: colours as 0-1 floats and the behaviour as an enum; 3.31 takes 0-255 ints and the ordinal. */
    public static RingParticleOptions ring(float yaw, float pitch, int duration, float r, float g, float b, float a, float scale,
                                           boolean facesCamera, RingParticle.EnumRingBehavior behavior) {
        return new RingParticleOptions(yaw, pitch, duration, Math.round(r * 255.0F), Math.round(g * 255.0F), Math.round(b * 255.0F),
            a, scale, facesCamera, behavior.ordinal());
    }

    /** Old {@code CMConfig.HarbingerHealingMultiplier} (default 1). */
    public static double harbingerHealingMultiplier() {
        return 1.0;
    }

    /** Old {@code CMConfig.HarbingerLightFire} (default true). */
    public static boolean harbingerLightFire() {
        return true;
    }
}
