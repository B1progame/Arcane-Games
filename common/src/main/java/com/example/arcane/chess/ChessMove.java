package com.example.arcane.chess;

public record ChessMove(String from, String to, Piece promotion) {
	public ChessMove(String from, String to) { this(from, to, null); }
	@Override public String toString() { return from + to + (promotion == null ? "" : promotion.fen()); }
}
