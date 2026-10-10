package com.mdvlcraft.binder.mixin.puffish;

import net.puffish.skillsmod.client.SkillsClientMod;
import net.puffish.skillsmod.client.data.ClientSkillScreenData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The skill tabs the client knows about (with their level and experience), for the character screen. */
@Mixin(value = SkillsClientMod.class, remap = false)
public interface SkillsClientModAccessor {
    @Accessor("screenData")
    ClientSkillScreenData mdvlcraft$screenData();
}
