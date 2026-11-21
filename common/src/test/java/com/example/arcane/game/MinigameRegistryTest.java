package com.example.arcane.game;

import org.junit.jupiter.api.Test;
import com.example.arcane.chess.ChessMinigame;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MinigameRegistryTest {
	@Test
	void chessDeclaresItsWorldAndScreenPresentation() {
		assertEquals(MinigamePresentation.HYBRID, ChessMinigame.INSTANCE.presentation());
	}

	@Test
	void registersAndFindsAnAdditionalGameWithoutChangingRegistryCode() {
		String id = "test:" + UUID.randomUUID().toString().replace('-', '_');
		MinigameDefinition checkers = new MinigameDefinition() {
			@Override public String id() { return id; }
			@Override public String displayName() { return "Checkers"; }
			@Override public String description() { return "A test adapter game"; }
			@Override public int minimumPlayers() { return 2; }
			@Override public int maximumPlayers() { return 2; }
			@Override public boolean playable() { return false; }
		};

		MinigameRegistry.register(checkers);

		assertSame(checkers, MinigameRegistry.get(id).orElseThrow());
		assertTrue(MinigameRegistry.all().contains(checkers));
	}

	@Test
	void rejectsUnnamespacedIdentifiersAndDuplicateRegistrations() {
		MinigameDefinition invalid = definition("Chess", 2, 2);
		assertThrows(IllegalArgumentException.class, () -> MinigameRegistry.register(invalid));

		String id = "test:" + UUID.randomUUID().toString().replace('-', '_');
		MinigameDefinition first = definition(id, 1, 4);
		MinigameRegistry.register(first);
		assertThrows(IllegalStateException.class, () -> MinigameRegistry.register(definition(id, 1, 2)));
	}

	private static MinigameDefinition definition(String id, int min, int max) {
		return new MinigameDefinition() {
			@Override public String id() { return id; }
			@Override public String displayName() { return "Test"; }
			@Override public String description() { return "Test game"; }
			@Override public int minimumPlayers() { return min; }
			@Override public int maximumPlayers() { return max; }
			@Override public boolean playable() { return false; }
		};
	}
}
