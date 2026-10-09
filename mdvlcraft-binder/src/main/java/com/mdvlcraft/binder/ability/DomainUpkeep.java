package com.mdvlcraft.binder.ability;

import cursedfate.entity.DomainExpansionEntityEntity;
import cursedfate.network.CursedfateModVariables.PlayerVariables;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

final class DomainUpkeep {
    static final int MAX_TICKS = 400;
    private static final int WIND_UP_TICKS = 100;
    private static final double DOMAIN_RADIUS = 27.0;
    private static final ResourceLocation OPEN_SHRINE = ResourceLocation.fromNamespaceAndPath("cursedfate", "shrine");
    private static final Map<UUID, DomainUpkeep.Domain> DOMAINS = new HashMap<>();

    private DomainUpkeep() {
    }

    static void windingUp(ServerPlayer player, Technique shrine) {
        DOMAINS.put(player.getUUID(), new DomainUpkeep.Domain(shrine, player.level().getGameTime(), -1L));
    }

    static boolean isWindingUp(ServerPlayer player) {
        DomainUpkeep.Domain domain = DOMAINS.get(player.getUUID());
        return domain != null && domain.openedTick() < 0L;
    }

    static boolean closeClosedDomain(ServerPlayer player) {
        List<DomainExpansionEntityEntity> domains = ownedClosedDomains(player);
        if (domains.isEmpty()) {
            return false;
        } else {
            domains.forEach(domain -> domain.getPersistentData().putBoolean("forcestopdomain", true));
            DOMAINS.remove(player.getUUID());
            return true;
        }
    }

    static void tick(MinecraftServer server) {
        Iterator<Entry<UUID, DomainUpkeep.Domain>> entries = DOMAINS.entrySet().iterator();

        while (entries.hasNext()) {
            Entry<UUID, DomainUpkeep.Domain> entry = entries.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                entries.remove();
            } else {
                DomainUpkeep.Domain domain = entry.getValue();
                long now = player.level().getGameTime();
                boolean up = domain.shrine() == Technique.MALEVOLENT_SHRINE ? !ownedClosedDomains(player).isEmpty() : ownsOpenShrine(player);
                if (domain.openedTick() < 0L) {
                    if (up) {
                        entry.setValue(new DomainUpkeep.Domain(domain.shrine(), domain.castTick(), now));
                    } else if (now - domain.castTick() > 100L) {
                        entries.remove();
                    }
                } else if (up && now - domain.openedTick() < 400L) {
                    PlayerVariables variables = TechniqueRunner.variables(player);
                    double needed = player.getPersistentData().getDouble("DomainCECost") + variables.CursedEnegryMax;
                    if (variables.CursedEnegry < needed) {
                        variables.CursedEnegry = needed;
                        variables.syncPlayerVariables(player);
                    }
                } else {
                    entries.remove();
                }
            }
        }
    }

    private static List<DomainExpansionEntityEntity> ownedClosedDomains(ServerPlayer player) {
        String owner = player.getStringUUID();
        return player.level()
            .getEntitiesOfClass(
                DomainExpansionEntityEntity.class,
                player.getBoundingBox().inflate(27.0),
                entity -> owner.equals(entity.getPersistentData().getString("CurrentDomainOwner"))
            );
    }

    private static boolean ownsOpenShrine(ServerPlayer player) {
        EntityType<?> shrineType = (EntityType<?>)ForgeRegistries.ENTITY_TYPES.getValue(OPEN_SHRINE);
        if (shrineType != null && OPEN_SHRINE.equals(ForgeRegistries.ENTITY_TYPES.getKey(shrineType))) {
            String owner = player.getStringUUID();
            return !player.level()
                .getEntities(
                    (Entity)null,
                    player.getBoundingBox().inflate(27.0),
                    entity -> entity.getType() == shrineType && owner.equals(entity.getPersistentData().getString("PlayerUUID"))
                )
                .isEmpty();
        } else {
            throw new IllegalStateException("Cursed Fate entity " + OPEN_SHRINE + " is not registered");
        }
    }

    private record Domain(Technique shrine, long castTick, long openedTick) {
    }
}
