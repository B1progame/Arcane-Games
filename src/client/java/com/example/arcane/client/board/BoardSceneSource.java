package com.example.arcane.client.board;

/** Converts a game's bounded synchronized state into immutable client render data. */
@FunctionalInterface
public interface BoardSceneSource {
	BoardScene create(String currentState, String previousState);
}
