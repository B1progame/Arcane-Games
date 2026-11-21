package com.example.arcane.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.net.URI;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;

/** First-run choice screen. Chess.com authentication stays in the user's own browser. */
final class ChesscomWelcomeScreen extends Screen {
	private final Screen parent;
	private int panelLeft;
	private int panelTop;
	private int panelWidth;
	private Button signIn;
	private Button createAccount;
	private Button connectProfile;

	ChesscomWelcomeScreen(Screen parent) {
		super(Component.literal("Arcane Games account options"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		panelWidth = Math.min(420, width - 24);
		int panelHeight = panelWidth >= 280 ? 240 : 280;
		panelLeft = (width - panelWidth) / 2;
		panelTop = Math.max(8, (height - panelHeight) / 2);
		int innerWidth = panelWidth - 32;
		int buttonY = panelTop + 150;
		if (panelWidth >= 280) {
			addRenderableWidget(Button.builder(linkToggleLabel(), button -> {
				ChesscomPreferences.setChesscomLinksEnabled(!ChesscomPreferences.areChesscomLinksEnabled());
				button.setMessage(linkToggleLabel());
				signIn.active = ChesscomPreferences.areChesscomLinksEnabled();
				createAccount.active = ChesscomPreferences.areChesscomLinksEnabled();
			}).bounds(panelLeft + 16, panelTop + 126, innerWidth, 20).build());
		}
		if (panelWidth >= 280) {
			int gap = 8;
			int buttonWidth = (innerWidth - gap) / 2;
			signIn = addRenderableWidget(Button.builder(Component.literal("Chess.com sign in"), button -> openWebsite("https://www.chess.com/login"))
					.bounds(panelLeft + 16, buttonY, buttonWidth, 24).build());
			createAccount = addRenderableWidget(Button.builder(Component.literal("Create account"), button -> openWebsite("https://www.chess.com/register"))
					.bounds(panelLeft + 16 + buttonWidth + gap, buttonY, buttonWidth, 24).build());
			connectProfile = addRenderableWidget(Button.builder(Component.literal("Connect public profile"), button -> openProfileScreen())
					.bounds(panelLeft + 16, buttonY + 31, innerWidth, 22).build());
			signIn.active = createAccount.active = ChesscomPreferences.areChesscomLinksEnabled();
			addRenderableWidget(Button.builder(Component.literal("Play as Guest"), button -> continueAsGuest())
					.bounds(panelLeft + 16, buttonY + 59, innerWidth, 22).build());
		} else {
			signIn = addRenderableWidget(Button.builder(Component.literal("Chess.com sign in"), button -> openWebsite("https://www.chess.com/login"))
					.bounds(panelLeft + 16, buttonY, innerWidth, 24).build());
			createAccount = addRenderableWidget(Button.builder(Component.literal("Create Chess.com account"), button -> openWebsite("https://www.chess.com/register"))
					.bounds(panelLeft + 16, buttonY + 30, innerWidth, 24).build());
			connectProfile = addRenderableWidget(Button.builder(Component.literal("Connect public profile"), button -> openProfileScreen())
					.bounds(panelLeft + 16, buttonY + 58, innerWidth, 22).build());
			signIn.active = createAccount.active = ChesscomPreferences.areChesscomLinksEnabled();
			addRenderableWidget(Button.builder(Component.literal("Play as Guest"), button -> continueAsGuest())
					.bounds(panelLeft + 16, buttonY + 84, innerWidth, 22).build());
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		graphics.fill(0, 0, width, height, 0xCC090712);
		int height = panelWidth >= 280 ? 240 : 280;
		graphics.fill(panelLeft - 1, panelTop - 1, panelLeft + panelWidth + 1, panelTop + height + 1, 0xFF09070D);
		graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + height, 0xF01C1724);
		graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + 2, 0xFFE0B759);
		graphics.fill(panelLeft, panelTop + height - 2, panelLeft + panelWidth, panelTop + height, 0xFF5E482D);
		graphics.fill(panelLeft + 12, panelTop + 12, panelLeft + 14, panelTop + height - 12, 0xFF54412E);
		graphics.fill(panelLeft + panelWidth - 14, panelTop + 12, panelLeft + panelWidth - 12, panelTop + height - 12, 0xFF54412E);
		graphics.centeredText(font, "ARCANE GAMES", width / 2, panelTop + 20, 0xFFFFD879);
		graphics.centeredText(font, "Chess.com profile", width / 2, panelTop + 45, 0xFFFFFFFF);
		boolean compact = panelWidth < 360;
		boolean narrow = panelWidth < 280;
		graphics.centeredText(font, narrow ? "Use Chess.com in your browser." : compact ? "Sign in or register on Chess.com." : "Sign in or register in your web browser.", width / 2, panelTop + 64, 0xFFD8D0E2);
		graphics.centeredText(font, narrow ? "Arcane never sees your password." : compact ? "Arcane never sees your password." : "Arcane never sees your Chess.com password.", width / 2, panelTop + 80, 0xFFB7AEC5);
		graphics.centeredText(font, narrow ? "Public profiles are read-only." : compact ? "Public profile lookup is read-only." : "Public profiles connect read-only; no login is shared.", width / 2, panelTop + 96, 0xFFB7AEC5);
		graphics.centeredText(font, narrow ? "Live play needs approval." : compact ? "Live Chess.com play needs approval." : "Chess.com live play needs Chess.com approval.", width / 2, panelTop + 112, 0xFFB7AEC5);
		graphics.centeredText(font, narrow ? "Independent project; no affiliation." : compact ? "Independent; not affiliated with Chess.com." : "Arcane is independent and not affiliated with Chess.com.", width / 2, panelTop + height - 22, 0xFF9B91A8);
		super.extractRenderState(graphics, mouseX, mouseY, delta);
	}

	private Component linkToggleLabel() {
		return Component.literal("Chess.com browser links: " + (ChesscomPreferences.areChesscomLinksEnabled() ? "ON" : "OFF"));
	}

	private void openWebsite(String address) {
		openExternal(this, address);
	}

	private void openProfileScreen() {
		Minecraft.getInstance().gui.setScreen(new ChesscomConnectScreen(this));
	}

	static void openExternal(Screen parent, String address) {
		URI uri = URI.create(address);
		boolean chessPage = "www.chess.com".equalsIgnoreCase(uri.getHost());
		boolean oauthApplication = "docs.google.com".equalsIgnoreCase(uri.getHost())
				&& "/forms/d/e/1FAIpQLSds2AeKLj9xqgu96Pu-rEAS0ItyqDbZbSgUFer0Mo6qMRx4Jg/viewform".equals(uri.getPath());
		if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getUserInfo() != null || uri.getPort() != -1 || (!chessPage && !oauthApplication)) {
			throw new IllegalArgumentException("Only approved Chess.com pages and its OAuth application form may be opened");
		}
		ConfirmLinkScreen.confirmLinkNow(parent, uri);
	}

	private void continueAsGuest() {
		ChesscomPreferences.completeWelcome();
		Minecraft.getInstance().gui.setScreen(parent);
	}

	@Override
	public void onClose() {
		continueAsGuest();
	}

	@Override
	public boolean isPauseScreen() { return true; }
}
