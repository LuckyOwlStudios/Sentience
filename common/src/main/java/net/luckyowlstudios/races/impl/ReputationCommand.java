package net.luckyowlstudios.races.impl;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.luckyowlstudios.races.api.Race;
import net.luckyowlstudios.races.api.Races;
import net.luckyowlstudios.races.api.Reputation;
import net.luckyowlstudios.races.api.ReputationCause;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

/**
 * {@code /reputation get|set|add|reset <players> [race] [value]}, for operators and testing.
 */
public final class ReputationCommand {

    private static final DynamicCommandExceptionType UNKNOWN_RACE =
            new DynamicCommandExceptionType(id -> Component.translatable("commands.races.unknown_race", String.valueOf(id)));

    private static final SuggestionProvider<CommandSourceStack> RACES = (context, builder) ->
            SharedSuggestionProvider.suggestResource(
                    Races.all(context.getSource().registryAccess()).map(holder -> holder.key().location()), builder);

    private ReputationCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("reputation")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("get")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ReputationCommand::getAll)
                                .then(Commands.argument("race", ResourceLocationArgument.id()).suggests(RACES)
                                        .executes(ReputationCommand::getOne))))
                .then(Commands.literal("set")
                        .then(Commands.argument("players", EntityArgument.players())
                                .then(Commands.argument("race", ResourceLocationArgument.id()).suggests(RACES)
                                        .then(Commands.argument("value", IntegerArgumentType.integer())
                                                .executes(context -> change(context, false))))))
                .then(Commands.literal("add")
                        .then(Commands.argument("players", EntityArgument.players())
                                .then(Commands.argument("race", ResourceLocationArgument.id()).suggests(RACES)
                                        .then(Commands.argument("amount", IntegerArgumentType.integer())
                                                .executes(context -> change(context, true))))))
                .then(Commands.literal("reset")
                        .then(Commands.argument("players", EntityArgument.players())
                                .executes(ReputationCommand::resetAll)
                                .then(Commands.argument("race", ResourceLocationArgument.id()).suggests(RACES)
                                        .executes(ReputationCommand::resetOne)))));
    }

    private static Holder.Reference<Race> race(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var id = ResourceLocationArgument.getId(context, "race");
        return Races.get(context.getSource().registryAccess(), Races.key(id)).orElseThrow(() -> UNKNOWN_RACE.create(id));
    }

    private static int getOne(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        Holder.Reference<Race> race = race(context);
        int value = Reputation.get(player, race.key());
        context.getSource().sendSuccess(() -> Component.translatable("commands.races.get",
                player.getDisplayName(), race.value().coloredName(), value), false);
        return value;
    }

    private static int getAll(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        context.getSource().sendSuccess(() -> Component.translatable("commands.races.get_all", player.getDisplayName()), false);
        Races.all(player.server.registryAccess()).forEach(race -> {
            int value = Reputation.get(player, race.key());
            context.getSource().sendSuccess(() -> Component.translatable("commands.races.get_all.entry",
                    race.value().coloredName(), value), false);
        });
        return 1;
    }

    private static int change(CommandContext<CommandSourceStack> context, boolean add) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "players");
        Holder.Reference<Race> race = race(context);
        ResourceKey<Race> key = race.key();
        int amount = IntegerArgumentType.getInteger(context, add ? "amount" : "value");
        for (ServerPlayer player : players) {
            int value = add
                    ? Reputation.add(player, key, amount, ReputationCause.COMMAND)
                    : Reputation.set(player, key, amount, ReputationCause.COMMAND);
            context.getSource().sendSuccess(() -> Component.translatable("commands.races.set",
                    player.getDisplayName(), race.value().coloredName(), value), true);
        }
        return players.size();
    }

    private static int resetOne(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "players");
        Holder.Reference<Race> race = race(context);
        for (ServerPlayer player : players) {
            Reputation.reset(player, race.key());
        }
        context.getSource().sendSuccess(() -> Component.translatable("commands.races.reset", players.size()), true);
        return players.size();
    }

    private static int resetAll(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "players");
        players.forEach(Reputation::resetAll);
        context.getSource().sendSuccess(() -> Component.translatable("commands.races.reset", players.size()), true);
        return players.size();
    }
}
