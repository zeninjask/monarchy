package com.mdvlcraft.binder.ability;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class Loadout {
    public static final int SLOTS = 8;
    private static final String KEY = "mdvlcraft:loadout";
    private static final String EMPTY = "";

    private Loadout() {
    }

    public static List<Optional<ResourceLocation>> get(Player player) {
        ListTag tag = data(player).getList("mdvlcraft:loadout", 8);
        List<Optional<ResourceLocation>> slots = new ArrayList<>(Collections.nCopies(8, Optional.empty()));

        for (int i = 0; i < Math.min(8, tag.size()); i++) {
            String id = tag.getString(i);
            slots.set(i, id.equals("") ? Optional.empty() : Optional.of(ResourceLocation.parse(id)));
        }

        return slots;
    }

    public static void set(ServerPlayer player, int slot, Optional<ResourceLocation> ability) {
        List<Optional<ResourceLocation>> slots = get(player);
        slots.set(slot, ability);
        ListTag tag = new ListTag();
        slots.forEach(id -> tag.add(StringTag.valueOf(id.<String>map(ResourceLocation::toString).orElse(""))));
        data(player).put("mdvlcraft:loadout", tag);
        AbilityGrants.markDirty(player);
    }

    private static CompoundTag data(Player player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains("PlayerPersisted", 10)) {
            persistent.put("PlayerPersisted", new CompoundTag());
        }

        return persistent.getCompound("PlayerPersisted");
    }
}
