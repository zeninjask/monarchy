package com.mdvlcraft.binder.ability;

import com.mdvlcraft.binder.attribute.BinderAttributes;
import cursedfate.network.CursedfateModVariables;
import cursedfate.network.CursedfateModVariables.PlayerVariables;
import cursedfate.procedures.AbilitysProcedure;
import cursedfate.procedures.ReturnPlayerNearAOpenedDomainProcedure;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;

final class TechniqueRunner {
    private static final Map<UUID, Technique> HELD = new HashMap<>();

    private TechniqueRunner() {
    }

    static boolean isSlashing(ServerPlayer player) {
        Technique held = HELD.get(player.getUUID());
        return held != null && held.isSlash();
    }

    static void press(ServerPlayer player, Technique technique) {
        if (!technique.held) {
            cast(player, technique);
        } else {
            HELD.put(player.getUUID(), technique);
            if (TechniqueCooldowns.remaining(player, technique) == 0L && !cast(player, technique)) {
                HELD.remove(player.getUUID());
            }
        }
    }

    static void release(ServerPlayer player) {
        HELD.remove(player.getUUID());
        PlayerVariables variables = variables(player);
        if (variables.holdability1) {
            variables.holdability1 = false;
            variables.holdability = false;
            variables.AbilityToCast = 0.0;
            variables.syncPlayerVariables(player);
        }
    }

    static void tick(MinecraftServer server) {
        Iterator<Entry<UUID, Technique>> entries = HELD.entrySet().iterator();

        while (entries.hasNext()) {
            Entry<UUID, Technique> entry = entries.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                entries.remove();
            } else if (TechniqueCooldowns.remaining(player, entry.getValue()) == 0L && !cast(player, entry.getValue())) {
                entries.remove();
            }
        }
    }

    private static boolean cast(ServerPlayer player, Technique technique) {
        if (technique == Technique.FOLLOW_UP_KICK && player.isShiftKeyDown()) {
            ProjectionSorcery.toggleFlow(player);
            return false;
        } else {
            if (technique.isShrine()) {
                if (DomainUpkeep.isWindingUp(player)) {
                    return false;
                }

                if (DomainUpkeep.closeClosedDomain(player)) {
                    return true;
                }
            }

            long cooldown = TechniqueCooldowns.remaining(player, technique);
            if (cooldown > 0L) {
                player.connection
                    .send(
                        new ClientboundSetActionBarTextPacket(
                            Component.translatable("ability.mdvlcraft.on_cooldown", new Object[]{technique.displayName(), (cooldown + 19L) / 20L})
                                .withStyle(ChatFormatting.RED)
                        )
                    );
                return false;
            } else {
                MagicData magic = MagicData.getPlayerMagicData(player);
                int cost = cost(player, technique);
                if (!player.isCreative()) {
                    if (magic.getMana() < cost) {
                        player.connection
                            .send(
                                new ClientboundSetActionBarTextPacket(
                                    Component.translatable("ui.irons_spellbooks.cast_error_mana", new Object[]{technique.displayName()})
                                        .withStyle(ChatFormatting.RED)
                                )
                            );
                        return false;
                    }

                    magic.setMana(magic.getMana() - cost);
                    PacketDistributor.sendToPlayer(player, new SyncManaPacket(magic));
                }

                PlayerVariables variables = variables(player);
                variables.CursedEnegry = Math.max(variables.CursedEnegry, variables.CursedEnegryMax);
                variables.InnateMastery = 0.0;
                variables.InnateMasteryXp = 0.0;
                variables.AbilityNum1 = technique.abilityId;
                variables.holdability1 = true;
                variables.holdability = true;
                variables.syncPlayerVariables(player);
                if (technique.projection()) {
                    ProjectionSorcery.prepare(player);
                }

                Runnable execute = () -> AbilitysProcedure.execute(player.level(), player.getX(), player.getY(), player.getZ(), player);
                if (technique.weaponFree()) {
                    WeaponFreeCasts.run(execute);
                } else {
                    execute.run();
                }

                if (!modeToggle(player, technique)) {
                    TechniqueCooldowns.start(player, technique);
                }

                if (technique.isShrine() && variables.SelectDomainVarient) {
                    variables.DomainVarientNumber = technique.domainVariant();
                    variables.syncPlayerVariables(player);
                    DomainUpkeep.windingUp(player, technique);
                }

                return true;
            }
        }
    }

    private static boolean modeToggle(ServerPlayer player, Technique technique) {
        return technique == Technique.PHANTOM_MOVEMENT && player.isShiftKeyDown();
    }

    private static int cost(ServerPlayer player, Technique technique) {
        if (modeToggle(player, technique)) {
            return 0;
        } else {
            return technique.isShrine()
                    && ReturnPlayerNearAOpenedDomainProcedure.execute(player.level(), player.getX(), player.getY(), player.getZ())
                ? 0
                : Math.round(technique.manaCost(1) * (float)(1.0 - player.getAttributeValue((Attribute)BinderAttributes.TECHNIQUE_EFFICIENCY.get())));
        }
    }

    static PlayerVariables variables(ServerPlayer player) {
        return (PlayerVariables)player.getCapability(CursedfateModVariables.PLAYER_VARIABLES_CAPABILITY)
            .orElseThrow(() -> new IllegalStateException("Cursed Fate variables missing on " + player.getGameProfile().getName()));
    }
}
