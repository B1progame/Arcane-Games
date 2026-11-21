package com.example.arcane.client.board;

import java.util.List;

/** Immutable renderer-neutral description of a tiled game board and its cell tokens. */
public record BoardScene(int columns, int rows, float width, float depth, float deckHeight,
		int frameColor, int[] tileColors, List<Token> tokens, Movement movement) {
	public BoardScene {
		if (columns < 1 || rows < 1 || width <= 0 || depth <= 0 || deckHeight <= 0) throw new IllegalArgumentException("Invalid board dimensions");
		if (tileColors == null || tileColors.length != columns * rows) throw new IllegalArgumentException("One tile color is required per cell");
		tileColors = tileColors.clone();
		tokens = List.copyOf(tokens);
	}
	@Override public int[] tileColors() { return tileColors.clone(); }
	public int tileColor(int column, int row) { return tileColors[row * columns + column]; }

	public record Token(String id, int column, int row, List<Cuboid> model) {
		public Token {
			if (id == null || id.isBlank() || model == null) throw new IllegalArgumentException("Invalid board token");
			model = List.copyOf(model);
		}
	}

	/** Coordinates are in fractions of one cell, centered at x/z zero with y starting at the cell floor. */
	public record Cuboid(float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int color) {
		public Cuboid {
			if (minX >= maxX || minY < 0 || minY >= maxY || minZ >= maxZ) throw new IllegalArgumentException("Invalid token cuboid");
		}
	}

	/** A single token movement; adapters decide which rules-state transition this represents. */
	public record Movement(Token token, int fromColumn, int fromRow, int toColumn, int toRow, float lift) {}
}
