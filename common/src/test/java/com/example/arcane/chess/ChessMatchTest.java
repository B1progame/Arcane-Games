package com.example.arcane.chess;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ChessMatchTest {
	@Test
	void timeoutResultIsDrawWhenRemainingPlayerHasOnlyTwoKnightsAgainstBareKing() {
		ChessPosition position = ChessPosition.fromFen("7k/8/8/8/8/8/8/NN3K2 w - - 0 1");
		assertEquals(ChessMatch.Status.DRAW, ChessMatch.timeoutResult(position, Side.WHITE));
		position = ChessPosition.fromFen("7k/8/8/8/8/8/4p3/NN3K2 w - - 0 1");
		assertEquals(ChessMatch.Status.BLACK_WON, ChessMatch.timeoutResult(position, Side.WHITE));
	}

	@Test
	void appliesServerClockIncrementAndAwardsTimeoutWithoutClientAction() {
		UUID white = UUID.randomUUID(), black = UUID.randomUUID();
		ChessMatch match = ChessMatch.create(white, 10_000, 1_000);
		assertTrue(match.join(black, 100));
		assertFalse(match.play(white, 0, "e2", "e5", null, 4_100));
		assertTrue(match.play(white, 0, "e2", "e4", null, 5_100));
		assertEquals(6_000, match.clockRemainingMillis(Side.WHITE, 5_100));
		assertEquals(10_000, match.clockRemainingMillis(Side.BLACK, 5_100));
		assertTrue(match.updateClock(15_100));
		assertEquals(ChessMatch.Status.WHITE_WON, match.status());
	}

	@Test
	void savedTimedMatchRestoresPausedAndResumesTheSavedSide() {
		UUID white = UUID.randomUUID(), black = UUID.randomUUID();
		ChessMatch match = ChessMatch.create(white, 60_000, 0);
		assertTrue(match.join(black, 100));
		assertTrue(match.play(white, 0, "e2", "e4", null, 2_100));
		ChessMatch restored = ChessMatch.restore(match.snapshot(5_000));
		assertEquals(57_100, restored.clockRemainingMillis(Side.BLACK, 500_000));
		assertTrue(restored.resumeClock(500_000));
		assertTrue(restored.updateClock(557_100));
		assertEquals(ChessMatch.Status.WHITE_WON, restored.status());
	}

	@Test
	void assignsPlayersAndRejectsOutOfTurnOrUnauthorizedMoves() {
		UUID white = UUID.randomUUID(), black = UUID.randomUUID(), stranger = UUID.randomUUID();
		ChessMatch match = ChessMatch.create(white);
		assertEquals(ChessMatch.Status.WAITING, match.status());
		assertFalse(match.play(white, 0, "e2", "e4", null));
		assertTrue(match.join(black));
		assertFalse(match.join(stranger));
		assertFalse(match.play(black, 0, "e7", "e5", null));
		assertFalse(match.play(stranger, 0, "e2", "e4", null));
		assertFalse(match.play(white, 0, "e2", "e5", null));
		assertTrue(match.play(white, 0, "e2", "e4", null));
		assertFalse(match.play(black, 0, "e7", "e5", null));
		assertTrue(match.play(black, 1, "e7", "e5", null));
	}

	@Test
	void restoresPositionHistoryAndDrawOfferFromSnapshot() {
		UUID white = UUID.randomUUID(), black = UUID.randomUUID();
		ChessMatch match = ChessMatch.create(white);
		assertTrue(match.join(black));
		assertTrue(match.play(white, 0, "e2", "e4", null));
		assertTrue(match.offerDraw(black));
		ChessMatch restored = ChessMatch.restore(match.snapshot());
		assertEquals(match.fen(), restored.fen());
		assertEquals(match.history(), restored.history());
		assertEquals(black, restored.drawOfferer());
		assertTrue(restored.acceptDraw(white));
		assertEquals(ChessMatch.Status.DRAW, restored.status());
	}

	@Test
	void rejectsSavedInProgressMatchWhoseMovesAlreadyEndedInCheckmate() {
		UUID white = UUID.randomUUID(), black = UUID.randomUUID();
		ChessMatch finished = ChessMatch.create(white);
		assertTrue(finished.join(black));
		assertTrue(finished.play(white, 0, "e2", "e4", null));
		assertTrue(finished.play(black, 1, "e7", "e5", null));
		assertTrue(finished.play(white, 2, "d1", "h5", null));
		assertTrue(finished.play(black, 3, "b8", "c6", null));
		assertTrue(finished.play(white, 4, "f1", "c4", null));
		assertTrue(finished.play(black, 5, "g8", "f6", null));
		assertTrue(finished.play(white, 6, "h5", "f7", null));
		ChessMatch.Snapshot valid = finished.snapshot();
		ChessMatch.Snapshot inconsistent = new ChessMatch.Snapshot(valid.id(), valid.whitePlayer(), valid.blackPlayer(),
				ChessMatch.Status.IN_PROGRESS, valid.fen(), valid.moves(), null);
		assertThrows(IllegalArgumentException.class, () -> ChessMatch.restore(inconsistent));
	}

	@Test
	void cannotReplaceAnOpponentDrawOfferInsteadOfAcceptingIt() {
		UUID white = UUID.randomUUID(), black = UUID.randomUUID();
		ChessMatch match = ChessMatch.create(white);
		assertTrue(match.join(black));
		assertTrue(match.offerDraw(white));
		assertFalse(match.offerDraw(black));
		assertEquals(white, match.drawOfferer());
		assertTrue(match.acceptDraw(black));
		assertEquals(ChessMatch.Status.DRAW, match.status());
	}

	@Test
	void recordsResignationAndCheckmateResults() {
		UUID white = UUID.randomUUID(), black = UUID.randomUUID();
		ChessMatch match = ChessMatch.create(white);
		assertTrue(match.join(black));
		assertTrue(match.resign(white));
		assertEquals(ChessMatch.Status.BLACK_WON, match.status());
		assertFalse(match.resign(black));

		match = ChessMatch.create(white);
		assertTrue(match.join(black));
		assertTrue(match.play(white, match.ply(), "f2", "f3", null));
		assertTrue(match.play(black, match.ply(), "e7", "e5", null));
		assertTrue(match.play(white, match.ply(), "g2", "g4", null));
		assertTrue(match.play(black, match.ply(), "d8", "h4", null));
		assertEquals(ChessMatch.Status.BLACK_WON, match.status());
	}
}
