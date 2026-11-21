package com.example.arcane.chess;

import com.example.arcane.Arcane;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.*;

/** World-scoped, server-authoritative match store. All calls must run on the server thread. */
public final class ArcaneChessSavedData extends SavedData {
	private static final int MAX_ACTIVE_MATCHES = 128;
	private static final int MAX_FINISHED_MATCHES = 512;
	private static final Codec<UUID> UUID_CODEC = Codec.STRING.comapFlatMap(value -> {
		try { return DataResult.success(UUID.fromString(value)); }
		catch (IllegalArgumentException invalid) { return DataResult.error(() -> "Invalid UUID"); }
	}, UUID::toString);
	private static final Codec<ChessMatch.Status> STATUS_CODEC = Codec.STRING.comapFlatMap(value -> {
		try { return DataResult.success(ChessMatch.Status.valueOf(value)); }
		catch (IllegalArgumentException invalid) { return DataResult.error(() -> "Invalid chess match status"); }
	}, ChessMatch.Status::name);
	private static final Codec<ChessMatch> MATCH_CODEC = RecordCodecBuilder.create(instance -> instance.group(
			UUID_CODEC.fieldOf("id").forGetter(ChessMatch::id),
			UUID_CODEC.fieldOf("white_player").forGetter(ChessMatch::whitePlayer),
			UUID_CODEC.optionalFieldOf("black_player").forGetter(match -> Optional.ofNullable(match.blackPlayer())),
			STATUS_CODEC.fieldOf("status").forGetter(ChessMatch::status),
			Codec.STRING.fieldOf("fen").forGetter(ChessMatch::fen),
			Codec.STRING.listOf().fieldOf("moves").forGetter(match -> match.history().stream().map(ChessMove::toString).toList()),
			UUID_CODEC.optionalFieldOf("draw_offerer").forGetter(match -> Optional.ofNullable(match.drawOfferer())),
			Codec.LONG.optionalFieldOf("clock_white_ms", -1L).forGetter(ChessMatch::savedWhiteMillis),
			Codec.LONG.optionalFieldOf("clock_black_ms", -1L).forGetter(ChessMatch::savedBlackMillis),
			Codec.LONG.optionalFieldOf("clock_increment_ms", -1L).forGetter(ChessMatch::clockIncrementMillis)
	).apply(instance, (id, white, black, status, fen, moves, offerer, clockWhite, clockBlack, clockIncrement) -> ChessMatch.restore(
			new ChessMatch.Snapshot(id, white, black.orElse(null), status, fen, moves, offerer.orElse(null), clockWhite, clockBlack, clockIncrement)
	)));
	private static final Codec<ChessPlayerStats> PLAYER_STATS_CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.LONG.fieldOf("elo").forGetter(ChessPlayerStats::elo),
			Codec.LONG.fieldOf("wins").forGetter(ChessPlayerStats::wins),
			Codec.LONG.fieldOf("losses").forGetter(ChessPlayerStats::losses),
			Codec.LONG.fieldOf("draws").forGetter(ChessPlayerStats::draws)
	).apply(instance, ChessPlayerStats::new));
	private static final Codec<PlayerStatsEntry> PLAYER_STATS_ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
			UUID_CODEC.fieldOf("player").forGetter(PlayerStatsEntry::player),
			PLAYER_STATS_CODEC.fieldOf("stats").forGetter(PlayerStatsEntry::stats)
	).apply(instance, PlayerStatsEntry::new));
	static final Codec<ArcaneChessSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			MATCH_CODEC.listOf().fieldOf("matches").forGetter(data -> List.copyOf(data.matches.values())),
			PLAYER_STATS_ENTRY_CODEC.listOf().optionalFieldOf("player_stats", List.of())
					.forGetter(data -> data.playerStats.entrySet().stream().map(entry -> new PlayerStatsEntry(entry.getKey(), entry.getValue())).toList())
	).apply(instance, ArcaneChessSavedData::new));
	private static final SavedDataType<ArcaneChessSavedData> TYPE = new SavedDataType<>(
			Arcane.id("chess_matches"), ArcaneChessSavedData::new, CODEC, null
	);

	private final Map<UUID, ChessMatch> matches = new LinkedHashMap<>();
	private final Map<UUID, ChessPlayerStats> playerStats = new HashMap<>();

	public ArcaneChessSavedData() {}
	private ArcaneChessSavedData(List<ChessMatch> loaded, List<PlayerStatsEntry> loadedStats) {
		if (loaded.size() > MAX_ACTIVE_MATCHES + MAX_FINISHED_MATCHES) throw new IllegalArgumentException("Too many saved matches");
		for (ChessMatch match : loaded) {
			if (matches.putIfAbsent(match.id(), match) != null) throw new IllegalArgumentException("Duplicate saved match id");
		}
		if (loadedStats.size() > 100_000) throw new IllegalArgumentException("Too many saved player-stat records");
		for (PlayerStatsEntry entry : loadedStats) {
			if (playerStats.putIfAbsent(entry.player(), entry.stats()) != null) throw new IllegalArgumentException("Duplicate saved player-stat record");
		}
	}

	public static ArcaneChessSavedData get(MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(TYPE);
	}

	public ChessMatch create(UUID owner) {
		long active = matches.values().stream().filter(match -> match.status() == ChessMatch.Status.WAITING || match.status() == ChessMatch.Status.IN_PROGRESS).count();
		if (active >= MAX_ACTIVE_MATCHES) throw new IllegalStateException("The server has reached its active chess match limit");
		ChessMatch match = ChessMatch.create(owner);
		matches.put(match.id(), match);
		setDirty();
		pruneFinished();
		return match;
	}

	public ChessMatch create(UUID owner, long initialMillis, long incrementMillis) {
		long active = matches.values().stream().filter(match -> match.status() == ChessMatch.Status.WAITING || match.status() == ChessMatch.Status.IN_PROGRESS).count();
		if (active >= MAX_ACTIVE_MATCHES) throw new IllegalStateException("The server has reached its active chess match limit");
		ChessMatch match = ChessMatch.create(owner, initialMillis, incrementMillis);
		matches.put(match.id(), match);
		setDirty();
		pruneFinished();
		return match;
	}

	public ChessMatch get(UUID id) { return matches.get(id); }
	public Collection<ChessMatch> all() { return List.copyOf(matches.values()); }
	public ChessPlayerStats stats(UUID player) { return playerStats.getOrDefault(player, ChessPlayerStats.NEW_PLAYER); }

	/** Record a terminal two-player match exactly once, at its active-to-finished transition. */
	public void recordCompletedMatch(ChessMatch match) {
		if (match.blackPlayer() == null) throw new IllegalArgumentException("A rated match needs two players");
		ChessPlayerStats white = stats(match.whitePlayer());
		ChessPlayerStats black = stats(match.blackPlayer());
		if (match.status() == ChessMatch.Status.DRAW) {
			ChessPlayerStats.RatingUpdate ratings = ChessPlayerStats.rate(white, black, 0.5);
			playerStats.put(match.whitePlayer(), white.withResult(ratings.firstElo(), ChessPlayerStats.Result.DRAW));
			playerStats.put(match.blackPlayer(), black.withResult(ratings.secondElo(), ChessPlayerStats.Result.DRAW));
		} else if (match.status() == ChessMatch.Status.WHITE_WON || match.status() == ChessMatch.Status.BLACK_WON) {
			boolean whiteWon = match.status() == ChessMatch.Status.WHITE_WON;
			ChessPlayerStats.RatingUpdate ratings = ChessPlayerStats.rate(white, black, whiteWon ? 1.0 : 0.0);
			playerStats.put(match.whitePlayer(), white.withResult(ratings.firstElo(), whiteWon ? ChessPlayerStats.Result.WIN : ChessPlayerStats.Result.LOSS));
			playerStats.put(match.blackPlayer(), black.withResult(ratings.secondElo(), whiteWon ? ChessPlayerStats.Result.LOSS : ChessPlayerStats.Result.WIN));
		} else {
			throw new IllegalArgumentException("Match is not finished");
		}
		setDirty();
	}
	public void markChanged() { setDirty(); }

	public void pruneFinished() {
		List<ChessMatch> finished = matches.values().stream()
				.filter(match -> match.status() != ChessMatch.Status.WAITING && match.status() != ChessMatch.Status.IN_PROGRESS)
				.toList();
		if (finished.size() <= MAX_FINISHED_MATCHES) return;
		for (int i = 0; i < finished.size() - MAX_FINISHED_MATCHES; i++) matches.remove(finished.get(i).id());
		setDirty();
	}

	private record PlayerStatsEntry(UUID player, ChessPlayerStats stats) {}
}
