package com.example.arcane.chess;

import com.example.arcane.game.MinigameDefinition;
import com.example.arcane.game.MinigamePresentation;

/** Registry metadata for the first Arcane game. Chess rules remain implemented by ChessMatch/ChessPosition. */
public final class ChessMinigame implements MinigameDefinition {
	public static final ChessMinigame INSTANCE = new ChessMinigame();
	private ChessMinigame() {}
	@Override public String id() { return "arcane:chess"; }
	@Override public String displayName() { return "Chess"; }
	@Override public String description() { return "A classic strategy game for two players."; }
	@Override public int minimumPlayers() { return 2; }
	@Override public int maximumPlayers() { return 2; }
	@Override public boolean playable() { return true; }
	@Override public MinigamePresentation presentation() { return MinigamePresentation.HYBRID; }
}
