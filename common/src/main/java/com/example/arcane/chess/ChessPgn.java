package com.example.arcane.chess;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Portable PGN export built from the same legal move history used for persistence. */
public final class ChessPgn {
	private static final int MAX_ARCHIVE_CHARS = 8_000_000;
	private static final int MAX_ARCHIVE_GAMES = 1_000;

	public record Replay(String white, String black, String result, List<String> positions, List<ChessMove> moves) {
		public Replay {
			positions = List.copyOf(positions);
			moves = List.copyOf(moves);
			if (positions.size() != moves.size() + 1) throw new IllegalArgumentException("Replay position and move counts do not match");
		}
	}

	private ChessPgn() {}

	/** Parse the concatenated standard-chess PGNs returned by a public monthly archive endpoint. */
	public static List<Replay> parseArchive(String archive) {
		if (archive == null || archive.length() > MAX_ARCHIVE_CHARS) throw new IllegalArgumentException("PGN archive is missing or too large");
		Matcher starts = Pattern.compile("(?m)^\\[Event\\s+\"").matcher(archive);
		List<Integer> boundaries = new ArrayList<>();
		while (starts.find()) boundaries.add(starts.start());
		if (boundaries.isEmpty()) return List.of(parse(archive));
		if (boundaries.size() > MAX_ARCHIVE_GAMES) throw new IllegalArgumentException("PGN archive contains too many games");
		List<Replay> games = new ArrayList<>(boundaries.size());
		for (int i = 0; i < boundaries.size(); i++) {
			int end = i + 1 < boundaries.size() ? boundaries.get(i + 1) : archive.length();
			String pgn = archive.substring(boundaries.get(i), end).strip();
			if (!pgn.isEmpty()) games.add(parse(pgn));
		}
		return List.copyOf(games);
	}

	/** Parse a standard-chess PGN into immutable FEN snapshots suitable for a read-only replay. */
	public static Replay parse(String pgn) {
		if (pgn == null || pgn.length() > 1_000_000) throw new IllegalArgumentException("PGN is missing or too large");
		Map<String, String> tags = new LinkedHashMap<>();
		Matcher tagMatcher = Pattern.compile("(?m)^\\s*\\[([A-Za-z0-9_]+)\\s+\"((?:\\\\.|[^\"])*)\"\\s*]\\s*$").matcher(pgn);
		while (tagMatcher.find()) tags.put(tagMatcher.group(1), tagMatcher.group(2).replace("\\\"", "\"").replace("\\\\", "\\"));
		String variant = tags.getOrDefault("Variant", "Standard");
		if (!variant.equalsIgnoreCase("standard") && !variant.equalsIgnoreCase("chess")) throw new IllegalArgumentException("Only standard chess PGNs can be replayed");
		ChessPosition position = tags.containsKey("FEN") ? ChessPosition.fromFen(tags.get("FEN")) : ChessPosition.initial();
		List<String> positions = new ArrayList<>();
		List<ChessMove> moves = new ArrayList<>();
		positions.add(position.toFen());
		String movetext = stripMovetext(pgn.replaceAll("(?m)^\\s*\\[[^\\r\\n]*]\\s*$", " "));
		String pgnResult = "*";
		for (String raw : movetext.trim().split("\\s+")) {
			if (raw.isBlank() || raw.startsWith("$") || raw.equalsIgnoreCase("e.p.") || raw.equalsIgnoreCase("ep")) continue;
			String token = raw.replaceFirst("^\\d+\\.{1,3}", "");
			if (token.isEmpty() || token.matches("\\d+\\.{1,3}")) continue;
			if (token.matches("1-0|0-1|1/2-1/2|\\*")) { pgnResult = token; break; }
			token = token.replaceAll("[!?]+$", "").replaceAll("[+#]+$", "").replace('0', 'O');
			if (token.isEmpty()) continue;
			ChessMove matched = null;
			for (ChessMove legal : position.legalMoves()) {
				if (toSan(position, legal).replaceAll("[+#]+$", "").equals(token)) {
					if (matched != null) throw new IllegalArgumentException("Ambiguous PGN move: " + raw);
					matched = legal;
				}
			}
			if (matched == null || !position.play(matched.from(), matched.to(), matched.promotion())) throw new IllegalArgumentException("Illegal or unsupported PGN move: " + raw);
			moves.add(matched);
			positions.add(position.toFen());
		}
		String declaredResult = tags.get("Result");
		if (declaredResult != null && !declaredResult.equals(pgnResult)) throw new IllegalArgumentException("PGN result tag does not match its movetext");
		return new Replay(tags.getOrDefault("White", "White"), tags.getOrDefault("Black", "Black"), pgnResult, positions, moves);
	}

	private static String stripMovetext(String text) {
		StringBuilder result = new StringBuilder(text.length());
		int variationDepth = 0;
		boolean comment = false, lineComment = false;
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (lineComment) { if (c == '\n' || c == '\r') { lineComment = false; result.append(' '); } continue; }
			if (comment) { if (c == '}') comment = false; continue; }
			if (c == ';' && variationDepth == 0) { lineComment = true; continue; }
			if (c == '{' && variationDepth == 0) { comment = true; continue; }
			if (c == '(') { variationDepth++; continue; }
			if (c == ')' && variationDepth > 0) { variationDepth--; continue; }
			if (variationDepth == 0) result.append(c);
		}
		if (comment || variationDepth != 0) throw new IllegalArgumentException("Unclosed PGN comment or variation");
		return result.toString();
	}

	public static String export(ChessMatch match) {
		String result = resultToken(match.status());
		StringBuilder pgn = new StringBuilder(256);
		pgn.append("[Event \"Arcane Games Chess\"]\n")
				.append("[Site \"Minecraft\"]\n")
				.append("[Date \"????.??.??\"]\n")
				.append("[Round \"-\"]\n")
				.append("[White \"").append(match.whitePlayer()).append("\"]\n")
				.append("[Black \"").append(match.blackPlayer() == null ? "?" : match.blackPlayer()).append("\"]\n")
				.append("[Result \"").append(result).append("\"]\n\n");

		ChessPosition position = ChessPosition.initial();
		List<String> notation = new ArrayList<>(match.history().size());
		for (ChessMove move : match.history()) {
			if (position.turn() == Side.WHITE) notation.add(position.toFen().split(" ")[5] + ".");
			else if (notation.isEmpty()) notation.add(position.toFen().split(" ")[5] + "...");
			notation.add(toSan(position, move));
			if (!position.play(move.from(), move.to(), move.promotion())) throw new IllegalArgumentException("Match contains an illegal move history");
		}
		if (!notation.isEmpty()) pgn.append(String.join(" ", notation)).append(' ');
		return pgn.append(result).toString();
	}

	static String toSan(ChessPosition position, ChessMove move) {
		ChessPiece moving = position.pieceAt(move.from());
		if (moving == null || !position.legalMoves().contains(move)) throw new IllegalArgumentException("Move is not legal in this position");
		boolean capture = position.pieceAt(move.to()) != null || position.isEnPassantCapture(move);
		StringBuilder san = new StringBuilder(8);
		if (moving.type() == Piece.KING && Math.abs(move.to().charAt(0) - move.from().charAt(0)) == 2) {
			san.append(move.to().charAt(0) > move.from().charAt(0) ? "O-O" : "O-O-O");
		} else {
			if (moving.type() != Piece.PAWN) {
				san.append(pieceLetter(moving.type()));
				List<ChessMove> alternatives = position.legalMoves().stream()
						.filter(other -> !other.from().equals(move.from()) && other.to().equals(move.to()))
						.filter(other -> {
							ChessPiece otherPiece = position.pieceAt(other.from());
							return otherPiece != null && otherPiece.side() == moving.side() && otherPiece.type() == moving.type();
						}).toList();
				if (!alternatives.isEmpty()) {
					boolean sameFile = alternatives.stream().anyMatch(other -> other.from().charAt(0) == move.from().charAt(0));
					boolean sameRank = alternatives.stream().anyMatch(other -> other.from().charAt(1) == move.from().charAt(1));
					if (!sameFile) san.append(move.from().charAt(0));
					else if (!sameRank) san.append(move.from().charAt(1));
					else san.append(move.from());
				}
			} else if (capture) {
				san.append(move.from().charAt(0));
			}
			if (capture) san.append('x');
			san.append(move.to());
			if (move.promotion() != null) san.append('=').append(pieceLetter(move.promotion()));
		}

		ChessPosition after = position.copyForSearch();
		if (!after.play(move.from(), move.to(), move.promotion())) throw new IllegalArgumentException("Move is not legal in this position");
		if (after.isCheckmate()) san.append('#');
		else if (after.inCheck(after.turn())) san.append('+');
		return san.toString();
	}

	private static char pieceLetter(Piece piece) {
		return switch (piece) {
			case KNIGHT -> 'N';
			case BISHOP -> 'B';
			case ROOK -> 'R';
			case QUEEN -> 'Q';
			case KING -> 'K';
			case PAWN -> 'P';
		};
	}

	private static String resultToken(ChessMatch.Status status) {
		return switch (status) {
			case WHITE_WON -> "1-0";
			case BLACK_WON -> "0-1";
			case DRAW -> "1/2-1/2";
			case WAITING, IN_PROGRESS -> "*";
		};
	}
}
