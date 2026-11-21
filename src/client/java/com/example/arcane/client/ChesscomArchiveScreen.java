package com.example.arcane.client;

import com.example.arcane.chess.ChessPgn;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Read-only browser for completed games in a public Chess.com monthly archive. */
final class ChesscomArchiveScreen extends Screen {
	private final Screen parent;
	private int left, top, panelWidth, panelHeight;
	private EditBox username;
	private Button fetch;
	private final Button[] gameButtons = new Button[6];
	private List<ChessPgn.Replay> games = List.of();
	private String status = "Fetch the latest public monthly archive.";
	private String month = "";
	private boolean loading;

	ChesscomArchiveScreen(Screen parent) {
		super(Component.literal("Chess.com public game archive"));
		this.parent = parent;
	}

	@Override protected void init() {
		panelWidth = Math.min(480, width - 24);
		panelHeight = Math.min(292, height - 12);
		left = (width - panelWidth) / 2;
		top = Math.max(6, (height - panelHeight) / 2);
		int contentX = left + 16;
		int contentWidth = panelWidth - 32;
		username = addRenderableWidget(new EditBox(font, contentX, top + 54, contentWidth - 86, 20, Component.literal("Chess.com username")));
		username.setHint(Component.literal("Chess.com username"));
		username.setMaxLength(25);
		username.setValue(ChesscomPreferences.chesscomProfileUsername());
		fetch = addRenderableWidget(Button.builder(Component.literal("Fetch archive"), button -> fetch())
				.bounds(contentX + contentWidth - 82, top + 54, 82, 20).build());
		for (int i = 0; i < gameButtons.length; i++) {
			final int index = i;
			gameButtons[i] = addRenderableWidget(Button.builder(Component.literal(""), button -> openGame(index))
					.bounds(contentX, top + 91 + i * 23, contentWidth, 20).build());
			gameButtons[i].visible = false;
		}
		addRenderableWidget(Button.builder(Component.literal("Done"), button -> close())
				.bounds(contentX, top + panelHeight - 28, contentWidth, 20).build());
		setInitialFocus(username);
	}

	private void fetch() {
		if (loading) return;
		loading = true;
		fetch.active = false;
		status = "Fetching the latest public archive…";
		games = List.of();
		updateGameButtons();
		ChesscomArchiveApi.recentGames(username.getValue()).whenComplete((archive, error) -> Minecraft.getInstance().execute(() -> {
			if (Minecraft.getInstance().gui.screen() != this) return;
			loading = false;
			fetch.active = true;
			if (error != null) {
				Throwable cause = error instanceof java.util.concurrent.CompletionException && error.getCause() != null ? error.getCause() : error;
				status = cause.getMessage() == null ? "Public archive import failed." : cause.getMessage();
				return;
			}
			games = archive.games();
			month = archive.month();
			status = "Imported " + games.size() + " completed games from " + month + ".";
			updateGameButtons();
		}));
	}

	private void updateGameButtons() {
		for (int i = 0; i < gameButtons.length; i++) {
			boolean available = i < Math.min(gameButtons.length, Math.max(0, (panelHeight - 122) / 23)) && i < games.size();
			gameButtons[i].visible = available;
			gameButtons[i].active = available;
			if (available) {
				ChessPgn.Replay replay = games.get(games.size() - 1 - i);
				gameButtons[i].setMessage(Component.literal((i + 1) + ". " + shortName(replay.white()) + " vs " + shortName(replay.black()) + "   " + replay.result()));
			}
		}
	}

	private static String shortName(String name) { return name.length() > 16 ? name.substring(0, 13) + "…" : name; }

	private void openGame(int index) {
		int gameIndex = games.size() - 1 - index;
		if (gameIndex < 0 || gameIndex >= games.size()) return;
		Minecraft.getInstance().gui.setScreen(new ChessScreen(games.get(gameIndex), this));
	}

	@Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		graphics.fill(0, 0, width, height, 0x9D09070F);
		graphics.fill(left - 2, top - 2, left + panelWidth + 2, top + panelHeight + 2, 0xFF08060B);
		graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xF31B1722);
		graphics.fill(left, top, left + panelWidth, top + 2, 0xFFE0B759);
		graphics.centeredText(font, "ARCANE GAMES  /  PUBLIC REPLAYS", width / 2, top + 12, 0xFFFFD879);
		graphics.centeredText(font, "Read-only completed games · no live Chess.com access", width / 2, top + 30, 0xFFD8D0E2);
		graphics.centeredText(font, status, width / 2, top + 78, loading ? 0xFFFFD879 : 0xFFB9AFC8);
		graphics.centeredText(font, "Latest archive · select a game to step through positions", width / 2, top + panelHeight - 43, 0xFF9B91A8);
		super.extractRenderState(graphics, mouseX, mouseY, delta);
	}

	private void close() { Minecraft.getInstance().gui.setScreen(parent); }
	@Override public void onClose() { close(); }
}
