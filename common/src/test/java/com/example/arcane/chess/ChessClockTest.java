package com.example.arcane.chess;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChessClockTest {
	@Test
	void subtractsTurnTimeAndAddsIncrementOnlyAfterLegalMoveCompletes() {
		ChessClock clock = ChessClock.create(60_000, 2_000);
		clock.start(Side.WHITE, 10_000);
		assertEquals(45_000, clock.remainingMillis(Side.WHITE, 25_000));
		assertTrue(clock.completeTurn(Side.WHITE, Side.BLACK, 25_000));
		assertEquals(47_000, clock.remainingMillis(Side.WHITE, 25_000));
		assertEquals(60_000, clock.remainingMillis(Side.BLACK, 25_000));
		assertEquals(58_000, clock.remainingMillis(Side.BLACK, 27_000));
	}

	@Test
	void flagsTheRunningSideAtZeroAndPausesOnCheckpoint() {
		ChessClock clock = ChessClock.create(1_000, 0);
		clock.start(Side.BLACK, 5_000);
		assertNull(clock.flaggedSide(5_999));
		assertEquals(Side.BLACK, clock.flaggedSide(6_000));
		assertTrue(clock.checkpoint(5_500));
		assertEquals(500, clock.remainingMillis(Side.BLACK, 20_000));
		assertNull(clock.flaggedSide(20_000));
	}

	@Test
	void restoreKeepsRemainingTimeButStartsPausedUntilServerResumesIt() {
		ChessClock clock = ChessClock.create(60_000, 0);
		clock.start(Side.WHITE, 0);
		assertTrue(clock.checkpoint(15_000));
		ChessClock restored = ChessClock.restore(clock.snapshot(500_000));
		assertEquals(45_000, restored.remainingMillis(Side.WHITE, 500_000));
		assertNull(restored.flaggedSide(500_000));
		restored.start(Side.WHITE, 500_000);
		assertEquals(Side.WHITE, restored.flaggedSide(545_000));
	}
}
