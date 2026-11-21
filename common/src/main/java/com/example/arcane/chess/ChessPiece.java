package com.example.arcane.chess;

public record ChessPiece(Side side, Piece type) {
	@Override public String toString() {
		char symbol = type.fen();
		return String.valueOf(side == Side.WHITE ? Character.toUpperCase(symbol) : symbol);
	}
}
