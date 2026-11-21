package com.example.arcane.network;

import com.example.arcane.chess.ArcaneChessSavedData;
import com.example.arcane.chess.ChessMatch;
import com.example.arcane.chess.ChessPlayerStats;
import com.example.arcane.chess.ChessPgn;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.UuidArgument;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.storage.LevelResource;

import java.util.UUID;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;

public final class ChessCommands {
	private ChessCommands() {}

	public static void initialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			LiteralArgumentBuilder<CommandSourceStack> chess = Commands.literal("chess");
			chess.then(Commands.literal("create").executes(context -> create(context, 0))
					.then(Commands.argument("minutes", IntegerArgumentType.integer(1, 60)).executes(context -> create(context, IntegerArgumentType.getInteger(context, "minutes")))));
			chess.then(Commands.literal("list").executes(ChessCommands::list));
			chess.then(Commands.literal("join").then(Commands.argument("match_id", UuidArgument.uuid()).executes(ChessCommands::join)));
			chess.then(Commands.literal("resume").then(Commands.argument("match_id", UuidArgument.uuid()).executes(ChessCommands::resume)));
			chess.then(Commands.literal("spectate").then(Commands.argument("match_id", UuidArgument.uuid()).executes(ChessCommands::spectate)));
			chess.then(Commands.literal("stats").executes(ChessCommands::stats));
			chess.then(Commands.literal("pgn").requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
					.then(Commands.argument("match_id", UuidArgument.uuid()).executes(ChessCommands::exportPgn)));
			chess.then(Commands.literal("practice").executes(context -> {
				ServerPlayer player = context.getSource().getPlayerOrException();
				if (!context.getSource().getServer().isSingleplayer()) {
					context.getSource().sendFailure(Component.literal("Computer practice is currently local-only. Start it from a singleplayer world."));
					return 0;
				}
				ServerPlayNetworking.send(player, new OpenPracticePayload());
				return 1;
			}));
			dispatcher.register(chess);
		});
	}

	private static int create(CommandContext<CommandSourceStack> context, int minutes) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		ArcaneChessSavedData saved = ArcaneChessSavedData.get(context.getSource().getServer());
		ChessMatch match;
		try {
			match = minutes == 0 ? saved.create(player.getUUID()) : saved.create(player.getUUID(), minutes * 60_000L, 0);
		} catch (IllegalStateException limitReached) {
			context.getSource().sendFailure(Component.literal("The server currently has the maximum number of active chess matches."));
			return 0;
		}
		ChessNetworking.openFor(player, match);
		context.getSource().sendSuccess(() -> Component.literal((minutes == 0 ? "Untimed" : minutes + " minute") + " chess match created. Share this ID: " + match.id()
				+ " — a player can run /chess join " + match.id()), false);
		return 1;
	}

	private static int list(CommandContext<CommandSourceStack> context) {
		List<ChessMatch> waiting = ArcaneChessSavedData.get(context.getSource().getServer()).all().stream()
				.filter(match -> match.status() == ChessMatch.Status.WAITING)
				.limit(10)
				.toList();
		if (waiting.isEmpty()) {
			context.getSource().sendSuccess(() -> Component.literal("No open chess matches. Create one with /chess create or use an Arcane Tome."), false);
			return 0;
		}
		context.getSource().sendSuccess(() -> Component.literal("Open chess matches (join one with /chess join <ID>):"), false);
		for (ChessMatch match : waiting) {
			context.getSource().sendSuccess(() -> Component.literal(match.id().toString()), false);
		}
		return waiting.size();
	}

	private static int join(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		UUID id = context.getArgument("match_id", UUID.class);
		ArcaneChessSavedData saved = ArcaneChessSavedData.get(context.getSource().getServer());
		ChessMatch match = saved.get(id);
		if (match == null || !match.join(player.getUUID())) {
			context.getSource().sendFailure(Component.literal("That chess match is unavailable or already has two players."));
			return 0;
		}
		saved.markChanged();
		saved.pruneFinished();
		ChessNetworking.sync(context.getSource().getServer(), match, false, false);
		ChessNetworking.openFor(player, match);
		context.getSource().sendSuccess(() -> Component.literal("Joined chess match " + id), false);
		return 1;
	}

	private static int resume(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		UUID id = context.getArgument("match_id", UUID.class);
		ChessMatch match = ArcaneChessSavedData.get(context.getSource().getServer()).get(id);
		if (match == null || !match.isParticipant(player.getUUID())) {
			context.getSource().sendFailure(Component.literal("You are not a player in that chess match."));
			return 0;
		}
		ChessNetworking.openFor(player, match);
		context.getSource().sendSuccess(() -> Component.literal("Resumed chess match " + id), false);
		return 1;
	}

	private static int spectate(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		UUID id = context.getArgument("match_id", UUID.class);
		ChessMatch match = ArcaneChessSavedData.get(context.getSource().getServer()).get(id);
		if (match == null) {
			context.getSource().sendFailure(Component.literal("That chess match is unavailable."));
			return 0;
		}
		ChessNetworking.openFor(player, match);
		context.getSource().sendSuccess(() -> Component.literal("Watching chess match " + id), false);
		return 1;
	}

	private static int stats(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		ChessPlayerStats stats = ArcaneChessSavedData.get(context.getSource().getServer()).stats(player.getUUID());
		context.getSource().sendSuccess(() -> Component.literal("Arcane Chess | Elo " + stats.elo()
				+ " | " + stats.wins() + " wins, " + stats.draws() + " draws, " + stats.losses()
				+ " losses | " + stats.games() + " rated games"), false);
		return 1;
	}

	private static int exportPgn(CommandContext<CommandSourceStack> context) {
		UUID id = context.getArgument("match_id", UUID.class);
		ChessMatch match = ArcaneChessSavedData.get(context.getSource().getServer()).get(id);
		if (match == null) {
			context.getSource().sendFailure(Component.literal("That chess match is unavailable."));
			return 0;
		}
		Path replayDirectory = context.getSource().getServer().getWorldPath(LevelResource.ROOT).resolve("arcane-games").resolve("replays");
		Path replay = replayDirectory.resolve(id + ".pgn");
		try {
			Files.createDirectories(replayDirectory);
			Files.writeString(replay, ChessPgn.export(match), StandardCharsets.UTF_8);
		} catch (IOException failure) {
			context.getSource().sendFailure(Component.literal("Could not save the PGN replay to the world folder."));
			return 0;
		}
		context.getSource().sendSuccess(() -> Component.literal("Saved PGN replay: " + replay), false);
		return 1;
	}
}
