package com.example.arcane.game;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Small central registry; each future game contributes one definition and its own adapters. */
public final class MinigameRegistry {
	private static final Map<String, MinigameDefinition> GAMES = new LinkedHashMap<>();

	private MinigameRegistry() {}

	public static synchronized void register(MinigameDefinition game) {
		if (game == null || game.id() == null || !game.id().matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
			throw new IllegalArgumentException("Minigame IDs must be namespaced identifiers");
		}
		if (game.displayName() == null || game.description() == null || game.minimumPlayers() < 1 || game.maximumPlayers() < game.minimumPlayers()) {
			throw new IllegalArgumentException("Invalid minigame metadata for " + game.id());
		}
		if (GAMES.putIfAbsent(game.id(), game) != null) throw new IllegalStateException("Duplicate minigame: " + game.id());
	}

	public static synchronized Optional<MinigameDefinition> get(String id) {
		return Optional.ofNullable(GAMES.get(id));
	}

	public static synchronized Collection<MinigameDefinition> all() {
		return List.copyOf(GAMES.values());
	}
}
