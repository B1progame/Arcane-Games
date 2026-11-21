package com.example.arcane.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TomeDeploymentTest {
	@Test
	void deploymentStartsClosedAndOpeningIsIdempotent() {
		TomeDeployment state = new TomeDeployment();
		assertFalse(state.isOpen());
		assertTrue(state.open(true));
		assertTrue(state.isOpen());
		assertFalse(state.open(true));
	}

	@Test
	void refusesDeploymentWithoutClearanceAndKeepsStateClosed() {
		TomeDeployment state = new TomeDeployment();
		assertFalse(state.open(false));
		assertFalse(state.isOpen());
	}

	@Test
	void restoringSavedDeploymentKeepsTheTomeOpenWithoutReplayingOpenTransition() {
		TomeDeployment state = new TomeDeployment();
		state.restore(true);
		assertTrue(state.isOpen());
		assertFalse(state.open(true));
	}
}
