package com.example.arcane.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Native Minecraft settings page exposed through optional Fabric Mod Menu. */
final class ArcaneSettingsScreen extends Screen {
	private final Screen parent;
	private int left, top, panelWidth, panelHeight;
	private Button toggle, connectProfile, signIn, createAccount, requestAccess;
	private boolean compact;

	ArcaneSettingsScreen(Screen parent) {
		super(Component.literal("Arcane Games settings"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		compact = width < 460 || height < 244;
		panelWidth = Math.min(compact ? 390 : 380, width - 24);
		panelHeight = compact ? Math.min(248, height - 12) : 244;
		left = (width - panelWidth) / 2;
		top = Math.max(6, (height - panelHeight) / 2);
		if (compact) initCompact();
		else initWide();
	}

	private void initWide() {
		int rightX = left + 174;
		int rightWidth = panelWidth - 194;
		if (rightWidth < 150) {
			compact = true;
			panelWidth = Math.min(390, width - 24);
			panelHeight = Math.min(238, height - 12);
			left = (width - panelWidth) / 2;
			top = Math.max(6, (height - panelHeight) / 2);
			initCompact();
			return;
		}
		toggle = addRenderableWidget(Button.builder(toggleLabel(), button -> toggleLinks())
				.bounds(rightX, top + 69, rightWidth, 20).build());
		connectProfile = addRenderableWidget(Button.builder(Component.literal("Add public profile (read-only)"), button -> openProfileScreen())
				.bounds(rightX, top + 94, rightWidth, 20).build());
		signIn = addRenderableWidget(Button.builder(Component.literal("Open sign in page"), button -> ChesscomWelcomeScreen.openExternal(this, "https://www.chess.com/login"))
				.bounds(rightX, top + 119, rightWidth, 20).build());
		createAccount = addRenderableWidget(Button.builder(Component.literal("Create Chess.com account"), button -> ChesscomWelcomeScreen.openExternal(this, "https://www.chess.com/register"))
				.bounds(rightX, top + 144, rightWidth, 20).build());
		requestAccess = addRenderableWidget(Button.builder(Component.literal("Apply for Chess.com OAuth access"), button -> openOAuthApplication())
				.bounds(rightX, top + 169, rightWidth, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Done"), button -> closeScreen())
				.bounds(rightX, top + 195, rightWidth, 20).build());
		updateLinkControls();
	}

	private void initCompact() {
		int pad = 16;
		int contentWidth = panelWidth - pad * 2;
		int rowY = top + 152;
		int gap = 6;
		int actionWidth = (contentWidth - gap) / 2;
		toggle = addRenderableWidget(Button.builder(toggleLabel(), button -> toggleLinks())
				.bounds(left + pad, top + 105, contentWidth, 20).build());
		connectProfile = addRenderableWidget(Button.builder(Component.literal("Connect public Chess.com profile"), button -> openProfileScreen())
				.bounds(left + pad, top + 129, contentWidth, 20).build());
		signIn = addRenderableWidget(Button.builder(Component.literal("Open Chess.com sign in"), button -> ChesscomWelcomeScreen.openExternal(this, "https://www.chess.com/login"))
				.bounds(left + pad, rowY, actionWidth, 20).build());
		createAccount = addRenderableWidget(Button.builder(Component.literal("Create account"), button -> ChesscomWelcomeScreen.openExternal(this, "https://www.chess.com/register"))
				.bounds(left + pad + actionWidth + gap, rowY, actionWidth, 20).build());
		if (panelHeight >= 228) {
			requestAccess = addRenderableWidget(Button.builder(Component.literal("Apply for live-board access"), button -> openOAuthApplication())
					.bounds(left + pad, top + 178, contentWidth, 20).build());
			addRenderableWidget(Button.builder(Component.literal("Done"), button -> closeScreen())
					.bounds(left + pad, top + 202, contentWidth, 20).build());
		} else {
			addRenderableWidget(Button.builder(Component.literal("Done"), button -> closeScreen())
					.bounds(left + pad, top + 178, contentWidth, 20).build());
		}
		updateLinkControls();
	}

	private Component toggleLabel() {
		return Component.literal("Chess.com browser links: " + (ChesscomPreferences.areChesscomLinksEnabled() ? "ON" : "OFF"));
	}

	private void toggleLinks() {
		ChesscomPreferences.setChesscomLinksEnabled(!ChesscomPreferences.areChesscomLinksEnabled());
		toggle.setMessage(toggleLabel());
		updateLinkControls();
	}

	private void updateLinkControls() {
		boolean enabled = ChesscomPreferences.areChesscomLinksEnabled();
		signIn.active = enabled;
		createAccount.active = enabled;
	}

	private void openProfileScreen() {
		Minecraft.getInstance().gui.setScreen(new ChesscomConnectScreen(this));
	}

	private void openOAuthApplication() {
		ChesscomWelcomeScreen.openExternal(this, "https://docs.google.com/forms/d/e/1FAIpQLSds2AeKLj9xqgu96Pu-rEAS0ItyqDbZbSgUFer0Mo6qMRx4Jg/viewform?usp=send_form");
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		graphics.fill(0, 0, width, height, 0x9D09070F);
		graphics.fill(left - 2, top - 2, left + panelWidth + 2, top + panelHeight + 2, 0xFF08060B);
		graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xF31B1722);
		graphics.fill(left, top, left + panelWidth, top + 2, 0xFFE0B759);
		graphics.fill(left + 1, top + 3, left + panelWidth - 1, top + 4, 0xFF58432D);
		graphics.fill(left, top + panelHeight - 2, left + panelWidth, top + panelHeight, 0xFF4A3828);
		if (compact) drawCompact(graphics);
		else drawWide(graphics);
		super.extractRenderState(graphics, mouseX, mouseY, delta);
	}

	private void drawWide(GuiGraphicsExtractor graphics) {
		int rightX = left + 174;
		graphics.fill(left + 156, top + 18, left + 157, top + panelHeight - 18, 0xFF504033);
		drawEmblem(graphics, left + 46, top + 46, 64);
		graphics.text(font, "ARCANE GAMES", left + 22, top + 18, 0xFFFFD879, true);
		graphics.text(font, "CHESS EDITION", left + 40, top + 119, 0xFFCCB46F, false);
		graphics.text(font, "Independent Minecraft mod", left + 18, top + 143, 0xFFD8D0E2, false);
		graphics.text(font, "No Chess.com credentials are stored.", left + 18, top + 159, 0xFFB8B0C2, false);
		graphics.text(font, "CHESS.COM PUBLIC DATA", rightX, top + 20, 0xFFFFD879, true);
		graphics.fill(rightX - 8, top + 41, left + panelWidth - 18, top + 66, 0xFF292330);
		graphics.fill(rightX - 8, top + 41, rightX - 5, top + 66, 0xFFD0A84C);
		String profile = ChesscomPreferences.chesscomProfileUsername();
		String profileLabel = profile.isBlank() ? "Public profile: not added" : "Public profile: " + (profile.length() > 16 ? profile.substring(0, 13) + "…" : profile);
		graphics.text(font, profileLabel, rightX, top + 49, 0xFFFFE6B4, false);
		graphics.text(font, "Public data only; no password or token.", rightX, top + 58, 0xFFB8B0C2, false);
		graphics.text(font, "External sign-in does not connect a game.", rightX, top + 220, 0xFFE0B759, false);
	}

	private void drawCompact(GuiGraphicsExtractor graphics) {
		graphics.text(font, "ARCANE GAMES  /  CHESS", left + 17, top + 14, 0xFFFFD879, true);
		drawEmblem(graphics, left + 18, top + 39, 48);
		String profile = ChesscomPreferences.chesscomProfileUsername();
		graphics.text(font, "Chess.com public profile", left + 77, top + 42, 0xFFFFFFFF, true);
		graphics.text(font, profile.isBlank() ? "No profile saved" : "Saved (unverified): " + profile, left + 77, top + 59, 0xFFD8D0E2, false);
		graphics.text(font, "Public profile lookup is read-only.", left + 17, top + 81, 0xFFB8B0C2, false);
		graphics.text(font, "Verified account link needs approval.", left + 17, top + 94, 0xFFE0B759, false);
		graphics.text(font, "Guest chess stays available offline.", left + 17, top + panelHeight - 16, 0xFFAAA1B5, false);
	}

	/** Code-drawn pixel emblem keeps the small settings screen sharp at every GUI scale. */
	private void drawEmblem(GuiGraphicsExtractor g, int x, int y, int size) {
		int u = Math.max(2, size / 16);
		g.fill(x + u, y + size / 2, x + size - u, y + size - u, 0xFF4B2B49);
		g.fill(x, y + size * 3 / 4, x + size, y + size - u, 0xFF241923);
		g.fill(x + u, y + size / 2, x + size - u, y + size - u, 0xFF593548);
		g.fill(x + u, y + size - u * 2, x + size - u, y + size - u, 0xFFD4A64D);
		g.fill(x + size / 4, y + size / 2, x + size * 3 / 4, y + size * 2 / 3, 0xFFEAD8B1);
		g.fill(x + size / 5, y + size / 2, x + size * 4 / 5, y + size * 2 / 3, 0xFF35242D);
		g.fill(x + size * 3 / 8, y + size / 3, x + size * 5 / 8, y + size * 3 / 5, 0xFFE6C36E);
		g.fill(x + size * 7 / 16, y + size / 6, x + size * 9 / 16, y + size * 3 / 8, 0xFFFFDF83);
		g.fill(x + size * 5 / 16, y + size / 4, x + size * 11 / 16, y + size / 3, 0xFFDEA940);
		g.fill(x + size * 2 / 5, y + size / 10, x + size * 3 / 5, y + size / 5, 0xFFE6C36E);
		g.fill(x + size * 3 / 8, y + size * 2 / 3, x + size * 5 / 8, y + size * 3 / 4, 0xFFD3A64D);
	}

	private void closeScreen() {
		Minecraft.getInstance().gui.setScreen(parent);
	}

	@Override public void onClose() { closeScreen(); }
	@Override public boolean isPauseScreen() { return true; }
}
