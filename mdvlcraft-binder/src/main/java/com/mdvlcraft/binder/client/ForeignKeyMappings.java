package com.mdvlcraft.binder.client;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.Collection;
import net.minecraft.client.KeyMapping;

public final class ForeignKeyMappings {
    private ForeignKeyMappings() {
    }

    public static void unbind(Collection<? extends KeyMapping> mappings) {
        mappings.forEach(mapping -> mapping.m_90848_(InputConstants.f_84822_));
        KeyMapping.m_90854_();
    }
}
