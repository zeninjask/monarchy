package com.mdvlcraft.binder.client;

import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.Pack.Position;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.fml.ModList;

final class BdHillFix {
    private static final String BDHILL = "BDHill";

    private BdHillFix() {
    }

    static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() == PackType.CLIENT_RESOURCES) {
            Path root = ModList.get().getModFileById("mdvlcraft").getFile().findResource(new String[]{"resourcepacks", "bdhill_fix"});
            event.addRepositorySource(
                consumer -> {
                    if (!Minecraft.m_91087_().f_91066_.f_92117_.stream().noneMatch(id -> id.contains("BDHill"))) {
                        Pack pack = Pack.m_245429_(
                            "mdvlcraft:bdhill_fix",
                            Component.m_237113_("MDVLCraft BD&Hill fixes"),
                            true,
                            id -> new PathPackResources(id, root, true),
                            PackType.CLIENT_RESOURCES,
                            Position.TOP,
                            PackSource.f_10528_
                        );
                        if (pack == null) {
                            throw new IllegalStateException("MDVLCraft BD&Hill fix resource pack is missing its pack.mcmeta");
                        } else {
                            consumer.accept(pack);
                        }
                    }
                }
            );
        }
    }
}
