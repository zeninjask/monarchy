package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.ability.Ability;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public final class ClientAbilities {
    private static Map<Ability, Integer> granted = Map.of();
    private static List<Optional<Ability>> wheel = Collections.nCopies(8, Optional.empty());
    private static int selected;

    private ClientAbilities() {
    }

    public static void update(Map<ResourceLocation, Integer> grantedIds, List<Optional<ResourceLocation>> loadout) {
        Map<Ability, Integer> resolved = new LinkedHashMap<>();
        grantedIds.entrySet()
            .stream()
            .map(entry -> Map.entry(resolve(entry.getKey()), entry.getValue()))
            .sorted(Comparator.comparing(entry -> entry.getKey().displayName().getString()))
            .forEach(entry -> resolved.put(entry.getKey(), entry.getValue()));
        granted = resolved;
        wheel = loadout.stream().map(slot -> slot.map(ClientAbilities::resolve)).toList();
    }

    public static void reset() {
        granted = Map.of();
        wheel = Collections.nCopies(8, Optional.empty());
        selected = 0;
    }

    private static Ability resolve(ResourceLocation id) {
        return Ability.byId(id).orElseThrow(() -> new IllegalStateException("Server sent unknown ability " + id));
    }

    public static Map<Ability, Integer> granted() {
        return granted;
    }

    public static int level(Ability ability) {
        return granted.getOrDefault(ability, 0);
    }

    public static Optional<Ability> slot(int slot) {
        return wheel.get(slot);
    }

    public static int selected() {
        return selected;
    }

    public static void select(int slot) {
        selected = slot;
    }
}
