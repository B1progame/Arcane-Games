package com.example.arcane.client.board;

import java.util.ArrayList;
import java.util.List;

/** Chess-to-scene adapter. The shared renderer knows nothing about FEN or chess piece rules. */
public final class ChessBoardSceneSource implements BoardSceneSource {
	private static final float BOARD_SIZE = 2.0f;
	private static final float DECK_HEIGHT = 0.035f;

	@Override
	public BoardScene create(String currentState, String previousState) {
		char[] current = ChessboardPieces.parseFenBoard(currentState);
		char[] previous = ChessboardPieces.parseFenBoard(previousState);
		List<BoardScene.Token> tokens = new ArrayList<>(32);
		for (int square = 0; square < 64; square++) {
			char piece = current[square];
			if (piece == 0) continue;
			int file = square % 8;
			int rank = square / 8;
			tokens.add(token(piece, file, rank));
		}
		BoardScene.Movement movement = findMovement(previousState, previous, current);
		int[] tileColors = new int[64];
		for (int row = 0; row < 8; row++) for (int column = 0; column < 8; column++)
			tileColors[row * 8 + column] = ((row + column) & 1) == 0 ? 0xFFD7C29A : 0xFF5C4636;
		return new BoardScene(8, 8, BOARD_SIZE, BOARD_SIZE, DECK_HEIGHT, 0xFF38271E, tileColors, tokens, movement);
	}

	private BoardScene.Movement findMovement(String oldFen, char[] before, char[] after) {
		if (before == null || after == null || oldFen == null || before.length != 64 || after.length != 64) return null;
		String[] fields = oldFen.split(" ");
		if (fields.length < 2) return null;
		boolean whiteMoved = fields[1].equals("w");
		char king = whiteMoved ? 'K' : 'k';
		int kingFrom = -1, kingTo = -1;
		for (int i = 0; i < 64; i++) {
			if (before[i] == king && after[i] != king) kingFrom = i;
			if (after[i] == king && before[i] != king) kingTo = i;
		}
		if (kingFrom >= 0 && kingTo >= 0) return movement(after[kingTo], kingFrom, kingTo);

		for (int to = 0; to < 64; to++) {
			char arriving = after[to];
			if (arriving == 0 || Character.isUpperCase(arriving) != whiteMoved || before[to] == arriving) continue;
			for (int from = 0; from < 64; from++) {
				if (before[from] == arriving && after[from] == 0) return movement(arriving, from, to);
			}
			if (Character.toLowerCase(arriving) != 'p') {
				for (int from = 0; from < 64; from++) {
					if (Character.toLowerCase(before[from]) == 'p' && Character.isUpperCase(before[from]) == whiteMoved && after[from] == 0 && from % 8 == to % 8) {
						return movement(arriving, from, to);
					}
				}
			}
		}
		return null;
	}

	private BoardScene.Movement movement(char piece, int from, int to) {
		BoardScene.Token token = token(piece, to % 8, to / 8);
		return new BoardScene.Movement(token, from % 8, from / 8, to % 8, to / 8, 0.08f);
	}

	private BoardScene.Token token(char fen, int file, int rank) {
		boolean white = Character.isUpperCase(fen);
		int main = white ? 0xFFF0E0C0 : 0xFF302A34;
		int light = white ? 0xFFFFF0D0 : 0xFF5D5264;
		int shade = white ? 0xFF997851 : 0xFF17141D;
		int gold = white ? 0xFFE3C36B : 0xFFB28D42;
		List<BoardScene.Cuboid> model = new ArrayList<>(9);
		// Shared stepped plinth and narrow inlaid band.
		box(model, -0.37f, 0.00f, -0.37f, 0.37f, 0.10f, 0.37f, shade);
		box(model, -0.34f, 0.10f, -0.34f, 0.34f, 0.17f, 0.34f, gold);
		box(model, -0.30f, 0.17f, -0.30f, 0.30f, 0.23f, 0.30f, main);
		switch (Character.toLowerCase(fen)) {
			case 'p' -> {
				box(model, -0.15f, 0.23f, -0.15f, 0.15f, 0.65f, 0.15f, shade);
				box(model, -0.13f, 0.23f, -0.13f, 0.13f, 0.62f, 0.13f, main);
				box(model, -0.21f, 0.62f, -0.21f, 0.21f, 0.91f, 0.21f, light);
				box(model, -0.09f, 0.90f, -0.09f, 0.09f, 1.00f, 0.09f, gold);
			}
			case 'r' -> {
				box(model, -0.29f, 0.23f, -0.29f, 0.29f, 0.82f, 0.29f, shade);
				box(model, -0.26f, 0.23f, -0.26f, 0.26f, 0.79f, 0.26f, main);
				box(model, -0.34f, 0.76f, -0.34f, 0.34f, 0.87f, 0.34f, light);
				for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) if (x == 0 || z == 0)
					box(model, x * 0.22f - 0.055f, 0.87f, z * 0.22f - 0.055f, x * 0.22f + 0.055f, 1.00f, z * 0.22f + 0.055f, gold);
			}
			case 'n' -> {
				box(model, -0.30f, 0.23f, -0.25f, 0.30f, 0.52f, 0.25f, shade);
				box(model, -0.24f, 0.25f, -0.22f, 0.23f, 0.55f, 0.22f, main);
				box(model, -0.15f, 0.48f, -0.20f, 0.18f, 0.82f, 0.19f, light);
				box(model, 0.05f, 0.66f, -0.17f, 0.34f, 0.89f, 0.16f, main);
				box(model, 0.23f, 0.58f, -0.12f, 0.39f, 0.71f, 0.11f, gold);
				box(model, 0.13f, 0.83f, -0.12f, 0.22f, 1.00f, -0.03f, shade);
			}
			case 'b' -> {
				box(model, -0.23f, 0.23f, -0.23f, 0.23f, 0.75f, 0.23f, shade);
				box(model, -0.19f, 0.23f, -0.19f, 0.19f, 0.72f, 0.19f, main);
				box(model, -0.25f, 0.58f, -0.25f, 0.25f, 0.72f, 0.25f, gold);
				box(model, -0.17f, 0.73f, -0.17f, 0.17f, 0.91f, 0.17f, light);
				box(model, -0.08f, 0.90f, -0.08f, 0.08f, 1.00f, 0.08f, gold);
			}
			case 'q' -> {
				box(model, -0.17f, 0.23f, -0.17f, 0.17f, 0.80f, 0.17f, shade);
				box(model, -0.13f, 0.23f, -0.13f, 0.13f, 0.77f, 0.13f, main);
				box(model, -0.32f, 0.74f, -0.32f, 0.32f, 0.85f, 0.32f, gold);
				for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) if (Math.abs(x) + Math.abs(z) <= 1)
					box(model, x * 0.20f - 0.055f, 0.85f, z * 0.20f - 0.055f, x * 0.20f + 0.055f, 0.97f, z * 0.20f + 0.055f, light);
				box(model, -0.07f, 0.94f, -0.07f, 0.07f, 1.06f, 0.07f, gold);
			}
			case 'k' -> {
				box(model, -0.20f, 0.23f, -0.20f, 0.20f, 0.82f, 0.20f, shade);
				box(model, -0.16f, 0.23f, -0.16f, 0.16f, 0.79f, 0.16f, main);
				box(model, -0.31f, 0.76f, -0.31f, 0.31f, 0.87f, 0.31f, gold);
				box(model, -0.07f, 0.86f, -0.07f, 0.07f, 1.08f, 0.07f, light);
				box(model, -0.20f, 0.94f, -0.20f, 0.20f, 1.05f, 0.20f, gold);
			}
		}
		return new BoardScene.Token("chess:" + file + ":" + rank, file, rank, model);
	}

	private static void box(List<BoardScene.Cuboid> parts, float minX, float minY, float minZ,
			float maxX, float maxY, float maxZ, int color) {
		parts.add(new BoardScene.Cuboid(minX, minY, minZ, maxX, maxY, maxZ, color));
	}
}

/** Package-private FEN helper keeps chess parsing inside the chess scene adapter. */
final class ChessboardPieces {
	private ChessboardPieces() {}
	static char[] parseFenBoard(String fen) {
		char[] board = new char[64];
		if (fen == null) return null;
		int rank = 7, file = 0;
		for (int i = 0; i < fen.length() && fen.charAt(i) != ' '; i++) {
			char value = fen.charAt(i);
			if (value == '/') { rank--; file = 0; }
			else if (value >= '1' && value <= '8') file += value - '0';
			else if (rank >= 0 && rank < 8 && file >= 0 && file < 8) board[rank * 8 + file++] = value;
		}
		return board;
	}
}
