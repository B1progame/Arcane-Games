package com.example.arcane.client;

import com.example.arcane.chess.ChessAi;
import com.example.arcane.chess.ChessPosition;
import com.example.arcane.chess.Side;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Local singleplayer setup for an offline chess opponent. */
final class ChessPracticeScreen extends Screen {
	private final Screen parent;
	private ChessAi.Difficulty difficulty = ChessAi.Difficulty.NORMAL;
	private Side humanSide = Side.WHITE;
	private int panelLeft, panelTop, panelWidth;

	ChessPracticeScreen(Screen parent) {
		super(Component.literal("Arcane chess practice"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		panelWidth = Math.min(380, width - 24);
		panelLeft = (width - panelWidth) / 2;
		panelTop = Math.max(8, (height - 252) / 2);
		int x = panelLeft + 16;
		int buttonWidth = panelWidth - 32;
		addRenderableWidget(Button.builder(Component.literal("Difficulty: " + label(difficulty)), button -> {
			difficulty = ChessAi.Difficulty.values()[(difficulty.ordinal() + 1) % ChessAi.Difficulty.values().length];
			button.setMessage(Component.literal("Difficulty: " + label(difficulty)));
		}).bounds(x, panelTop + 112, buttonWidth, 22).build());
		addRenderableWidget(Button.builder(Component.literal(sideLabel()), button -> {
			humanSide = humanSide == Side.WHITE ? Side.BLACK : Side.WHITE;
			button.setMessage(Component.literal(sideLabel()));
		}).bounds(x, panelTop + 142, buttonWidth, 22).build());
		addRenderableWidget(Button.builder(Component.literal("Start practice"), button -> startGame())
				.bounds(x, panelTop + 180, buttonWidth, 22).build());
		addRenderableWidget(Button.builder(Component.literal("Return to world"), button -> closeScreen())
				.bounds(x, panelTop + 210, buttonWidth, 22).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		graphics.fill(0, 0, width, height, 0x9908050E);
		graphics.fill(panelLeft - 1, panelTop - 1, panelLeft + panelWidth + 1, panelTop + 253, 0xFF08060B);
		graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + 252, 0xF01C1724);
		graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + 2, 0xFFE0B759);
		graphics.fill(panelLeft + 12, panelTop + 12, panelLeft + panelWidth - 12, panelTop + 13, 0xFF54412E);
		graphics.centeredText(font, "ARCANE GAMES  /  CHESS", width / 2, panelTop + 20, 0xFFFFD879);
		graphics.centeredText(font, "Play against a computer", width / 2, panelTop + 43, 0xFFFFFFFF);
		graphics.centeredText(font, "A local offline opponent. No account or internet", width / 2, panelTop + 70, 0xFFD8D0E2);
		graphics.centeredText(font, "connection is needed. This is not a Chess.com bot.", width / 2, panelTop + 85, 0xFFB7AEC5);
		graphics.centeredText(font, "The search runs in the background; your world stays safe.", width / 2, panelTop + 239, 0xFF9B91A8);
		super.extractRenderState(graphics, mouseX, mouseY, delta);
	}

	private void startGame() {
		Minecraft.getInstance().gui.setScreen(new ChessScreen(ChessPosition.initial(), humanSide, difficulty));
	}

	private String sideLabel() {
		return "Play as " + (humanSide == Side.WHITE ? "White" : "Black") + " (change side)";
	}

	private static String label(ChessAi.Difficulty difficulty) {
		return switch (difficulty) {
			case BEGINNER -> "Beginner";
			case EASY -> "Easy";
			case NORMAL -> "Normal";
			case HARD -> "Hard";
		};
	}

	private void closeScreen() { Minecraft.getInstance().gui.setScreen(parent); }
	@Override public void onClose() { closeScreen(); }
	@Override public boolean isPauseScreen() { return false; }
}
