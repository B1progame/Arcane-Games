package com.example.arcane.client.board;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

/** Cached adapter output; expensive state decoding happens only when synchronized state changes. */
public final class BoardSceneRenderState extends BlockEntityRenderState {
	private String cachedGameId;
	private String cachedCurrent;
	private String cachedPrevious;
	BoardScene scene;
	long animationStartedAt;
	boolean deployed;

	void update(String gameId, String current, String previous) {
		if (java.util.Objects.equals(cachedGameId, gameId) && java.util.Objects.equals(cachedCurrent, current)
				&& java.util.Objects.equals(cachedPrevious, previous)) return;
		cachedGameId = gameId;
		cachedCurrent = current;
		cachedPrevious = previous;
		scene = BoardSceneRegistry.get(gameId).map(source -> source.create(current, previous)).orElse(null);
	}
}
