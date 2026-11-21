package com.example.arcane.client;

import com.example.arcane.chess.ChessPosition;
import com.example.arcane.chess.ChessPiece;
import com.example.arcane.chess.ChessMove;
import com.example.arcane.chess.ChessMatch;
import com.example.arcane.chess.Side;
import com.example.arcane.chess.ChessAi;
import com.example.arcane.chess.ChessBoardCoordinates;
import com.example.arcane.chess.ChessPgn;
import com.example.arcane.chess.Piece;
import com.example.arcane.network.ChessActionPayload;
import com.example.arcane.network.MatchStatePayload;
import com.example.arcane.network.MatchClockPayload;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class ChessScreen extends Screen {
	private ChessPosition position;
	private final boolean online;
	private final boolean computerOpponent;
	private final boolean replayMode;
	private final ChessPgn.Replay replay;
	private final Screen replayParent;
	private final Side humanSide;
	private final ChessAi.Difficulty aiDifficulty;
	private boolean aiThinking;
	private UUID matchId, whitePlayer, blackPlayer, drawOfferer;
	private int ply, whiteElo, blackElo;
	private boolean clockEnabled;
	private long whiteClockMillis, blackClockMillis;
	private boolean yourTurn, drawOfferToYou, spectator, pending;
	private boolean matchActive;
	private String selected;
	private int boardX, boardY, squareSize;
	private int panelWidth;
	private Button drawButton, resignButton, rotateButton, returnButton, previousButton, nextButton;
	private int replayIndex;
	private Button[] promotionButtons;
	private boolean dragging, dragMoved;
	private double dragStartX, dragStartY;
	private double dragOffsetX, dragOffsetY;
	private String dragTarget;
	private boolean blackPerspective;
	private boolean boardRotated;
	private Set<String> legalDestinations = Set.of();
	private String lastMoveFrom, lastMoveTo;
	private String statusText;
	private int statusColor;
	private boolean terminal;
	private ChessMove promotionMove;
	private int promotionX, promotionY, promotionCell;

	public ChessScreen(ChessPosition position) {
		super(Component.literal("Arcane Games — Chess"));
		this.position = position;
		this.online = false;
		this.computerOpponent = false;
		this.replayMode = false;
		this.replay = null;
		this.replayParent = null;
		this.humanSide = Side.WHITE;
		this.aiDifficulty = null;
		updateStatus();
	}

	public ChessScreen(ChessPosition position, Side humanSide, ChessAi.Difficulty difficulty) {
		super(Component.literal("Arcane Games Chess"));
		this.position = position;
		this.online = false;
		this.computerOpponent = true;
		this.replayMode = false;
		this.replay = null;
		this.replayParent = null;
		this.humanSide = humanSide;
		this.aiDifficulty = difficulty;
		updateStatus();
		startComputerTurn();
	}

	public ChessScreen(MatchStatePayload state) {
		super(Component.literal("Arcane Games — Chess"));
		this.position = ChessPosition.fromFen(state.fen());
		this.online = true;
		this.computerOpponent = false;
		this.replayMode = false;
		this.replay = null;
		this.replayParent = null;
		this.humanSide = Side.WHITE;
		this.aiDifficulty = null;
		apply(state);
	}

	public ChessScreen(ChessPgn.Replay replay) {
		this(replay, null);
	}

	public ChessScreen(ChessPgn.Replay replay, Screen replayParent) {
		super(Component.literal("Arcane Games — Public Chess Replay"));
		this.replay = replay;
		this.replayParent = replayParent;
		this.replayMode = true;
		this.position = ChessPosition.fromFen(replay.positions().get(0));
		this.online = false;
		this.computerOpponent = false;
		this.humanSide = Side.WHITE;
		this.aiDifficulty = null;
		this.statusText = "Completed public game · " + replay.result();
		this.statusColor = 0xFFFFD879;
		this.terminal = true;
	}

	public UUID matchId() { return matchId; }

	@Override
	protected void init() {
		drawButton = resignButton = rotateButton = returnButton = previousButton = nextButton = null;
		promotionButtons = null;
		updateLayout();
		int panelX = boardX + squareSize * 8 + 18;
		int buttonWidth = Math.max(48, panelWidth - 12);
		if (online) {
			drawButton = addRenderableWidget(Button.builder(Component.literal("Offer draw"), button -> submitDrawAction())
					.bounds(panelX - 3, boardY + squareSize * 8 - 54, buttonWidth, 20).build());
			resignButton = addRenderableWidget(Button.builder(Component.literal("Resign"), button -> sendOnlineAction(ChessActionPayload.RESIGN))
					.bounds(panelX - 3, boardY + squareSize * 8 - 29, buttonWidth, 20).build());
		} else if (replayMode) {
			previousButton = addRenderableWidget(Button.builder(Component.literal("Previous move"), button -> stepReplay(-1))
					.bounds(panelX - 3, boardY + 163, buttonWidth, 20).build());
			nextButton = addRenderableWidget(Button.builder(Component.literal("Next move"), button -> stepReplay(1))
					.bounds(panelX - 3, boardY + 188, buttonWidth, 20).build());
		} else if (computerOpponent) {
			rotateButton = addRenderableWidget(Button.builder(Component.literal("Rotate board"), button -> {
				boardRotated = !boardRotated;
				selected = null;
				legalDestinations = Set.of();
			}).bounds(panelX - 3, boardY + 143, buttonWidth, 20).build());
		}
		returnButton = addRenderableWidget(Button.builder(Component.literal(replayMode ? "Return to archive" : "Return to world"), button -> Minecraft.getInstance().gui.setScreen(replayMode && replayParent != null ? replayParent : null))
				.bounds(panelX - 3, boardY + squareSize * 8 + 14, buttonWidth, 20).build());
		promotionButtons = new Button[4];
		String[] labels = {"Queen", "Rook", "Bishop", "Knight"};
		for (int i = 0; i < promotionButtons.length; i++) {
			final int choice = i;
			int cell = Math.min(42, Math.max(30, squareSize));
			int x = boardX + squareSize * 4 - cell * 2 + i * cell;
			int y = boardY + squareSize * 4 - cell / 2;
			promotionButtons[i] = addRenderableWidget(Button.builder(Component.literal(labels[i]), button -> choosePromotion(choice))
					.bounds(x, y, cell - 2, cell - 2).build());
			promotionButtons[i].visible = false;
		}
		updateActionControls();
	}

	public void apply(MatchStatePayload state) {
		if (!online || (matchId != null && !matchId.equals(state.matchId()))) return;
		matchId = state.matchId();
		ply = state.ply();
		ChessPosition nextPosition = ChessPosition.fromFen(state.fen());
		updateLastMove(position, nextPosition);
		position = nextPosition;
		whitePlayer = state.whitePlayer();
		blackPlayer = state.blackPlayer();
		whiteElo = state.whiteElo();
		blackElo = state.blackElo();
		clockEnabled = state.clockEnabled();
		whiteClockMillis = state.whiteClockMillis();
		blackClockMillis = state.blackClockMillis();
		drawOfferer = state.drawOfferer();
		yourTurn = state.yourTurn();
		drawOfferToYou = state.drawOfferToYou();
		spectator = state.spectator();
		pending = false;
		selected = null;
		promotionMove = null;
		legalDestinations = Set.of();
		updateStatus(state.status());
		updateActionControls();
	}

	public void applyClock(MatchClockPayload update) {
		if (!online || !clockEnabled || matchId == null || !matchId.equals(update.matchId())) return;
		whiteClockMillis = update.whiteMillis();
		blackClockMillis = update.blackMillis();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		updateLayout();
		int boardPixels = squareSize * 8;
		int gap = 18;
		int panelX = boardX + boardPixels + 18;
		String hoveredSquare = squareAt(mouseX, mouseY);
		graphics.text(font, "ARCANE GAMES  /  CHESS", boardX, Math.max(4, boardY - 22), 0xFFFFD879, true);
		graphics.fill(boardX - 3, boardY - 3, boardX + boardPixels + 3, boardY + boardPixels + 3, 0xFF241B19);

		for (int rank = 7; rank >= 0; rank--) for (int file = 0; file < 8; file++) {
			String square = "" + (char) ('a' + file) + (char) ('1' + rank);
			int visualFile = blackPerspective ? 7 - file : file;
			int x = boardX + visualFile * squareSize;
			int y = boardY + (blackPerspective ? rank : 7 - rank) * squareSize;
			boolean pale = (file + rank) % 2 == 0;
			int base = pale ? 0xFFD8C59F : 0xFF62513F;
			boolean hovered = square.equals(hoveredSquare) && !square.equals(selected);
			if (square.equals(lastMoveFrom) || square.equals(lastMoveTo)) base = pale ? 0xFFE1C878 : 0xFF8A7340;
			if (square.equals(selected)) base = 0xFFC99A43;
			if (square.equals(dragTarget) && legalDestinations.contains(square)) base = pale ? 0xFFEAD880 : 0xFF9B833F;
			graphics.fill(x, y, x + squareSize, y + squareSize, base);
			if (hovered) {
				int edge = Math.max(1, squareSize / 24);
				int hoverColor = 0xCCFFD879;
				graphics.fill(x, y, x + squareSize, y + edge, hoverColor);
				graphics.fill(x, y + squareSize - edge, x + squareSize, y + squareSize, hoverColor);
				graphics.fill(x, y, x + edge, y + squareSize, hoverColor);
				graphics.fill(x + squareSize - edge, y, x + squareSize, y + squareSize, hoverColor);
			}
			if (legalDestinations.contains(square)) {
				ChessPiece target = position.pieceAt(square);
				if (target == null) {
					int dot = Math.max(3, squareSize / 8);
					graphics.fill(x + squareSize / 2 - dot, y + squareSize / 2 - dot, x + squareSize / 2 + dot, y + squareSize / 2 + dot, 0xBB263421);
				} else {
					int ring = Math.max(2, squareSize / 12);
					graphics.fill(x, y, x + squareSize, y + ring, 0xDD9B3434);
					graphics.fill(x, y + squareSize - ring, x + squareSize, y + squareSize, 0xDD9B3434);
					graphics.fill(x, y, x + ring, y + squareSize, 0xDD9B3434);
					graphics.fill(x + squareSize - ring, y, x + squareSize, y + squareSize, 0xDD9B3434);
				}
			}
			ChessPiece piece = position.pieceAt(square);
			if (piece != null && !(dragging && dragMoved && square.equals(selected))) drawPiece(graphics, piece, x, y, squareSize);
		}
		if (dragging && dragMoved && selected != null) {
			ChessPiece moving = position.pieceAt(selected);
			if (moving != null) drawPiece(graphics, moving, (int) (mouseX - dragOffsetX), (int) (mouseY - dragOffsetY), squareSize);
		}
		drawCoordinates(graphics, boardPixels);

		graphics.fill(panelX - 9, boardY - 3, Math.min(width - 8, panelX + panelWidth), boardY + boardPixels + 3, 0xE8171420);
		graphics.fill(panelX - 9, boardY - 3, Math.min(width - 8, panelX + panelWidth), boardY, 0xFFE0B759);
		graphics.fill(panelX - 9, boardY + boardPixels + 9, Math.min(width - 8, panelX + panelWidth), boardY + boardPixels + 39, 0xE8171420);
		if (promotionMove != null) {
			promotionCell = Math.min(42, Math.max(30, squareSize));
			promotionX = boardX + boardPixels / 2 - promotionCell * 2;
			promotionY = boardY + boardPixels / 2 - promotionCell / 2;
			graphics.fill(promotionX - 6, promotionY - 6, promotionX + promotionCell * 4 + 6, promotionY + promotionCell + 6, 0xF01B1722);
		}
		String turn = replayMode ? "Public game replay" : online ? (spectator ? terminal ? "Final board" : "Live spectator" : statusText.equals("Waiting for a rival") ? "Waiting for rival" : yourTurn ? "Your move" : terminal ? "Game finished" : "Opponent's move")
				: terminal ? "Game finished" : aiThinking ? "Arcane is thinking..." : computerOpponent ? "You play " + (humanSide == Side.WHITE ? "White" : "Black") : position.turn() == Side.WHITE ? "White to move" : "Black to move";
		graphics.text(font, turn, panelX, boardY + 10, 0xFFFFFFFF, true);
		graphics.text(font, statusText, panelX, boardY + 31, statusColor, true);
		if (online) {
			graphics.text(font, "Match ID", panelX, boardY + 55, 0xFFFFD879, false);
			graphics.text(font, matchId.toString().substring(0, 18), panelX, boardY + 69, 0xFFD6CDE2, false);
			graphics.text(font, matchId.toString().substring(18), panelX, boardY + 83, 0xFFD6CDE2, false);
			graphics.text(font, "Join: /chess join", panelX, boardY + 101, 0xFF9B91A8, false);
			if (statusText.equals("Waiting for a rival")) graphics.text(font, "Share this ID", panelX, boardY + 116, 0xFF9B91A8, false);
			graphics.text(font, "White Elo  " + whiteElo + (clockEnabled ? "   " + formatClock(whiteClockMillis) : ""), panelX, boardY + 143, 0xFFFFE6B4, false);
			graphics.text(font, "Black Elo  " + (blackPlayer == null ? "—" : blackElo) + (clockEnabled ? "   " + formatClock(blackClockMillis) : ""), panelX, boardY + 157, 0xFFD8D0E2, false);
		} else if (replayMode) {
			graphics.text(font, replay.white(), panelX, boardY + 63, 0xFFFFE6B4, false);
			graphics.text(font, "vs " + replay.black(), panelX, boardY + 78, 0xFFD8D0E2, false);
			graphics.text(font, "Result: " + replay.result(), panelX, boardY + 101, 0xFFFFD879, false);
			graphics.text(font, "Ply " + replayIndex + " / " + replay.moves().size(), panelX, boardY + 122, 0xFFD8D0E2, false);
			graphics.text(font, "Read-only public replay", panelX, boardY + 145, 0xFF9B91A8, false);
		} else {
			graphics.text(font, "Click or drag a piece", panelX, boardY + 65, 0xFFD6CDE2, false);
			graphics.text(font, "choose a marked square.", panelX, boardY + 79, 0xFFD6CDE2, false);
			if (computerOpponent) {
				graphics.text(font, "Arcane opponent", panelX, boardY + 110, 0xFFFFD879, false);
				graphics.text(font, "Difficulty: " + aiDifficulty.name(), panelX, boardY + 124, 0xFFB9AFC8, false);
			} else {
				graphics.text(font, "Esc returns to world.", panelX, boardY + 110, 0xFF9B91A8, false);
				graphics.text(font, "Right-click clears selection.", panelX, boardY + 124, 0xFF9B91A8, false);
			}
		}
		super.extractRenderState(graphics, mouseX, mouseY, delta);
	}

	private void drawCoordinates(GuiGraphicsExtractor graphics, int boardPixels) {
		for (int i = 0; i < 8; i++) {
			int file = blackPerspective ? 7 - i : i;
			int rank = blackPerspective ? i + 1 : 8 - i;
			String fileLabel = String.valueOf((char) ('a' + file));
			String rankLabel = String.valueOf(rank);
			int x = boardX + i * squareSize + (squareSize - font.width(fileLabel)) / 2;
			int y = boardY + boardPixels + 4;
			int rankY = boardY + i * squareSize + (squareSize - font.lineHeight) / 2;
			graphics.text(font, fileLabel, x, y, 0xFFD8C59F, false);
			graphics.text(font, rankLabel, boardX - font.width(rankLabel) - 5, rankY, 0xFFD8C59F, false);
		}
	}

	private static String formatClock(long millis) {
		long seconds = Math.max(0, (millis + 999) / 1000);
		return String.format(java.util.Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
	}

	private void drawPiece(GuiGraphicsExtractor graphics, ChessPiece piece, int x, int y, int size) {
		int light = piece.side() == Side.WHITE ? 0xFFFFEAC0 : 0xFF413844;
		int main = piece.side() == Side.WHITE ? 0xFFE8D7B4 : 0xFF241F2B;
		int shade = piece.side() == Side.WHITE ? 0xFF9A7952 : 0xFF100E16;
		int gold = piece.side() == Side.WHITE ? 0xFFD2A94D : 0xFFB99143;
		int cx = x + size / 2;
		int unit = Math.max(2, size / 10);
		int baseY = y + size * 8 / 10;
		int baseW = size * 6 / 10;
		graphics.fill(cx - baseW / 2 + 1, baseY + 1, cx + baseW / 2 + 2, baseY + unit * 2, 0x66000000);
		graphics.fill(cx - baseW / 2, baseY, cx + baseW / 2, baseY + unit * 2, shade);
		graphics.fill(cx - baseW / 2 + unit / 2, baseY, cx + baseW / 2 - unit / 2, baseY + unit, main);
		graphics.fill(cx - baseW / 2, baseY - unit, cx + baseW / 2, baseY, gold);
		int stem = Math.max(3, size / 4);
		switch (piece.type()) {
			case PAWN -> {
				graphics.fill(cx - unit, y + size * 4 / 10, cx + unit + 1, baseY - unit, shade);
				graphics.fill(cx - unit, y + size * 4 / 10, cx + unit, baseY - unit * 2, main);
				int head = Math.max(4, size * 3 / 10);
				graphics.fill(cx - head / 2 + 1, y + size / 5 + 1, cx + head / 2 + 1, y + size / 5 + head, shade);
				graphics.fill(cx - head / 2, y + size / 5, cx + head / 2, y + size / 5 + head - 1, light);
				graphics.fill(cx - unit, y + size / 2, cx + unit, y + size / 2 + unit, gold);
			}
			case ROOK -> {
				int w = size * 5 / 10;
				graphics.fill(cx - w / 2 + 1, y + size / 4 + 1, cx + w / 2 + 1, baseY - unit + 1, shade);
				graphics.fill(cx - w / 2, y + size / 4, cx + w / 2, baseY - unit, main);
				graphics.fill(cx - w / 2, y + size / 4, cx + w / 2, y + size / 4 + unit, light);
				int crenel = w / 4;
				for (int i = -1; i <= 1; i++) graphics.fill(cx + i * crenel - unit / 2, y + size / 7, cx + i * crenel + unit, y + size / 4 + unit, light);
			}
			case KNIGHT -> {
				graphics.fill(cx - size * 3 / 10, y + size * 5 / 10, cx + size * 3 / 10, baseY - unit, main);
				graphics.fill(cx - size / 10, y + size * 3 / 10, cx + size * 2 / 10, y + size * 6 / 10, light);
				graphics.fill(cx + size / 10, y + size * 2 / 10, cx + size * 4 / 10, y + size * 4 / 10, light);
				graphics.fill(cx + size * 2 / 10, y + size / 4, cx + size * 4 / 10, y + size * 3 / 10, shade);
				graphics.fill(cx - size / 10, y + size * 2 / 10, cx + size / 10, y + size * 3 / 10, gold);
			}
			case BISHOP -> {
				graphics.fill(cx - stem / 2, y + size * 3 / 10, cx + stem / 2, baseY - unit, shade);
				graphics.fill(cx - stem / 2, y + size * 3 / 10, cx + stem / 2 - 1, baseY - unit * 2, main);
				graphics.fill(cx - size / 5, y + size / 4, cx + size / 5, y + size * 4 / 10, light);
				graphics.fill(cx - size / 10, y + size / 8, cx + size / 10, y + size / 4, gold);
			}
			case QUEEN -> {
				graphics.fill(cx - unit, y + size * 3 / 10, cx + unit, baseY - unit, shade);
				graphics.fill(cx - unit, y + size * 3 / 10, cx + unit - 1, baseY - unit * 2, main);
				graphics.fill(cx - size * 3 / 10, y + size * 3 / 10, cx + size * 3 / 10, y + size * 4 / 10, gold);
				for (int i = -1; i <= 1; i++) graphics.fill(cx + i * size / 5 - unit / 2, y + size / 5, cx + i * size / 5 + unit / 2 + 1, y + size * 3 / 10, light);
			}
			case KING -> {
				graphics.fill(cx - unit, y + size * 3 / 10, cx + unit, baseY - unit, shade);
				graphics.fill(cx - unit, y + size * 3 / 10, cx + unit - 1, baseY - unit * 2, main);
				graphics.fill(cx - size / 5, y + size / 4, cx + size / 5, y + size / 3, gold);
				graphics.fill(cx - unit / 2, y + size / 8, cx + unit / 2 + 1, y + size * 4 / 10, light);
				graphics.fill(cx - size / 6, y + size / 5, cx + size / 6, y + size / 4, light);
			}
		}
		graphics.fill(cx - baseW / 2, baseY + unit, cx + baseW / 2, baseY + unit * 2, gold);
	}

	private void updateStatus() {
		if (online) return;
		if (position.isCheckmate()) {
			statusText = "Checkmate";
			statusColor = 0xFFFF7777;
			terminal = true;
		} else if (position.isStalemate()) {
			statusText = "Stalemate";
			statusColor = 0xFFFFD879;
			terminal = true;
		} else if (position.isDraw()) {
			statusText = "Draw";
			statusColor = 0xFFFFD879;
			terminal = true;
		} else if (position.inCheck(position.turn())) {
			statusText = "Check!";
			statusColor = 0xFFFF7777;
			terminal = false;
		} else {
			statusText = "Local game";
			statusColor = 0xFFB9AFC8;
			terminal = false;
		}
	}

	private void updateStatus(ChessMatch.Status status) {
		if (status == ChessMatch.Status.WAITING) {
			statusText = "Waiting for a rival";
			statusColor = 0xFFFFD879;
			terminal = false;
		} else if (status == ChessMatch.Status.IN_PROGRESS) {
			matchActive = true;
			statusText = position.inCheck(position.turn()) ? "Check!" : drawOfferer != null ? "Draw offer pending" : "In progress";
			statusColor = position.inCheck(position.turn()) ? 0xFFFF7777 : 0xFFB9AFC8;
			terminal = false;
		} else {
			matchActive = false;
			statusText = switch (status) {
				case WHITE_WON -> "White wins";
				case BLACK_WON -> "Black wins";
				case DRAW -> "Draw";
				default -> "Game finished";
			};
			statusColor = status == ChessMatch.Status.DRAW ? 0xFFFFD879 : 0xFFFF7777;
			terminal = true;
		}
		updateActionControls();
	}

	private void selectPiece(String square) {
		selected = square;
		Set<String> targets = new HashSet<>();
		for (ChessMove move : position.legalMoves()) if (move.from().equals(square)) targets.add(move.to());
		legalDestinations = Set.copyOf(targets);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		updateLayout();
		// Let regular controls (promotion choices, match actions) receive their click first.
		// The chessboard is drawn directly by this screen, so it should not need a
		// transparent widget whose hitbox can drift from the scaled board geometry.
		if (super.mouseClicked(event, doubleClick)) return true;
		return handleBoardClick(event);
	}

	private boolean handleBoardClick(MouseButtonEvent event) {
		if (replayMode) return true;
		double mouseX = event.x();
		double mouseY = event.y();
		if (event.button() == 1) {
			selected = null;
			promotionMove = null;
			legalDestinations = Set.of();
			updateActionControls();
			return true;
		}
		if (promotionMove != null) {
			return true;
		}
		if (event.button() != 0 || squareSize == 0) return false;
		if (terminal) return true;
		if (computerOpponent && (aiThinking || position.turn() != humanSide)) return true;
		int localX = (int) mouseX - boardX;
		int localY = (int) mouseY - boardY;
		if (localX < 0 || localX >= squareSize * 8 || localY < 0 || localY >= squareSize * 8) return false;
		String square = squareAt(mouseX, mouseY);
		ChessPiece clicked = position.pieceAt(square);
		if (selected == null) {
			if (clicked != null && clicked.side() == position.turn() && (!online || yourTurn && !pending)) {
				selectPiece(square);
				beginDrag(event);
			}
			return true;
		}
		if (clicked != null && clicked.side() == position.turn()) {
			if (!online || yourTurn && !pending) {
				selectPiece(square);
				beginDrag(event);
			}
			return true;
		}
		for (ChessMove move : position.legalMoves()) if (move.from().equals(selected) && move.to().equals(square)) {
			if (move.promotion() != null) { promotionMove = move; updateActionControls(); }
			else commitMove(move);
			return true;
		}
		selected = null;
		legalDestinations = Set.of();
		return true;
	}

	private void choosePromotion(int choice) {
		if (promotionMove == null) return;
		Piece type = switch (choice) { case 0 -> Piece.QUEEN; case 1 -> Piece.ROOK; case 2 -> Piece.BISHOP; default -> Piece.KNIGHT; };
		ChessMove move = new ChessMove(promotionMove.from(), promotionMove.to(), type);
		promotionMove = null;
		commitMove(move);
		updateActionControls();
	}

	private void submitDrawAction() {
		sendOnlineAction(drawOfferer == null ? ChessActionPayload.OFFER_DRAW : ChessActionPayload.ACCEPT_DRAW);
	}

	private void sendOnlineAction(int action) {
		if (!online || !matchActive || spectator || pending || matchId == null) return;
		ClientPlayNetworking.send(new ChessActionPayload(matchId, ply, action, "", "", 0));
		pending = true;
		updateActionControls();
	}

	private void updateActionControls() {
		if (drawButton != null) {
			String label = drawOfferer == null ? "Offer draw" : drawOfferToYou ? "Accept draw" : "Draw offered";
			drawButton.setMessage(Component.literal(label));
			drawButton.visible = online && matchActive && !spectator;
			drawButton.active = drawButton.visible && !pending && (drawOfferer == null || drawOfferToYou);
		}
		if (resignButton != null) {
			resignButton.visible = online && matchActive && !spectator;
			resignButton.active = resignButton.visible && !pending;
		}
		if (rotateButton != null) rotateButton.visible = computerOpponent;
		if (previousButton != null) previousButton.active = replayMode && replayIndex > 0;
		if (nextButton != null) nextButton.active = replayMode && replayIndex < replay.moves().size();
		if (returnButton != null) returnButton.visible = true;
		if (promotionButtons != null) for (Button button : promotionButtons) button.visible = promotionMove != null;
	}

	private void beginDrag(MouseButtonEvent event) {
		dragging = true;
		dragMoved = false;
		dragStartX = event.x();
		dragStartY = event.y();
		int file = selected.charAt(0) - 'a';
		int rank = selected.charAt(1) - '1';
		int screenFile = blackPerspective ? 7 - file : file;
		int screenRow = blackPerspective ? rank : 7 - rank;
		dragOffsetX = event.x() - (boardX + screenFile * squareSize);
		dragOffsetY = event.y() - (boardY + screenRow * squareSize);
		dragTarget = null;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
		// Some Minecraft input paths report a sentinel button value on drag
		// continuation events. The initiating left press already owns this drag.
		if (!dragging || selected == null) return super.mouseDragged(event, deltaX, deltaY);
		if (Math.abs(event.x() - dragStartX) + Math.abs(event.y() - dragStartY) > 4) dragMoved = true;
		if (dragMoved) dragTarget = squareAt(event.x(), event.y());
		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (!dragging) return super.mouseReleased(event);
		dragging = false;
		String destination = squareAt(event.x(), event.y());
		if (dragMoved && destination != null && selected != null) {
			for (ChessMove move : position.legalMoves()) {
				if (move.from().equals(selected) && move.to().equals(destination)) {
					if (move.promotion() != null) { promotionMove = move; updateActionControls(); }
					else commitMove(move);
					break;
				}
			}
		}
		dragMoved = false;
		dragTarget = null;
		return true;
	}

	private String squareAt(double mouseX, double mouseY) {
		return ChessBoardCoordinates.squareAt(mouseX, mouseY, boardX, boardY, squareSize, blackPerspective);
	}

	private void updateLayout() {
		blackPerspective = (computerOpponent && humanSide == Side.BLACK) ^ boardRotated;
		panelWidth = Math.min(250, Math.max(118, width / 4));
		int gap = 18;
		int fitByWidth = (width - panelWidth - gap - 24) / 8;
		int fitByHeight = (height - 92) / 8;
		squareSize = Math.max(8, Math.min(56, Math.min(fitByWidth, fitByHeight)));
		int boardPixels = squareSize * 8;
		int sparePanel = width - boardPixels - gap - 20;
		if (sparePanel < panelWidth) panelWidth = Math.max(72, sparePanel);
		int layoutWidth = boardPixels + gap + panelWidth;
		boardX = Math.max(6, (width - layoutWidth) / 2);
		boardY = Math.max(26, Math.min(56, (height - boardPixels - 24) / 2));
	}

	@Override public boolean isPauseScreen() { return false; }

	private void commitMove(ChessMove move) {
		if (online) {
			int promotion = move.promotion() == null ? 0 : switch (move.promotion()) { case QUEEN -> 1; case ROOK -> 2; case BISHOP -> 3; case KNIGHT -> 4; default -> 0; };
			ClientPlayNetworking.send(new ChessActionPayload(matchId, ply, ChessActionPayload.MOVE, move.from(), move.to(), promotion));
			pending = true;
		} else {
			lastMoveFrom = move.from();
			lastMoveTo = move.to();
			position.play(move.from(), move.to(), move.promotion());
			updateStatus();
			startComputerTurn();
		}
		selected = null;
		legalDestinations = Set.of();
	}

	private void stepReplay(int direction) {
		if (!replayMode) return;
		replayIndex = Math.max(0, Math.min(replay.moves().size(), replayIndex + direction));
		position = ChessPosition.fromFen(replay.positions().get(replayIndex));
		lastMoveFrom = replayIndex == 0 ? null : replay.moves().get(replayIndex - 1).from();
		lastMoveTo = replayIndex == 0 ? null : replay.moves().get(replayIndex - 1).to();
		selected = null;
		legalDestinations = Set.of();
		updateActionControls();
	}

	private void startComputerTurn() {
		if (!computerOpponent || terminal || aiThinking || position.turn() == humanSide) return;
		aiThinking = true;
		String expectedFen = position.toFen();
		CompletableFuture.supplyAsync(() -> ChessAi.chooseMove(ChessPosition.fromFen(expectedFen), aiDifficulty))
				.whenComplete((move, error) -> Minecraft.getInstance().execute(() -> {
					if (Minecraft.getInstance().gui.screen() != this || !position.toFen().equals(expectedFen)) return;
					aiThinking = false;
					if (error != null || move == null || !position.play(move.from(), move.to(), move.promotion())) {
						statusText = "Opponent could not find a legal move";
						statusColor = 0xFFFF7777;
						return;
					}
			lastMoveFrom = move.from();
			lastMoveTo = move.to();
					updateStatus();
				}));
	}

	private void updateLastMove(ChessPosition before, ChessPosition after) {
		if (before == null) return;
		Side movingSide = before.turn();
		String kingFrom = null, kingTo = null;
		for (int rank = 1; rank <= 8; rank++) for (char file = 'a'; file <= 'h'; file++) {
			String square = "" + file + rank;
			ChessPiece oldPiece = before.pieceAt(square);
			ChessPiece newPiece = after.pieceAt(square);
			if (oldPiece != null && oldPiece.side() == movingSide && oldPiece.type() == Piece.KING &&
					(newPiece == null || newPiece.side() != movingSide || newPiece.type() != Piece.KING)) kingFrom = square;
			if (newPiece != null && newPiece.side() == movingSide && newPiece.type() == Piece.KING &&
					(oldPiece == null || oldPiece.side() != movingSide || oldPiece.type() != Piece.KING)) kingTo = square;
		}
		if (kingFrom != null && kingTo != null) {
			lastMoveFrom = kingFrom;
			lastMoveTo = kingTo;
			return;
		}
		String from = null, to = null;
		for (int rank = 1; rank <= 8; rank++) for (char file = 'a'; file <= 'h'; file++) {
			String square = "" + file + rank;
			ChessPiece oldPiece = before.pieceAt(square);
			ChessPiece newPiece = after.pieceAt(square);
			if (oldPiece != null && oldPiece.side() == movingSide && newPiece == null) from = square;
			if (newPiece != null && newPiece.side() == movingSide &&
					(oldPiece == null || oldPiece.side() != movingSide || oldPiece.type() != newPiece.type())) to = square;
		}
		if (from != null && to != null) {
			lastMoveFrom = from;
			lastMoveTo = to;
		}
	}
}
