package com.example.arcane.network;

import com.example.arcane.Arcane;
import com.example.arcane.chess.ArcaneChessSavedData;
import com.example.arcane.chess.ChessMatch;
import com.example.arcane.chess.Piece;
import com.example.arcane.block.GameBoardBlockEntity;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ChessNetworking {
	private static final Map<UUID, Integer> LAST_ACTION_TICK = new HashMap<>();
	private static final Map<UUID, UUID> SPECTATORS = new HashMap<>();
	private static final Map<UUID, GameBoardBlockEntity> BOARDS = new HashMap<>();
	private ChessNetworking() {}

	public static void initialize() {
		PayloadTypeRegistry.serverboundPlay().register(ChessActionPayload.TYPE, ChessActionPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(MatchStatePayload.TYPE, MatchStatePayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(MatchClockPayload.TYPE, MatchClockPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(OpenPracticePayload.TYPE, OpenPracticePayload.CODEC);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			LAST_ACTION_TICK.remove(handler.player.getUUID());
			SPECTATORS.remove(handler.player.getUUID());
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			LAST_ACTION_TICK.clear();
			SPECTATORS.clear();
			BOARDS.clear();
		});
		ServerChunkEvents.CHUNK_LOAD.register((world, chunk, blockEntityTagUpdated) -> {
			for (BlockEntity entity : chunk.getBlockEntities().values()) {
				if (!(entity instanceof GameBoardBlockEntity board) || board.matchId() == null) continue;
				ChessMatch match = ArcaneChessSavedData.get(world.getServer()).get(board.matchId());
				if (match != null) trackBoard(board, match);
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(ChessActionPayload.TYPE, (payload, context) -> handle(context.player(), payload));
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			long now = System.nanoTime() / 1_000_000L;
			ArcaneChessSavedData saved = ArcaneChessSavedData.get(server);
			boolean hasTimedMatches = false;
			for (ChessMatch match : saved.all()) {
				if (!match.hasClock() || match.status() != ChessMatch.Status.IN_PROGRESS) continue;
				hasTimedMatches = true;
				match.resumeClock(now);
				if (match.updateClock(now)) {
					saved.recordCompletedMatch(match);
					saved.markChanged();
					ChessNetworking.sync(server, match, false, false);
				} else if (server.getTickCount() % 20 == 0) {
					ChessNetworking.syncClock(server, match, now);
				}
			}
			if (hasTimedMatches && server.getTickCount() % 20 == 0) saved.markChanged();
		});
	}

	public static void openFor(ServerPlayer player, ChessMatch match) {
		MinecraftServer server = player.level().getServer();
		if (server == null) return;
		match.resumeClock(System.nanoTime() / 1_000_000L);
		if (match.isParticipant(player.getUUID())) SPECTATORS.remove(player.getUUID());
		else SPECTATORS.put(player.getUUID(), match.id());
		send(player, match, true);
		UUID opponentId = player.getUUID().equals(match.whitePlayer()) ? match.blackPlayer() : match.whitePlayer();
		if (opponentId != null) {
			ServerPlayer opponent = server.getPlayerList().getPlayer(opponentId);
			if (opponent != null) send(opponent, match, false);
		}
	}

	public static void trackBoard(GameBoardBlockEntity board, ChessMatch match) {
		BOARDS.put(match.id(), board);
		board.setMatch(match.id(), match.fen());
	}

	public static void sync(MinecraftServer server, ChessMatch match, boolean openForWhite, boolean openForBlack) {
		syncBoard(match);
		ServerPlayer white = server.getPlayerList().getPlayer(match.whitePlayer());
		if (white != null) send(white, match, openForWhite);
		if (match.blackPlayer() != null) {
			ServerPlayer black = server.getPlayerList().getPlayer(match.blackPlayer());
			if (black != null) send(black, match, openForBlack);
		}
		for (Map.Entry<UUID, UUID> entry : Map.copyOf(SPECTATORS).entrySet()) {
			if (!entry.getValue().equals(match.id())) continue;
			ServerPlayer spectator = server.getPlayerList().getPlayer(entry.getKey());
			if (spectator == null) SPECTATORS.remove(entry.getKey());
			else send(spectator, match, false);
		}
	}

	private static void syncClock(MinecraftServer server, ChessMatch match, long nowMillis) {
		MatchClockPayload payload = new MatchClockPayload(match.id(), match.clockRemainingMillis(com.example.arcane.chess.Side.WHITE, nowMillis),
				match.clockRemainingMillis(com.example.arcane.chess.Side.BLACK, nowMillis));
		ServerPlayer white = server.getPlayerList().getPlayer(match.whitePlayer());
		if (white != null) ServerPlayNetworking.send(white, payload);
		if (match.blackPlayer() != null) {
			ServerPlayer black = server.getPlayerList().getPlayer(match.blackPlayer());
			if (black != null) ServerPlayNetworking.send(black, payload);
		}
		for (Map.Entry<UUID, UUID> entry : Map.copyOf(SPECTATORS).entrySet()) {
			if (!entry.getValue().equals(match.id())) continue;
			ServerPlayer spectator = server.getPlayerList().getPlayer(entry.getKey());
			if (spectator != null) ServerPlayNetworking.send(spectator, payload);
			else SPECTATORS.remove(entry.getKey());
		}
	}

	private static void syncBoard(ChessMatch match) {
		GameBoardBlockEntity board = BOARDS.get(match.id());
		if (board == null) return;
		if (board.isRemoved() || board.getLevel() == null || board.getLevel().getBlockEntity(board.getBlockPos()) != board) {
			BOARDS.remove(match.id(), board);
			return;
		}
		board.setGameState(match.fen());
	}

	private static void send(ServerPlayer player, ChessMatch match, boolean open) {
		ArcaneChessSavedData saved = ArcaneChessSavedData.get(player.level().getServer());
		int whiteElo = (int) Math.min(1_000_000, saved.stats(match.whitePlayer()).elo());
		int blackElo = match.blackPlayer() == null ? 0
				: (int) Math.min(1_000_000, saved.stats(match.blackPlayer()).elo());
		ServerPlayNetworking.send(player, MatchStatePayload.from(match, player.getUUID(), open, whiteElo, blackElo));
	}

	private static void handle(ServerPlayer player, ChessActionPayload payload) {
		MinecraftServer server = player.level().getServer();
		if (server == null) return;
		int tick = server.getTickCount();
		Integer previous = LAST_ACTION_TICK.put(player.getUUID(), tick);
		if (previous != null && previous == tick) return;
		ArcaneChessSavedData saved = ArcaneChessSavedData.get(server);
		ChessMatch match = saved.get(payload.matchId());
		if (match == null || !match.isParticipant(player.getUUID())) return;
		long now = System.nanoTime() / 1_000_000L;
		if (match.updateClock(now)) {
			saved.recordCompletedMatch(match);
			saved.markChanged();
			ChessNetworking.sync(server, match, false, false);
			return;
		}
		if (payload.expectedPly() != match.ply()) {
			send(player, match, false);
			return;
		}
		ChessMatch.Status statusBefore = match.status();
		boolean accepted = switch (payload.action()) {
			case ChessActionPayload.MOVE -> match.play(player.getUUID(), payload.expectedPly(), payload.from(), payload.to(), piece(payload.promotion()), now);
			case ChessActionPayload.RESIGN -> match.resign(player.getUUID(), now);
			case ChessActionPayload.OFFER_DRAW -> match.offerDraw(player.getUUID(), now);
			case ChessActionPayload.ACCEPT_DRAW -> match.acceptDraw(player.getUUID(), now);
			default -> false;
		};
		if (accepted) {
			if ((statusBefore == ChessMatch.Status.WAITING || statusBefore == ChessMatch.Status.IN_PROGRESS)
					&& match.status() != ChessMatch.Status.WAITING && match.status() != ChessMatch.Status.IN_PROGRESS) {
				saved.recordCompletedMatch(match);
			}
			saved.markChanged();
			saved.pruneFinished();
			ChessNetworking.sync(server, match, false, false);
		} else send(player, match, false);
	}

	private static Piece piece(int code) {
		return switch (code) { case 1 -> Piece.QUEEN; case 2 -> Piece.ROOK; case 3 -> Piece.BISHOP; case 4 -> Piece.KNIGHT; default -> null; };
	}
}
