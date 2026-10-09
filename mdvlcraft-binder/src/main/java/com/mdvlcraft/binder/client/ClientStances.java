package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.ability.StanceElement;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class ClientStances {
    private static final Map<Integer, ClientStances.State> STATES = new HashMap<>();

    private ClientStances() {
    }

    public static void update(int entityId, StanceElement element, boolean active) {
        STATES.put(entityId, new ClientStances.State(element, active));
    }

    public static void reset() {
        STATES.clear();
    }

    public static Optional<StanceElement> active(int entityId) {
        ClientStances.State state = STATES.get(entityId);
        return state != null && state.active() ? Optional.of(state.element()) : Optional.empty();
    }

    public static StanceElement element(int entityId) {
        ClientStances.State state = STATES.get(entityId);
        return state == null ? StanceElement.FIRE : state.element();
    }

    private record State(StanceElement element, boolean active) {
    }
}
