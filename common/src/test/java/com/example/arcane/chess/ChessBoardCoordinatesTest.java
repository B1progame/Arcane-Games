package com.example.arcane.chess;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChessBoardCoordinatesTest {
	@Test
	void mapsEveryCellCenterToExpectedSquareInWhitePerspective() {
		int boardX = 37, boardY = 61, cell = 40;
		for (int row = 0; row < 8; row++) for (int file = 0; file < 8; file++) {
			String expected = "" + (char) ('a' + file) + (char) ('8' - row);
			assertEquals(expected, ChessBoardCoordinates.squareAt(boardX + file * cell + cell / 2.0,
					boardY + row * cell + cell / 2.0, boardX, boardY, cell, false));
		}
	}

	@Test
	void mapsEveryCellCenterToExpectedSquareInBlackPerspective() {
		int boardX = 11, boardY = 23, cell = 32;
		for (int row = 0; row < 8; row++) for (int file = 0; file < 8; file++) {
			String expected = "" + (char) ('h' - file) + (char) ('1' + row);
			assertEquals(expected, ChessBoardCoordinates.squareAt(boardX + file * cell + cell / 2.0,
					boardY + row * cell + cell / 2.0, boardX, boardY, cell, true));
		}
	}

	@Test
	void rejectsOutsideEdgesAndInvalidCellSize() {
		assertNull(ChessBoardCoordinates.squareAt(100, 100, 100, 100, 0, false));
		assertNull(ChessBoardCoordinates.squareAt(99, 100, 100, 100, 20, false));
		assertNull(ChessBoardCoordinates.squareAt(260, 100, 100, 100, 20, false));
		assertNull(ChessBoardCoordinates.squareAt(100, 260, 100, 100, 20, false));
	}

	@Test
	void includesTopAndLeftEdgesButExcludesRightAndBottomEdges() {
		assertEquals("a8", ChessBoardCoordinates.squareAt(20, 30, 20, 30, 24, false));
		assertEquals("h1", ChessBoardCoordinates.squareAt(20 + 8 * 24 - 0.001, 30 + 8 * 24 - 0.001, 20, 30, 24, false));
		assertNull(ChessBoardCoordinates.squareAt(20 + 8 * 24, 30 + 8 * 24 / 2.0, 20, 30, 24, false));
		assertNull(ChessBoardCoordinates.squareAt(20 + 8 * 24 / 2.0, 30 + 8 * 24, 20, 30, 24, false));
	}

	@Test
	void keepsSubpixelPointerHitsWithinTheSameCellForBothOrientations() {
		for (int cell : new int[] {12, 29, 48, 56}) {
			int left = 7, top = 19;
			for (int screenRow = 0; screenRow < 8; screenRow++) for (int screenFile = 0; screenFile < 8; screenFile++) {
				double x = left + screenFile * cell + cell * 0.73;
				double y = top + screenRow * cell + cell * 0.61;
				String whiteExpected = "" + (char) ('a' + screenFile) + (char) ('8' - screenRow);
				String blackExpected = "" + (char) ('h' - screenFile) + (char) ('1' + screenRow);
				assertEquals(whiteExpected, ChessBoardCoordinates.squareAt(x, y, left, top, cell, false));
				assertEquals(blackExpected, ChessBoardCoordinates.squareAt(x, y, left, top, cell, true));
			}
		}
	}
}
