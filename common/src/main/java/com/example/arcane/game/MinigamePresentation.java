package com.example.arcane.game;

/** Broad UI/world surface hint; each game still owns its rules and concrete screens. */
public enum MinigamePresentation {
	/** A screen-driven game with no persistent board geometry in the world. */
	SCREEN_ONLY,
	/** A game represented in the world, optionally paired with a detailed screen. */
	WORLD_BOARD,
	/** A world/table representation plus a dedicated interactive screen. */
	HYBRID
}
