package com.example.arcane.chess;

import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArcaneChessSavedDataTest {
	@Test
	void persistsTimedMatchRemainderAndRestoresItsClockPaused() {
		UUID white = UUID.randomUUID(), black = UUID.randomUUID();
		ArcaneChessSavedData before = new ArcaneChessSavedData();
		ChessMatch match = before.create(white, 60_000, 1_000);
		assertTrue(match.join(black));
		assertTrue(match.play(white, 0, "e2", "e4", null));
		var encoded = ArcaneChessSavedData.CODEC.encodeStart(JsonOps.INSTANCE, before).getOrThrow();
		ArcaneChessSavedData after = ArcaneChessSavedData.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
		ChessMatch restored = after.get(match.id());
		assertTrue(restored.hasClock());
		long remaining = restored.clockRemainingMillis(Side.BLACK, Long.MAX_VALUE);
		assertTrue(remaining > 59_000 && remaining <= 60_000);
		assertTrue(restored.resumeClock(500_000));
		assertEquals(Side.BLACK, ChessPosition.fromFen(restored.fen()).turn());
		assertEquals(1_000, restored.clockIncrementMillis());
	}

	@Test
	void serializesAndRestoresActiveMatchWithMoveHistory() {
		UUID white = UUID.randomUUID(), black = UUID.randomUUID();
		ArcaneChessSavedData before = new ArcaneChessSavedData();
		ChessMatch match = before.create(white);
		assertTrue(match.join(black));
		assertTrue(match.play(white, 0, "e2", "e4", null));

		var encoded = ArcaneChessSavedData.CODEC.encodeStart(JsonOps.INSTANCE, before).getOrThrow();
		ArcaneChessSavedData after = ArcaneChessSavedData.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
		ChessMatch restored = after.get(match.id());
		assertEquals(match.status(), restored.status());
		assertEquals(match.fen(), restored.fen());
		assertEquals(match.history(), restored.history());
		assertEquals(black, restored.blackPlayer());
	}

	@Test
	void persistsEloAndRecordAfterCompletedGame() {
		UUID white = UUID.randomUUID(), black = UUID.randomUUID();
		ArcaneChessSavedData before = new ArcaneChessSavedData();
		ChessMatch match = before.create(white);
		assertTrue(match.join(black));
		assertTrue(match.play(white, 0, "f2", "f3", null));
		assertTrue(match.play(black, 1, "e7", "e5", null));
		assertTrue(match.play(white, 2, "g2", "g4", null));
		assertTrue(match.play(black, 3, "d8", "h4", null));
		assertEquals(ChessMatch.Status.BLACK_WON, match.status());
		before.recordCompletedMatch(match);

		var encoded = ArcaneChessSavedData.CODEC.encodeStart(JsonOps.INSTANCE, before).getOrThrow();
		ArcaneChessSavedData after = ArcaneChessSavedData.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
		assertEquals(new ChessPlayerStats(984, 0, 1, 0), after.stats(white));
		assertEquals(new ChessPlayerStats(1016, 1, 0, 0), after.stats(black));
	}
}
