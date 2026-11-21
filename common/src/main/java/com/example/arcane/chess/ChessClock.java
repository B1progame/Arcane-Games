package com.example.arcane.chess;

/** Server-owned, monotonic chess clock. Snapshots store remaining time and restore paused. */
public final class ChessClock {
	private static final long MAX_TIME_MILLIS = 86_400_000L;
	private long whiteMillis;
	private long blackMillis;
	private final long incrementMillis;
	private Side activeSide;
	private long startedAtMillis = -1;

	public record Snapshot(long whiteMillis, long blackMillis, long incrementMillis) {
		public Snapshot {
			if (whiteMillis < 0 || whiteMillis > MAX_TIME_MILLIS || blackMillis < 0 || blackMillis > MAX_TIME_MILLIS
					|| incrementMillis < 0 || incrementMillis > MAX_TIME_MILLIS) throw new IllegalArgumentException("Invalid chess clock snapshot");
		}
	}

	private ChessClock(long initialMillis, long incrementMillis) {
		if (initialMillis < 1 || initialMillis > MAX_TIME_MILLIS || incrementMillis < 0 || incrementMillis > MAX_TIME_MILLIS)
			throw new IllegalArgumentException("Invalid chess time control");
		whiteMillis = blackMillis = initialMillis;
		this.incrementMillis = incrementMillis;
	}

	private ChessClock(Snapshot snapshot) {
		whiteMillis = snapshot.whiteMillis();
		blackMillis = snapshot.blackMillis();
		incrementMillis = snapshot.incrementMillis();
	}

	public static ChessClock create(long initialMillis, long incrementMillis) { return new ChessClock(initialMillis, incrementMillis); }
	public static ChessClock restore(Snapshot snapshot) { return new ChessClock(snapshot); }
	public long incrementMillis() { return incrementMillis; }
	public boolean isRunning() { return activeSide != null; }
	public Side activeSide() { return activeSide; }

	public void start(Side side, long nowMillis) {
		if (side == null || nowMillis < 0) throw new IllegalArgumentException("Invalid chess clock start");
		activeSide = side;
		startedAtMillis = nowMillis;
	}

	public long remainingMillis(Side side, long nowMillis) {
		long stored = side == Side.WHITE ? whiteMillis : blackMillis;
		if (activeSide != side || startedAtMillis < 0) return stored;
		return Math.max(0, stored - Math.max(0, nowMillis - startedAtMillis));
	}

	public Side flaggedSide(long nowMillis) {
		if (activeSide == null || startedAtMillis < 0) return null;
		return remainingMillis(activeSide, nowMillis) == 0 ? activeSide : null;
	}

	/** Completes one turn, charging elapsed time and increment before starting the opponent. */
	public boolean completeTurn(Side movingSide, Side nextSide, long nowMillis) {
		if (movingSide == null || nextSide == null || movingSide == nextSide || activeSide != movingSide || flaggedSide(nowMillis) != null) return false;
		long remaining = remainingMillis(movingSide, nowMillis);
		setRemaining(movingSide, Math.min(MAX_TIME_MILLIS, remaining + incrementMillis));
		start(nextSide, nowMillis);
		return true;
	}

	/** Save a stable remainder and pause. Server restart recovery deliberately resumes paused. */
	public boolean checkpoint(long nowMillis) {
		if (activeSide == null) return false;
		Side side = activeSide;
		setRemaining(side, remainingMillis(side, nowMillis));
		activeSide = null;
		startedAtMillis = -1;
		return true;
	}

	public Snapshot snapshot(long nowMillis) {
		return new Snapshot(remainingMillis(Side.WHITE, nowMillis), remainingMillis(Side.BLACK, nowMillis), incrementMillis);
	}

	private void setRemaining(Side side, long millis) {
		if (side == Side.WHITE) whiteMillis = millis;
		else blackMillis = millis;
	}
}
