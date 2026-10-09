package com.mdvlcraft.binder.compat.villagerecruits;

import com.example.villagerecruits.faction.VillageFaction;
import com.example.villagerecruits.structures.workers.PlayerBuildConsent;
import com.mdvlcraft.binder.config.BinderConfig;

public final class AiVillageBuilding {
    private AiVillageBuilding() {
    }

    /** Whether a Village Recruits faction may build up its villages: always for player factions. */
    public static boolean allowed(VillageFaction faction) {
        return !BinderConfig.stopAiVillageBuilding() || faction != null && PlayerBuildConsent.isPlayerFaction(faction.id);
    }
}
