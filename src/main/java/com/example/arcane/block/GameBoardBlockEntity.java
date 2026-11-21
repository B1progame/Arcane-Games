package com.example.arcane.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import com.example.arcane.game.MinigameRegistry;
import com.example.arcane.game.TomeDeployment;
import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;

import java.util.UUID;

public final class GameBoardBlockEntity extends BlockEntity implements GeoBlockEntity {
	private static final String START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
	private static final int MAX_GAME_STATE_CHARS = 8192;
	private static final RawAnimation OPEN_ANIMATION = RawAnimation.begin().thenPlayAndHold("animation.arcane.chessboard.open");
	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
	private final TomeDeployment deployment = new TomeDeployment();
	private UUID matchId;
	private String gameId = "arcane:chess";
	private String gameState = START_FEN;
	private String previousGameState = START_FEN;
	private long animationStartedAt;

	public GameBoardBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.GAME_BOARD, pos, state);
	}

	public UUID matchId() { return matchId; }
	public String gameId() { return gameId; }
	public String gameState() { return gameState; }
	public String previousGameState() { return previousGameState; }
	/** Chess compatibility accessor; new game integrations should use {@link #gameState()}. */
	public String positionFen() { return gameState; }
	/** Chess compatibility accessor; new game integrations should use {@link #previousGameState()}. */
	public String previousFen() { return previousGameState; }
	public long animationStartedAt() { return animationStartedAt; }
	public boolean isOpen() { return deployment.isOpen(); }

	public boolean openTome(boolean clearanceAvailable) {
		if (!deployment.open(clearanceAvailable)) return false;
		markGameStateChanged();
		return true;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<GameBoardBlockEntity>("tome_open", 0, state ->
				deployment.isOpen() ? state.setAndContinue(OPEN_ANIMATION) : PlayState.STOP));
	}

	@Override public AnimatableInstanceCache getAnimatableInstanceCache() { return geoCache; }

	public void setMatch(UUID matchId, String fen) {
		gameId = "arcane:chess";
		this.matchId = matchId;
		setGameState(fen);
	}

	public void setPositionFen(String fen) {
		setGameState(fen);
	}

	/** Select another registered minigame on an unclaimed Tome and seed its bounded serialized state. */
	public boolean selectGame(String nextGameId, String initialState) {
		if (matchId != null || MinigameRegistry.get(nextGameId).isEmpty() || initialState == null || initialState.length() > MAX_GAME_STATE_CHARS) return false;
		gameId = nextGameId;
		gameState = initialState;
		previousGameState = initialState;
		animationStartedAt = 0L;
		markGameStateChanged();
		return true;
	}

	public void setGameState(String state) {
		if (state == null || state.length() > MAX_GAME_STATE_CHARS) return;
		if (!gameState.equals(state)) {
			previousGameState = gameState;
			gameState = state;
			animationStartedAt = System.currentTimeMillis();
		}
		markGameStateChanged();
	}

	private void markGameStateChanged() {
		setChanged();
		if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		if (matchId != null) output.putString("match_id", matchId.toString());
		output.putString("game_id", gameId);
		output.putString("game_state", gameState);
		output.putBoolean("tome_open", deployment.isOpen());
		super.saveAdditional(output);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		String raw = input.getStringOr("match_id", "");
		try { matchId = raw.isEmpty() ? null : UUID.fromString(raw); }
		catch (IllegalArgumentException invalid) { matchId = null; }
		String loadedGameId = input.getStringOr("game_id", "arcane:chess");
		if (loadedGameId.length() <= 64 && loadedGameId.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) gameId = loadedGameId;
		String loadedState = input.getStringOr("game_state", input.getStringOr("position_fen", START_FEN));
		if (loadedState.length() <= MAX_GAME_STATE_CHARS) gameState = loadedState;
		deployment.restore(input.getBooleanOr("tome_open", false));
		previousGameState = input.getStringOr("previous_game_state", input.getStringOr("previous_fen", gameState));
		if (previousGameState.length() > MAX_GAME_STATE_CHARS) previousGameState = gameState;
		animationStartedAt = input.getLongOr("animation_started_at", 0L);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = super.getUpdateTag(registries);
		tag.putString("game_id", gameId);
		tag.putString("game_state", gameState);
		tag.putString("previous_game_state", previousGameState);
		tag.putLong("animation_started_at", animationStartedAt);
		tag.putBoolean("tome_open", deployment.isOpen());
		return tag;
	}
}
