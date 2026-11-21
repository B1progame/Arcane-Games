package com.example.arcane.chess;

/** Persistent server-local rating and result counts. Ratings are updated after completed multiplayer matches. */
public record ChessPlayerStats(long elo, long wins, long losses, long draws) {
	public static final long INITIAL_ELO = 1000;
	public static final int K_FACTOR = 32;
	public static final ChessPlayerStats NEW_PLAYER = new ChessPlayerStats(INITIAL_ELO, 0, 0, 0);

	public ChessPlayerStats {
		if (elo < 0 || wins < 0 || losses < 0 || draws < 0) throw new IllegalArgumentException("Chess stats cannot be negative");
		Math.addExact(Math.addExact(wins, losses), draws);
	}

	public long games() { return wins + losses + draws; }

	public static RatingUpdate rate(ChessPlayerStats first, ChessPlayerStats second, double firstScore) {
		if (firstScore < 0.0 || firstScore > 1.0 || !Double.isFinite(firstScore)) throw new IllegalArgumentException("Invalid match score");
		double expectedFirst = 1.0 / (1.0 + Math.pow(10.0, (second.elo - first.elo) / 400.0));
		long deltaFirst = Math.round(K_FACTOR * (firstScore - expectedFirst));
		long deltaSecond = -deltaFirst;
		return new RatingUpdate(Math.max(0, first.elo + deltaFirst), Math.max(0, second.elo + deltaSecond));
	}

	public ChessPlayerStats withResult(long nextElo, Result result) {
		return switch (result) {
			case WIN -> new ChessPlayerStats(nextElo, wins + 1, losses, draws);
			case LOSS -> new ChessPlayerStats(nextElo, wins, losses + 1, draws);
			case DRAW -> new ChessPlayerStats(nextElo, wins, losses, draws + 1);
		};
	}

	public enum Result { WIN, LOSS, DRAW }
	public record RatingUpdate(long firstElo, long secondElo) {}
}
