package com.example.arcane.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Connects an Arcane client to a Chess.com public profile using the documented read-only API. */
final class ChesscomConnectScreen extends Screen {
	private final Screen parent;
	private int left;
	private int top;
	private int panelWidth;
	private int panelHeight;
	private boolean compact;
	private EditBox username;
	private Button connect;
	private Button disconnect;
	private Button openProfile;
	private Button importGames;
	private String status = "Save a public Chess.com profile. This does not sign you in.";
	private String profileUrl = "";
	private boolean loading;

	ChesscomConnectScreen(Screen parent) {
		super(Component.literal("Connect Chess.com public profile"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		compact = width < 380 || height < 260;
		panelWidth = Math.min(420, width - 24);
		panelHeight = Math.min(compact ? 212 : 244, height - 12);
		left = (width - panelWidth) / 2;
		top = Math.max(6, (height - panelHeight) / 2);
		int contentWidth = panelWidth - 32;
		int fieldY = top + (compact ? 80 : 91);
		int connectY = fieldY + 28;
		int actionsY = connectY + 27;
		username = addRenderableWidget(new EditBox(font, left + 16, fieldY, contentWidth, 20, Component.literal("Chess.com username")));
		username.setHint(Component.literal("Chess.com username"));
		username.setMaxLength(25);
		username.setValue(ChesscomPreferences.chesscomProfileUsername());
		if (!ChesscomPreferences.chesscomProfileUsername().isBlank()) {
			profileUrl = "https://www.chess.com/member/" + ChesscomPreferences.chesscomProfileUsername();
			status = "Saved public profile · connect to refresh.";
		}
		connect = addRenderableWidget(Button.builder(Component.literal("Save public profile"), button -> connectProfile())
				.bounds(left + 16, connectY, contentWidth, 20).build());
		int halfWidth = (contentWidth - 6) / 2;
		disconnect = addRenderableWidget(Button.builder(Component.literal("Disconnect"), button -> disconnectProfile())
				.bounds(left + 16, actionsY, halfWidth, 20).build());
		openProfile = addRenderableWidget(Button.builder(Component.literal("Open profile"), button -> openProfile())
				.bounds(left + 22 + halfWidth, actionsY, halfWidth, 20).build());
		importGames = addRenderableWidget(Button.builder(Component.literal("Import public game replays"), button -> Minecraft.getInstance().gui.setScreen(new ChesscomArchiveScreen(this)))
				.bounds(left + 16, actionsY + 23, contentWidth, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Done"), button -> returnToParent())
				.bounds(left + 16, actionsY + 47, contentWidth, 20).build());
		updateProfileControls();
		setInitialFocus(username);
	}

	private void connectProfile() {
		if (loading) return;
		String requestedUsername = username.getValue().trim();
		if (!ChesscomProfileApi.isValidUsername(requestedUsername)) {
			status = compact ? "Use 3–25 letters, digits, _ or -." : "Use 3–25 letters, numbers, underscores or hyphens.";
			return;
		}
		loading = true;
		connect.active = false;
		status = "Looking up the public Chess.com profile…";
		ChesscomProfileApi.lookup(requestedUsername).whenComplete((profile, error) -> Minecraft.getInstance().execute(() -> {
			loading = false;
			connect.active = true;
			if (error != null) {
				Throwable cause = error;
				if (cause instanceof java.util.concurrent.CompletionException && cause.getCause() != null) cause = cause.getCause();
				status = cause.getMessage() == null ? "Chess.com profile lookup failed." : cause.getMessage();
				return;
			}
			ChesscomPreferences.setChesscomProfileUsername(profile.username());
			ChesscomPreferences.setChesscomProfileDisplayName(profile.displayName());
			username.setValue(profile.username());
			profileUrl = profile.profileUrl();
			status = compact ? "Profile saved; ownership unverified." : "Public profile connected · ownership not verified.";
			updateProfileControls();
		}));
	}

	private void disconnectProfile() {
		ChesscomPreferences.setChesscomProfileUsername("");
		ChesscomPreferences.setChesscomProfileDisplayName("");
		username.setValue("");
		profileUrl = "";
		status = "Saved profile removed from this client.";
		updateProfileControls();
	}

	private void updateProfileControls() {
		boolean connected = !ChesscomPreferences.chesscomProfileUsername().isBlank();
		disconnect.active = connected;
		openProfile.active = connected;
		importGames.active = true;
	}

	private void openProfile() {
		if (!profileUrl.isBlank()) ChesscomWelcomeScreen.openExternal(this, profileUrl);
		else if (!ChesscomPreferences.chesscomProfileUsername().isBlank()) {
			String linked = ChesscomPreferences.chesscomProfileUsername();
			ChesscomWelcomeScreen.openExternal(this, "https://www.chess.com/member/" + linked);
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		graphics.fill(0, 0, width, height, 0x9D09070F);
		graphics.fill(left - 2, top - 2, left + panelWidth + 2, top + panelHeight + 2, 0xFF08060B);
		graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xF31B1722);
		graphics.fill(left, top, left + panelWidth, top + 2, 0xFFE0B759);
		graphics.fill(left + 1, top + 3, left + panelWidth - 1, top + 4, 0xFF58432D);
		graphics.fill(left, top + panelHeight - 2, left + panelWidth, top + panelHeight, 0xFF4A3828);
		int headerY = compact ? 10 : 18;
		graphics.centeredText(font, "ARCANE GAMES  /  CHESS.COM", width / 2, top + headerY, 0xFFFFD879);
		String connected = ChesscomPreferences.chesscomProfileUsername();
		graphics.centeredText(font, connected.isBlank() ? "Save a public Chess.com profile" : "Saved profile: " + connected,
				width / 2, top + (compact ? 28 : 42), 0xFFFFFFFF);
		graphics.centeredText(font, compact ? "Public read-only. No password or token." : "Read-only public data. No password or token is used.", width / 2, top + (compact ? 43 : 57), 0xFFD8D0E2);
		graphics.centeredText(font, compact ? "No login, ownership proof, or live play." : "No sign-in, ownership check, or live games.", width / 2, top + (compact ? 56 : 70), 0xFFB7AEC5);
		graphics.centeredText(font, status, width / 2, top + (compact ? 69 : 83), statusColor());
		super.extractRenderState(graphics, mouseX, mouseY, delta);
	}

	private int statusColor() {
		if (loading) return 0xFFFFD879;
		if (status.startsWith("Public profile connected") || status.startsWith("Profile saved") || status.startsWith("Saved public profile")) return 0xFFB7E3A1;
		if (status.startsWith("Use ") || status.startsWith("No public") || status.contains("failed") || status.contains("could not")
				|| status.contains("rate limiting") || status.contains("too large") || status.contains("unreadable") || status.contains("interrupted")) return 0xFFFFA59B;
		return 0xFFB7AEC5;
	}

	private void returnToParent() { Minecraft.getInstance().gui.setScreen(parent); }
	@Override public void onClose() { returnToParent(); }
	@Override public boolean isPauseScreen() { return true; }
}
