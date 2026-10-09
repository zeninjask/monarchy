package com.mdvlcraft.binder.ability;

import com.bless.naoyaaddon.network.NaoyaAddonVariables;
import com.cursedfate_roslon_meimei.network.CursedfateRoslonMeimeiModVariables;
import net.minecraft.server.level.ServerPlayer;

/**
 * Black Bird Manipulation and Projection Sorcery both cast Cursed Fate abilities 113-118, each only for
 * a player whose Cursed Fate technique matches the addon's own (its {@code NewTechnique}). Projection
 * Sorcery stays active as before; Black Bird Manipulation is switched on only while one of its
 * techniques is held, with Projection Sorcery switched off meanwhile, so one cast never fires both.
 */
public final class BlackBirdManipulation {
    private static final String INACTIVE = "mdvlcraft:inactive";

    private BlackBirdManipulation() {
    }

    static void activate(ServerPlayer player) {
        player.getCapability(CursedfateRoslonMeimeiModVariables.PLAYER_VARIABLES_CAPABILITY).ifPresent(variables -> {
            variables.NewTechnique = variables.CursedfateTechnique;
            variables.syncPlayerVariables(player);
        });
        player.getCapability(NaoyaAddonVariables.PLAYER_VARIABLES_CAPABILITY).ifPresent(variables -> {
            variables.NewTechnique = INACTIVE;
            variables.syncPlayerVariables(player);
        });
    }

    /** Back to the normal state: Black Bird Manipulation off, Projection Sorcery on. */
    public static void deactivate(ServerPlayer player) {
        player.getCapability(CursedfateRoslonMeimeiModVariables.PLAYER_VARIABLES_CAPABILITY).ifPresent(variables -> {
            if (!INACTIVE.equals(variables.NewTechnique)) {
                variables.NewTechnique = INACTIVE;
                variables.syncPlayerVariables(player);
                // the addon resets this when the key is let go, which it no longer sees once switched off
                player.getPersistentData().putBoolean("simpleCheck", false);
            }
        });
        player.getCapability(NaoyaAddonVariables.PLAYER_VARIABLES_CAPABILITY).ifPresent(variables -> {
            if (INACTIVE.equals(variables.NewTechnique)) {
                variables.NewTechnique = variables.CursedfateTechnique;
                variables.syncPlayerVariables(player);
            }
        });
    }
}
