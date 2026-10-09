package com.mdvlcraft.binder.command;

import com.mdvlcraft.binder.ability.Ability;
import com.mdvlcraft.binder.ability.AbilityActions;
import com.mdvlcraft.binder.ability.AbilityGrants;
import com.mdvlcraft.binder.ability.Loadout;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
    modid = "mdvlcraft"
)
public final class MdvlCommand {
    private MdvlCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_(
                                            "mdvl"
                                        )
                                        .requires(source -> source.m_6761_(2)))
                                    .then(
                                        Commands.m_82127_("abilities")
                                            .then(Commands.m_82129_("player", EntityArgument.m_91466_()).executes(MdvlCommand::abilities))
                                    ))
                                .then(
                                    Commands.m_82127_("assign")
                                        .then(
                                            Commands.m_82129_("player", EntityArgument.m_91466_())
                                                .then(
                                                    Commands.m_82129_("slot", IntegerArgumentType.integer(1, 8))
                                                        .then(Commands.m_82129_("ability", ResourceLocationArgument.m_106984_()).executes(MdvlCommand::assign))
                                                )
                                        )
                                ))
                            .then(
                                Commands.m_82127_("clear")
                                    .then(
                                        Commands.m_82129_("player", EntityArgument.m_91466_())
                                            .then(Commands.m_82129_("slot", IntegerArgumentType.integer(1, 8)).executes(MdvlCommand::clear))
                                    )
                            ))
                        .then(
                            Commands.m_82127_("cast")
                                .then(
                                    Commands.m_82129_("player", EntityArgument.m_91466_())
                                        .then(Commands.m_82129_("slot", IntegerArgumentType.integer(1, 8)).executes(MdvlCommand::cast))
                                )
                        ))
                    .then(Commands.m_82127_("release").then(Commands.m_82129_("player", EntityArgument.m_91466_()).executes(MdvlCommand::release))))
                .then(Commands.m_82127_("weapons").executes(MdvlCommand::weapons))
        );
    }

    private static int weapons(CommandContext<CommandSourceStack> context) {
        List<WeaponReport.Row> rows = WeaponReport.build();
        Path file = ((CommandSourceStack)context.getSource()).m_81377_().m_6237_().toPath().resolve("weapons.tsv");
        List<String> lines = new ArrayList<>(List.of("item\tsupported\tcategory\tweapon_type\tclass"));
        rows.forEach(
            row -> lines.add(String.join("\t", row.id().toString(), String.valueOf(row.supported()), row.category(), row.weaponType(), row.itemClass()))
        );

        try {
            Files.write(file, lines);
        } catch (IOException var6) {
            throw new UncheckedIOException(var6);
        }

        long unsupported = rows.stream().filter(row -> !row.supported()).count();
        ((CommandSourceStack)context.getSource())
            .m_288197_(() -> Component.m_237113_(rows.size() + " weapons, " + unsupported + " without Epic Fight support; written to " + file), false);
        return (int)unsupported;
    }

    private static int abilities(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.m_91474_(context, "player");
        String granted = AbilityGrants.of(player)
            .entrySet()
            .stream()
            .map(entry -> entry.getKey().id() + " " + entry.getValue())
            .collect(Collectors.joining(", "));
        String wheel = Loadout.get(player).stream().map(slot -> slot.map(ResourceLocation::toString).orElse("-")).collect(Collectors.joining(", "));
        int mana = Math.round(MagicData.getPlayerMagicData(player).getMana());
        int maxMana = (int)player.m_21133_((Attribute)AttributeRegistry.MAX_MANA.get());
        ((CommandSourceStack)context.getSource())
            .m_288197_(() -> Component.m_237113_("Granted: [" + granted + "] Wheel: [" + wheel + "] Mana: " + mana + "/" + maxMana), false);
        return AbilityGrants.of(player).size();
    }

    private static int assign(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.m_91474_(context, "player");
        ResourceLocation id = ResourceLocationArgument.m_107011_(context, "ability");
        if (Ability.byId(id).isEmpty()) {
            ((CommandSourceStack)context.getSource()).m_81352_(Component.m_237113_("Unknown ability " + id));
            return 0;
        } else if (!AbilityActions.assign(player, IntegerArgumentType.getInteger(context, "slot") - 1, Optional.of(id))) {
            ((CommandSourceStack)context.getSource()).m_81352_(Component.m_237113_(player.m_36316_().getName() + " has not unlocked " + id));
            return 0;
        } else {
            ((CommandSourceStack)context.getSource()).m_288197_(() -> Component.m_237113_("Assigned " + id), false);
            return 1;
        }
    }

    private static int clear(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        AbilityActions.assign(EntityArgument.m_91474_(context, "player"), IntegerArgumentType.getInteger(context, "slot") - 1, Optional.empty());
        return 1;
    }

    private static int cast(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        AbilityActions.press(EntityArgument.m_91474_(context, "player"), IntegerArgumentType.getInteger(context, "slot") - 1);
        return 1;
    }

    private static int release(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        AbilityActions.release(EntityArgument.m_91474_(context, "player"));
        return 1;
    }
}
