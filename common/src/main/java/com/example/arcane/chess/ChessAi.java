package com.example.arcane.chess;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SplittableRandom;

/** Small offline opponent. Search operates on copies and must be called off the render/server tick thread. */
public final class ChessAi {
	private static final int MATE_SCORE = 100_000;
	private static final int[] VALUES = {100, 320, 335, 500, 900, 20_000};

	public enum Difficulty {
		BEGINNER(0, 0), EASY(1, 350), NORMAL(2, 900), HARD(3, 1800);
		private final int depth;
		private final int budgetMillis;
		Difficulty(int depth, int budgetMillis) { this.depth = depth; this.budgetMillis = budgetMillis; }
	}

	private ChessAi() {}

	/** Returns a legal move, or null when the supplied position is already terminal. */
	public static ChessMove chooseMove(ChessPosition source, Difficulty difficulty) {
		ChessPosition root = source.copyForSearch();
		List<ChessMove> legal = root.legalMoves();
		if (legal.isEmpty() || root.isFiftyMoveDraw() || root.isThreefoldRepetition() || root.isInsufficientMaterial()) return null;
		if (difficulty == Difficulty.BEGINNER) {
			return legal.get(new SplittableRandom(System.nanoTime() ^ root.toFen().hashCode()).nextInt(legal.size()));
		}

		long deadline = System.nanoTime() + difficulty.budgetMillis * 1_000_000L;
		ChessMove best = orderMoves(root, legal).getFirst();
		for (int depth = 1; depth <= difficulty.depth; depth++) {
			try {
				best = searchRoot(root, depth, deadline);
			} catch (SearchExpired ignored) {
				break;
			}
		}
		return best;
	}

	private static ChessMove searchRoot(ChessPosition root, int depth, long deadline) {
		checkTime(deadline);
		List<ChessMove> moves = orderMoves(root, root.legalMoves());
		ChessMove bestMove = moves.getFirst();
		int bestScore = Integer.MIN_VALUE + 1;
		int alpha = -MATE_SCORE;
		for (ChessMove move : moves) {
			checkTime(deadline);
			ChessPosition child = root.copyForSearch();
			child.applyGeneratedMove(move);
			int score = -negamax(child, depth - 1, -MATE_SCORE, -alpha, 1, deadline);
			if (score > bestScore) {
				bestScore = score;
				bestMove = move;
			}
			alpha = Math.max(alpha, score);
		}
		return bestMove;
	}

	private static int negamax(ChessPosition position, int depth, int alpha, int beta, int ply, long deadline) {
		checkTime(deadline);
		if (position.isFiftyMoveDraw() || position.isThreefoldRepetition() || position.isInsufficientMaterial()) return 0;
		List<ChessMove> legal = position.legalMoves();
		if (legal.isEmpty()) return position.inCheck(position.turn()) ? -MATE_SCORE + ply : 0;
		if (depth <= 0) return evaluate(position);

		int best = Integer.MIN_VALUE + 1;
		for (ChessMove move : orderMoves(position, legal)) {
			ChessPosition child = position.copyForSearch();
			child.applyGeneratedMove(move);
			int score = -negamax(child, depth - 1, -beta, -alpha, ply + 1, deadline);
			best = Math.max(best, score);
			alpha = Math.max(alpha, score);
			if (alpha >= beta) break;
		}
		return best;
	}

	private static List<ChessMove> orderMoves(ChessPosition position, List<ChessMove> moves) {
		List<ChessMove> ordered = new ArrayList<>(moves);
		ordered.sort(Comparator.comparingInt((ChessMove move) -> movePriority(position, move)).reversed());
		return ordered;
	}

	private static int movePriority(ChessPosition position, ChessMove move) {
		int from = squareIndex(move.from()), to = squareIndex(move.to());
		char attacker = position.pieceAtIndex(from), victim = position.pieceAtIndex(to);
		int score = victim == 0 ? 0 : 10 * value(victim) - value(attacker);
		if (victim == 0 && position.isEnPassantCapture(move)) score += 10 * VALUES[0] - value(attacker);
		if (move.promotion() != null) score += value(move.promotion().fen()) + 800;
		return score;
	}

	private static int evaluate(ChessPosition position) {
		int score = 0;
		for (int square = 0; square < 64; square++) {
			char piece = position.pieceAtIndex(square);
			if (piece == 0) continue;
			boolean white = Character.isUpperCase(piece);
			Piece type = Piece.fromFen(piece);
			int file = square % 8, rank = square / 8;
			int advance = white ? rank : 7 - rank;
			int center = 7 - (Math.abs(3 - file) + Math.abs(3 - rank));
			int positional = switch (type) {
				case PAWN -> advance * 7 + Math.max(0, center) * 2;
				case KNIGHT -> center * 9;
				case BISHOP -> center * 4;
				case ROOK -> advance * 2;
				case QUEEN -> center * 2;
				case KING -> 0;
			};
			score += (white ? 1 : -1) * (value(piece) + positional);
		}
		return position.turn() == Side.WHITE ? score : -score;
	}

	private static int value(char piece) {
		return VALUES[Piece.fromFen(piece).ordinal()];
	}

	private static int squareIndex(String square) {
		return square.charAt(0) - 'a' + (square.charAt(1) - '1') * 8;
	}

	private static void checkTime(long deadline) {
		if (System.nanoTime() >= deadline) throw SearchExpired.INSTANCE;
	}

	private static final class SearchExpired extends RuntimeException {
		private static final SearchExpired INSTANCE = new SearchExpired();
		private SearchExpired() { super(null, null, false, false); }
	}
}
