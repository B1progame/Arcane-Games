package com.example.arcane.client.board;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Client-only adapter registry. A new game can supply its own scene source without changing the renderer. */
public final class BoardSceneRegistry {
	private static final Map<String, BoardSceneSource> SOURCES = new ConcurrentHashMap<>();
	private BoardSceneRegistry() {}

	public static void register(String gameId, BoardSceneSource source) {
		if (gameId == null || !gameId.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) throw new IllegalArgumentException("Invalid game id");
		if (SOURCES.putIfAbsent(gameId, source) != null) throw new IllegalStateException("Duplicate board scene source: " + gameId);
	}

	public static Optional<BoardSceneSource> get(String gameId) {
		return Optional.ofNullable(SOURCES.get(gameId));
	}
}
