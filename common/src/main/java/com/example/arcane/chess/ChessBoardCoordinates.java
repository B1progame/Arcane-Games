package com.example.arcane.chess;

/** Maps scaled Minecraft screen coordinates to algebraic board squares. */
public final class ChessBoardCoordinates {
	private ChessBoardCoordinates() {}

	public static String squareAt(double mouseX, double mouseY, int boardX, int boardY, int squareSize, boolean blackPerspective) {
		if (squareSize <= 0) return null;
		double localX = mouseX - boardX;
		double localY = mouseY - boardY;
		int boardPixels = squareSize * 8;
		if (localX < 0 || localX >= boardPixels || localY < 0 || localY >= boardPixels) return null;
		int screenFile = (int) (localX / squareSize);
		int screenRow = (int) (localY / squareSize);
		int file = blackPerspective ? 7 - screenFile : screenFile;
		int rank = blackPerspective ? screenRow + 1 : 8 - screenRow;
		return "" + (char) ('a' + file) + (char) ('0' + rank);
	}
}
