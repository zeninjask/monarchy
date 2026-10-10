package com.mdvlcraft.binder.compat;

import io.github.edwinmindcraft.calio.common.access.MappedRegistryAccess;
import io.github.edwinmindcraft.calio.common.registry.CalioDynamicRegistryManager;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Origins' powers reach the client on the network thread, which creates registry holders for "multiple" powers'
 * sub-powers while the main thread is resetting and filling the same registry. The two writers race on the
 * registry's HashMap, a holder can come back null, and the join fails ("null value in entry: _hostile_when_hit").
 * Holders are created here under the lock the main thread already holds while it fills the registry.
 */
public final class CalioHolders {
    private CalioHolders() {
    }

    public static <T> Holder<T> getOrCreate(MappedRegistryAccess<T> access, ResourceKey<T> key) {
        synchronized (lock(access)) {
            Holder<T> holder = access.calio$getOrCreateHolderOrThrow(key);
            return holder != null ? holder : access.calio$getOrCreateHolderOrThrow(key);
        }
    }

    private static Object lock(Object registry) {
        if (FMLEnvironment.dist.isClient() && registry instanceof MappedRegistry<?> mapped) {
            Object lock = CalioDynamicRegistryManager.getClientInstance().getLock((ResourceKey) mapped.key());
            if (lock != null) {
                return lock;
            }
        }
        return registry;
    }
}
