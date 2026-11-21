package com.example.arcane.chess;

public enum Piece {
	PAWN('p'), KNIGHT('n'), BISHOP('b'), ROOK('r'), QUEEN('q'), KING('k');

	private final char fen;
	Piece(char fen) { this.fen = fen; }
	char fen() { return fen; }
	static Piece fromFen(char value) {
		for (Piece piece : values()) if (piece.fen == Character.toLowerCase(value)) return piece;
		throw new IllegalArgumentException("Invalid FEN piece: " + value);
	}
}
