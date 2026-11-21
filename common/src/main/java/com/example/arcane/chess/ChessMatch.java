package com.example.arcane.chess;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Match-level authorization and result logic. Callers should mutate it on the logical server thread. */
public final class ChessMatch {
	public enum Status { WAITING, IN_PROGRESS, WHITE_WON, BLACK_WON, DRAW }

	public record Snapshot(UUID id, UUID whitePlayer, UUID blackPlayer, Status status, String fen,
			List<String> moves, UUID drawOfferer, long clockWhiteMillis, long clockBlackMillis, long clockIncrementMillis) {
		public Snapshot(UUID id, UUID whitePlayer, UUID blackPlayer, Status status, String fen, List<String> moves, UUID drawOfferer) {
			this(id, whitePlayer, blackPlayer, status, fen, moves, drawOfferer, -1, -1, -1);
		}
		public Snapshot {
			Objects.requireNonNull(id);
			Objects.requireNonNull(whitePlayer);
			Objects.requireNonNull(status);
			Objects.requireNonNull(fen);
			moves = List.copyOf(moves);
			if (moves.size() > 4096) throw new IllegalArgumentException("Match history is too large");
			boolean noClock = clockWhiteMillis == -1 && clockBlackMillis == -1 && clockIncrementMillis == -1;
			boolean hasClock = clockWhiteMillis >= 0 && clockBlackMillis >= 0 && clockIncrementMillis >= 0;
			if (!noClock && !hasClock) throw new IllegalArgumentException("Invalid saved chess clock");
			if (hasClock) new ChessClock.Snapshot(clockWhiteMillis, clockBlackMillis, clockIncrementMillis);
		}
	}

	private final UUID id;
	private final UUID whitePlayer;
	private UUID blackPlayer;
	private ChessPosition position;
	private Status status;
	private UUID drawOfferer;
	private final ChessClock clock;

	private ChessMatch(UUID id, UUID whitePlayer, UUID blackPlayer, ChessPosition position, Status status, UUID drawOfferer, ChessClock clock) {
		this.id = Objects.requireNonNull(id);
		this.whitePlayer = Objects.requireNonNull(whitePlayer);
		this.blackPlayer = blackPlayer;
		this.position = Objects.requireNonNull(position);
		this.status = Objects.requireNonNull(status);
		this.drawOfferer = drawOfferer;
		this.clock = clock;
	}

	public static ChessMatch create(UUID whitePlayer) {
		return new ChessMatch(UUID.randomUUID(), whitePlayer, null, ChessPosition.initial(), Status.WAITING, null, null);
	}

	public static ChessMatch create(UUID whitePlayer, long initialMillis, long incrementMillis) {
		return new ChessMatch(UUID.randomUUID(), whitePlayer, null, ChessPosition.initial(), Status.WAITING, null, ChessClock.create(initialMillis, incrementMillis));
	}

	public UUID id() { return id; }
	public UUID whitePlayer() { return whitePlayer; }
	public UUID blackPlayer() { return blackPlayer; }
	public Status status() { return status; }
	public String fen() { return position.toFen(); }
	public List<ChessMove> history() { return position.history(); }
	public int ply() { return position.history().size(); }
	public UUID drawOfferer() { return drawOfferer; }
	public boolean hasClock() { return clock != null; }
	public long clockRemainingMillis(Side side, long nowMillis) { return clock == null ? 0 : clock.remainingMillis(side, nowMillis); }
	public long savedWhiteMillis() { return clock == null ? -1 : clock.remainingMillis(Side.WHITE, System.nanoTime() / 1_000_000L); }
	public long savedBlackMillis() { return clock == null ? -1 : clock.remainingMillis(Side.BLACK, System.nanoTime() / 1_000_000L); }
	public long clockIncrementMillis() { return clock == null ? -1 : clock.incrementMillis(); }

	public boolean join(UUID player) {
		return join(player, System.nanoTime() / 1_000_000L);
	}

	public boolean join(UUID player, long nowMillis) {
		if (player == null || player.equals(whitePlayer) || status != Status.WAITING) return false;
		blackPlayer = player;
		status = Status.IN_PROGRESS;
		if (clock != null) clock.start(position.turn(), nowMillis);
		return true;
	}

	public boolean isParticipant(UUID player) {
		return player != null && (player.equals(whitePlayer) || player.equals(blackPlayer));
	}

	public boolean canMove(UUID player) {
		if (status != Status.IN_PROGRESS || !isParticipant(player)) return false;
		return player.equals(position.turn() == Side.WHITE ? whitePlayer : blackPlayer);
	}

	public boolean play(UUID player, int expectedPly, String from, String to, Piece promotion) {
		return play(player, expectedPly, from, to, promotion, System.nanoTime() / 1_000_000L);
	}

	public boolean play(UUID player, int expectedPly, String from, String to, Piece promotion, long nowMillis) {
		if (expectedPly != ply() || !canMove(player)) return false;
		if (clock != null) {
			resumeClock(nowMillis);
			if (updateClock(nowMillis)) return false;
		}
		Side movingSide = position.turn();
		try {
			if (!position.play(from, to, promotion)) return false;
		} catch (IllegalArgumentException invalidSquare) {
			return false;
		}
		drawOfferer = null;
		updateResult();
		if (clock != null) {
			if (status == Status.IN_PROGRESS) clock.completeTurn(movingSide, position.turn(), nowMillis);
			else clock.checkpoint(nowMillis);
		}
		return true;
	}

	public boolean resumeClock(long nowMillis) {
		if (clock == null || status != Status.IN_PROGRESS || clock.isRunning()) return false;
		clock.start(position.turn(), nowMillis);
		return true;
	}

	/** Advances a timed match using server time. Returns true only when this call flags a player. */
	public boolean updateClock(long nowMillis) {
		if (clock == null || status != Status.IN_PROGRESS) return false;
		Side flagged = clock.flaggedSide(nowMillis);
		if (flagged == null) return false;
		clock.checkpoint(nowMillis);
		status = timeoutResult(position, flagged);
		drawOfferer = null;
		return true;
	}

	static Status timeoutResult(ChessPosition position, Side flaggedSide) {
		Objects.requireNonNull(position);
		Objects.requireNonNull(flaggedSide);
		return position.isInsufficientMaterial() ? Status.DRAW : flaggedSide == Side.WHITE ? Status.BLACK_WON : Status.WHITE_WON;
	}

	public boolean resign(UUID player) {
		return resign(player, System.nanoTime() / 1_000_000L);
	}

	public boolean resign(UUID player, long nowMillis) {
		if (status != Status.IN_PROGRESS || !isParticipant(player)) return false;
		status = player.equals(whitePlayer) ? Status.BLACK_WON : Status.WHITE_WON;
		drawOfferer = null;
		if (clock != null) clock.checkpoint(nowMillis);
		return true;
	}

	public boolean offerDraw(UUID player) {
		return offerDraw(player, System.nanoTime() / 1_000_000L);
	}

	public boolean offerDraw(UUID player, long nowMillis) {
		if (status != Status.IN_PROGRESS || !isParticipant(player) || drawOfferer != null) return false;
		drawOfferer = player;
		return true;
	}

	public boolean acceptDraw(UUID player) {
		return acceptDraw(player, System.nanoTime() / 1_000_000L);
	}

	public boolean acceptDraw(UUID player, long nowMillis) {
		if (status != Status.IN_PROGRESS || drawOfferer == null || drawOfferer.equals(player) || !isParticipant(player)) return false;
		status = Status.DRAW;
		drawOfferer = null;
		if (clock != null) clock.checkpoint(nowMillis);
		return true;
	}

	public Snapshot snapshot() {
		return snapshot(System.nanoTime() / 1_000_000L);
	}

	public Snapshot snapshot(long nowMillis) {
		ChessClock.Snapshot clockSnapshot = clock == null ? null : clock.snapshot(nowMillis);
		return new Snapshot(id, whitePlayer, blackPlayer, status, position.toFen(),
				position.history().stream().map(ChessMove::toString).toList(), drawOfferer,
				clockSnapshot == null ? -1 : clockSnapshot.whiteMillis(),
				clockSnapshot == null ? -1 : clockSnapshot.blackMillis(),
				clockSnapshot == null ? -1 : clockSnapshot.incrementMillis());
	}

	public static ChessMatch restore(Snapshot snapshot) {
		Objects.requireNonNull(snapshot);
		if (snapshot.moves().size() > 4096) throw new IllegalArgumentException("Match history is too large");
		ChessPosition position = ChessPosition.initial();
		for (String uci : snapshot.moves()) {
			if (uci == null || (uci.length() != 4 && uci.length() != 5)) throw new IllegalArgumentException("Invalid move record");
			Piece promotion = uci.length() == 5 ? Piece.fromFen(uci.charAt(4)) : null;
			if (!position.play(uci.substring(0, 2), uci.substring(2, 4), promotion)) throw new IllegalArgumentException("Illegal saved move");
		}
		if (!position.toFen().equals(snapshot.fen())) throw new IllegalArgumentException("Saved position does not match move history");
		boolean waiting = snapshot.blackPlayer() == null;
		if (waiting != (snapshot.status() == Status.WAITING)) throw new IllegalArgumentException("Invalid match status");
		if (waiting && (snapshot.drawOfferer() != null || !snapshot.moves().isEmpty())) throw new IllegalArgumentException("Waiting match cannot contain a game history or draw offer");
		if (snapshot.drawOfferer() != null && !snapshot.drawOfferer().equals(snapshot.whitePlayer()) && !snapshot.drawOfferer().equals(snapshot.blackPlayer())) throw new IllegalArgumentException("Invalid draw offer");
		boolean checkmate = position.isCheckmate();
		if (checkmate) {
			Status checkmateResult = position.turn() == Side.WHITE ? Status.BLACK_WON : Status.WHITE_WON;
			if (snapshot.status() != checkmateResult) throw new IllegalArgumentException("Saved result does not match checkmate position");
		} else if (snapshot.status() == Status.IN_PROGRESS && position.isDraw()) {
			throw new IllegalArgumentException("Saved in-progress match already ended in a draw");
		}
		if ((snapshot.status() == Status.WHITE_WON || snapshot.status() == Status.BLACK_WON || snapshot.status() == Status.DRAW)
				&& snapshot.drawOfferer() != null) throw new IllegalArgumentException("Finished match cannot contain a draw offer");
		ChessClock clock = snapshot.clockWhiteMillis() < 0 ? null : ChessClock.restore(new ChessClock.Snapshot(
				snapshot.clockWhiteMillis(), snapshot.clockBlackMillis(), snapshot.clockIncrementMillis()));
		return new ChessMatch(snapshot.id(), snapshot.whitePlayer(), snapshot.blackPlayer(), position, snapshot.status(), snapshot.drawOfferer(), clock);
	}

	private void updateResult() {
		if (position.isCheckmate()) status = position.turn() == Side.WHITE ? Status.BLACK_WON : Status.WHITE_WON;
		else if (position.isDraw()) status = Status.DRAW;
	}
}
