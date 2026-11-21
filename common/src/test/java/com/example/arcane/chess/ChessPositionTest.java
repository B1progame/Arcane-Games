package com.example.arcane.chess;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChessPositionTest {
    @Test
    void startsWithTwentyLegalMovesAndSerializesStartingFen() {
        ChessPosition game = ChessPosition.initial();
        assertEquals(20, game.legalMoves().size());
        assertEquals("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", game.toFen());
		assertEquals(8902, perft(game, 3));
    }

	private static long perft(ChessPosition position, int depth) {
		if (depth == 0) return 1;
		long nodes = 0;
		for (ChessMove move : position.legalMoves()) {
		ChessPosition next = position.copyForSearch();
		next.applyGeneratedMove(move);
			nodes += perft(next, depth - 1);
		}
		return nodes;
	}

	@Test
	void matchesKiwipetePerftThroughDepthThree() {
		ChessPosition position = ChessPosition.fromFen("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1");
		assertEquals(48, position.legalMoves().size());
		assertEquals(2039, perft(position, 2));
		assertEquals(97862, perft(position, 3));
	}

	@Test
	void matchesStandardEndgamePerftThroughDepthFour() {
		ChessPosition position = ChessPosition.fromFen("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1");
		assertEquals(14, position.legalMoves().size());
		assertEquals(191, perft(position, 2));
		assertEquals(2812, perft(position, 3));
		assertEquals(43238, perft(position, 4));
	}

    @Test
    void rejectsMoveThatLeavesOwnKingInCheck() {
        ChessPosition game = ChessPosition.fromFen("4r1k1/8/8/8/8/8/4R3/4K3 w - - 0 1");
        assertFalse(game.play("e2", "d2"));
        assertTrue(game.play("e2", "e8"));
    }

    @Test
    void supportsEnPassantAndPromotion() {
        ChessPosition game = ChessPosition.fromFen("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1");
        assertTrue(game.play("e5", "d6"));
        assertEquals("P", game.pieceAt("d6").toString());
        assertNull(game.pieceAt("d5"));

        game = ChessPosition.fromFen("4k3/P7/8/8/8/8/8/4K3 w - - 0 1");
        assertTrue(game.play("a7", "a8", Piece.QUEEN));
        assertEquals(Piece.QUEEN, game.pieceAt("a8").type());
    }

	@Test
	void validatesAllFourPromotionChoices() {
		for (Piece promotion : List.of(Piece.QUEEN, Piece.ROOK, Piece.BISHOP, Piece.KNIGHT)) {
			ChessPosition game = ChessPosition.fromFen("4k3/P7/8/8/8/8/8/4K3 w - - 0 1");
			assertTrue(game.play("a7", "a8", promotion));
			assertEquals(promotion, game.pieceAt("a8").type());
		}
		ChessPosition game = ChessPosition.fromFen("4k3/P7/8/8/8/8/8/4K3 w - - 0 1");
		assertFalse(game.play("a7", "a8", Piece.KING));
	}

	@Test
	void rejectsEnPassantThatWouldExposeTheKingAndCastlingThroughCheck() {
		ChessPosition game = ChessPosition.fromFen("k3r3/8/8/3pP3/8/8/8/4K3 w - d6 0 1");
		assertFalse(game.play("e5", "d6"));

		game = ChessPosition.fromFen("4kr2/8/8/8/8/8/8/R3K2R w KQ - 0 1");
		assertFalse(game.play("e1", "g1"));
	}

	@Test
	void rejectsMalformedFenEnPassantAndCastlingFields() {
		assertThrows(IllegalArgumentException.class, () -> ChessPosition.fromFen("4k3/8/8/8/8/8/8/4K3 w - a1 0 1"));
		assertThrows(IllegalArgumentException.class, () -> ChessPosition.fromFen("4k3/8/8/8/8/8/8/4K3 w K - 0 1"));
	}

    @Test
    void supportsCastlingAndCheckmateDetection() {
        ChessPosition game = ChessPosition.fromFen("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1");
        assertTrue(game.play("e1", "g1"));
        assertEquals(Piece.ROOK, game.pieceAt("f1").type());

        game = ChessPosition.initial();
        assertTrue(game.play("f2", "f3"));
        assertTrue(game.play("e7", "e5"));
        assertTrue(game.play("g2", "g4"));
        assertTrue(game.play("d8", "h4"));
        assertTrue(game.isCheckmate());
    }

	@Test
	void detectsStalemateFiftyMoveAndThreefoldDraws() {
		ChessPosition game = ChessPosition.fromFen("7k/5K2/6Q1/8/8/8/8/8 b - - 0 1");
		assertTrue(game.isStalemate());

		game = ChessPosition.fromFen("4k3/8/8/8/8/8/8/4K3 w - - 100 51");
		assertTrue(game.isFiftyMoveDraw());
		assertTrue(game.isInsufficientMaterial());
		game = ChessPosition.fromFen("4k3/8/8/8/8/8/8/2B1K1b1 w - - 0 1");
		assertTrue(game.isInsufficientMaterial());

		game = ChessPosition.initial();
		String[][] cycle = {{"g1", "f3"}, {"g8", "f6"}, {"f3", "g1"}, {"f6", "g8"}};
		for (int repeat = 0; repeat < 2; repeat++) for (String[] move : cycle) assertTrue(game.play(move[0], move[1]));
		assertTrue(game.isThreefoldRepetition());
	}

	@Test
	void treatsTwoKnightsAgainstABareKingAsTimeoutInsufficientMaterial() {
		ChessPosition position = ChessPosition.fromFen("7k/8/8/8/8/8/8/NN3K2 w - - 0 1");
		assertTrue(position.isInsufficientMaterial());
		assertTrue(position.isDraw());
		position = ChessPosition.fromFen("7k/8/8/8/8/8/4p3/NN3K2 w - - 0 1");
		assertFalse(position.isInsufficientMaterial());
	}
}
