package com.example.arcane.chess;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChessAiTest {
	@Test
	void everyDifficultyReturnsOnlyLegalMovesAndLeavesPositionUntouched() {
		for (ChessAi.Difficulty difficulty : ChessAi.Difficulty.values()) {
			ChessPosition position = ChessPosition.initial();
			String before = position.toFen();
			ChessMove move = ChessAi.chooseMove(position, difficulty);
			assertNotNull(move, difficulty.name());
			assertTrue(position.legalMoves().contains(move), difficulty.name());
			assertEquals(before, position.toFen(), difficulty.name());
		}
	}

	@Test
	void returnsNullForATerminalPosition() {
		ChessPosition checkmate = ChessPosition.fromFen("7k/5K2/6Q1/8/8/8/8/8 b - - 0 1");
		assertTrue(checkmate.isStalemate());
		assertNull(ChessAi.chooseMove(checkmate, ChessAi.Difficulty.BEGINNER));

		ChessPosition fiftyMoveDraw = ChessPosition.fromFen("4k3/8/8/8/8/8/8/R3K3 w - - 100 51");
		assertTrue(fiftyMoveDraw.isFiftyMoveDraw());
		assertNull(ChessAi.chooseMove(fiftyMoveDraw, ChessAi.Difficulty.BEGINNER));
	}

	@Test
	void hardDifficultyFindsMateInOne() {
		ChessPosition position = ChessPosition.fromFen("7k/8/5KQ1/8/8/8/8/8 w - - 0 1");
		ChessMove move = ChessAi.chooseMove(position, ChessAi.Difficulty.HARD);
		assertNotNull(move);
		ChessPosition after = position.copyForSearch();
		after.applyGeneratedMove(move);
		assertTrue(after.isCheckmate(), "AI should find the available mate in one: " + move);
	}
}
