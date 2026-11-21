package com.example.arcane.game;

/** Presentation-only state for the placed Tome. It does not alter chess rules or match state. */
public final class TomeDeployment {
	private boolean open;

	public boolean isOpen() { return open; }

	/** @return true only for the first transition into the deployed state. */
	public boolean open(boolean clearanceAvailable) {
		if (!clearanceAvailable || open) return false;
		open = true;
		return true;
	}

	public void restore(boolean open) {
		this.open = open;
	}
}
