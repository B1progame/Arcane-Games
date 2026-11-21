package com.example.arcane.chess;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChessPlayerStatsTest {
	@Test
	void equalRatedWinMovesRatingsBySixteenPoints() {
		var update = ChessPlayerStats.rate(ChessPlayerStats.NEW_PLAYER, ChessPlayerStats.NEW_PLAYER, 1.0);
		assertEquals(1016, update.firstElo());
		assertEquals(984, update.secondElo());
	}

	@Test
	void equalRatedDrawLeavesRatingsUnchanged() {
		var update = ChessPlayerStats.rate(ChessPlayerStats.NEW_PLAYER, ChessPlayerStats.NEW_PLAYER, 0.5);
		assertEquals(1000, update.firstElo());
		assertEquals(1000, update.secondElo());
	}

	@Test
	void resultCountersAccumulate() {
		var stats = ChessPlayerStats.NEW_PLAYER.withResult(1016, ChessPlayerStats.Result.WIN)
				.withResult(1000, ChessPlayerStats.Result.DRAW)
				.withResult(984, ChessPlayerStats.Result.LOSS);
		assertEquals(1, stats.wins());
		assertEquals(1, stats.draws());
		assertEquals(1, stats.losses());
		assertEquals(3, stats.games());
	}
}
