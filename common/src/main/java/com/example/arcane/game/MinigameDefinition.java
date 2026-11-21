package com.example.arcane.game;

/** Loader-independent description used by the Tome menu and server game discovery. */
public interface MinigameDefinition {
	String id();
	String displayName();
	String description();
	int minimumPlayers();
	int maximumPlayers();
	boolean playable();
	default MinigamePresentation presentation() { return MinigamePresentation.SCREEN_ONLY; }
}
