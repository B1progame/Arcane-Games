package com.example.arcane.chess;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChessPgnTest {
	@Test
	void splitsAChesscomMonthlyArchiveIntoIndependentPgnGames() {
		String archive = "[Event \"First\"]\n[White \"Alice\"]\n[Black \"Bob\"]\n[Result \"1-0\"]\n\n1. e4 e5 1-0\n\n"
				+ "[Event \"Second\"]\n[White \"Carol\"]\n[Black \"Dave\"]\n[Result \"0-1\"]\n\n1. d4 d5 0-1\n";
		List<ChessPgn.Replay> games = ChessPgn.parseArchive(archive);
		assertEquals(2, games.size());
		assertEquals("Alice", games.get(0).white());
		assertEquals("1-0", games.get(0).result());
		assertEquals("Carol", games.get(1).white());
		assertEquals("0-1", games.get(1).result());
	}

	@Test
	void importsPgnIntoVerifiedReadOnlyPositionSnapshots() {
		String pgn = "[Event \"Imported game\"]\n[White \"B1progame\"]\n[Black \"Rival\"]\n[Result \"1-0\"]\n\n"
				+ "1. e4 {opening} e5 (1... c5 2. Nf3) 2. Bc4 Nc6 3. Qh5 Nf6?! 4. Qxf7# 1-0";
		ChessPgn.Replay replay = ChessPgn.parse(pgn);
		assertEquals("B1progame", replay.white());
		assertEquals("Rival", replay.black());
		assertEquals("1-0", replay.result());
		assertEquals(7, replay.moves().size());
		assertEquals(8, replay.positions().size());
		assertTrue(ChessPosition.fromFen(replay.positions().get(7)).isCheckmate());
	}

	@Test
	void rejectsIllegalMovesAndNonstandardVariantsInImportedPgn() {
		assertThrows(IllegalArgumentException.class, () -> ChessPgn.parse("1. e4 e5 2. Ke8 *"));
		assertThrows(IllegalArgumentException.class, () -> ChessPgn.parse("[Variant \"Chess960\"]\n1. e4 *"));
	}

	@Test
	void exportsStandardNotationAndTerminalResult() {
		ChessMatch match = ChessMatch.create(UUID.randomUUID());
		UUID black = UUID.randomUUID();
		assertTrue(match.join(black));
		assertTrue(match.play(match.whitePlayer(), 0, "f2", "f3", null));
		assertTrue(match.play(black, 1, "e7", "e5", null));
		assertTrue(match.play(match.whitePlayer(), 2, "g2", "g4", null));
		assertTrue(match.play(black, 3, "d8", "h4", null));

		String pgn = ChessPgn.export(match);
		assertTrue(pgn.startsWith("[Event \"Arcane Games Chess\"]"));
		assertTrue(pgn.contains("[Result \"0-1\"]"));
		assertTrue(pgn.endsWith("1. f3 e5 2. g4 Qh4# 0-1"));
	}

	@Test
	void sanHandlesAmbiguityCastlingEnPassantAndPromotion() {
		ChessPosition position = ChessPosition.fromFen("4k3/8/8/8/8/2N1N3/8/4K3 w - - 0 1");
		assertEquals("Ncd5", ChessPgn.toSan(position, new ChessMove("c3", "d5")));

		position = ChessPosition.fromFen("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1");
		assertEquals("O-O", ChessPgn.toSan(position, new ChessMove("e1", "g1")));

		position = ChessPosition.fromFen("rnbqkbnr/ppp1pppp/8/3pP3/8/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 3");
		assertEquals("exd6", ChessPgn.toSan(position, new ChessMove("e5", "d6")));

		position = ChessPosition.fromFen("4k3/P7/8/8/8/8/8/4K3 w - - 0 1");
		assertEquals("a8=Q+", ChessPgn.toSan(position, new ChessMove("a7", "a8", Piece.QUEEN)));
	}
}
