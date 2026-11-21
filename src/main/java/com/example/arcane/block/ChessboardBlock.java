package com.example.arcane.block;

import com.example.arcane.chess.ArcaneChessSavedData;
import com.example.arcane.chess.ChessMatch;
import com.example.arcane.network.ChessNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.network.chat.Component;

public final class ChessboardBlock extends Block implements EntityBlock {
	public ChessboardBlock(Properties properties) { super(properties); }

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new GameBoardBlockEntity(pos, state); }

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) return InteractionResult.SUCCESS;
		if (!(player instanceof ServerPlayer serverPlayer) || !(level.getBlockEntity(pos) instanceof GameBoardBlockEntity board)) return InteractionResult.PASS;
		if (!board.isOpen() && !hasDeploymentClearance(level, pos)) {
			serverPlayer.sendSystemMessage(Component.literal("Clear a 2x2 area and two blocks of headroom around the Tome before opening its chessboard."));
			return InteractionResult.FAIL;
		}
		board.openTome(true);
		ArcaneChessSavedData saved = ArcaneChessSavedData.get(serverPlayer.level().getServer());
		ChessMatch match;
		if (board.matchId() == null) {
			match = saved.create(serverPlayer.getUUID());
			board.setMatch(match.id(), match.fen());
		} else {
			match = saved.get(board.matchId());
			if (match == null) {
				serverPlayer.sendSystemMessage(Component.literal("This board is linked to a match that is no longer available."));
				return InteractionResult.FAIL;
			}
			if (!match.isParticipant(serverPlayer.getUUID())) {
				if (match.status() == ChessMatch.Status.WAITING && match.join(serverPlayer.getUUID())) {
					saved.markChanged();
					ChessNetworking.sync(serverPlayer.level().getServer(), match, false, false);
				} else if (match.status() == ChessMatch.Status.WAITING) {
					serverPlayer.sendSystemMessage(Component.literal("This match already has two players."));
					return InteractionResult.FAIL;
				}
			}
		}
		ChessNetworking.trackBoard(board, match);
		ChessNetworking.openFor(serverPlayer, match);
		return InteractionResult.SUCCESS;
	}

	private static boolean hasDeploymentClearance(Level level, BlockPos anchor) {
		for (int dx = -1; dx <= 0; dx++) for (int dz = -1; dz <= 0; dz++) {
			for (int dy = 1; dy <= 2; dy++) {
				if (!level.isEmptyBlock(anchor.offset(dx, dy, dz))) return false;
			}
		}
		return true;
	}
}
