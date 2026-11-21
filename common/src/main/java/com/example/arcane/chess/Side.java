package com.example.arcane.chess;

public enum Side { WHITE, BLACK;
	Side opposite() { return this == WHITE ? BLACK : WHITE; }
}
