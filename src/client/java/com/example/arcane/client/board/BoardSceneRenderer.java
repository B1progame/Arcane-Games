package com.example.arcane.client.board;

import com.example.arcane.block.GameBoardBlockEntity;
import com.example.arcane.block.ModBlockEntities;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.geckolib.renderer.GeoBlockRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Shared low-cost cuboid board renderer. Game-specific rules and token shapes live in scene adapters. */
public final class BoardSceneRenderer extends GeoBlockRenderer<GameBoardBlockEntity, BoardSceneRenderState> {
	private static final long MOVE_ANIMATION_MILLIS = 280L;
	private static final float BOARD_MIN_Y = 1.002f;
	private static final float TOKEN_HEIGHT = 0.32f;

	public BoardSceneRenderer(BlockEntityRendererProvider.Context context) { super(context, ModBlockEntities.GAME_BOARD); }
	@Override public BoardSceneRenderState createRenderState() { return new BoardSceneRenderState(); }

	@Override
	public void extractRenderState(GameBoardBlockEntity board, BoardSceneRenderState state, float tickProgress,
			Vec3 cameraPos, @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		super.extractRenderState(board, state, tickProgress, cameraPos, crumblingOverlay);
		state.update(board.gameId(), board.gameState(), board.previousGameState());
		state.animationStartedAt = board.animationStartedAt();
		state.deployed = board.isOpen();
	}

	@Override
	public void submit(BoardSceneRenderState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraState) {
		super.submit(state, matrices, queue, cameraState);
		if (!state.deployed) return;
		BoardScene scene = state.scene;
		if (scene == null) return;
		long elapsed = System.currentTimeMillis() - state.animationStartedAt;
		float progress = state.animationStartedAt == 0 ? 1f : Math.min(1f, Math.max(0f, elapsed / (float) MOVE_ANIMATION_MILLIS));
		float eased = progress * progress * (3f - 2f * progress);
		int light = state.lightCoords;
		matrices.pushPose();
		matrices.translate(0.5, 0, 0.5);
		queue.submitCustomGeometry(matrices, RenderTypes.solidMovingBlock(), (pose, vertices) ->
				drawScene(pose, vertices, scene, light, eased, progress));
		matrices.popPose();
	}

	private void drawScene(PoseStack.Pose pose, VertexConsumer out, BoardScene scene, int light, float eased, float progress) {
		float minX = -scene.width() / 2f, maxX = scene.width() / 2f;
		float minZ = -scene.depth() / 2f, maxZ = scene.depth() / 2f;
		float frameTop = 1f + scene.deckHeight();
		float tileTop = frameTop + 0.014f;
		box(pose, out, minX, BOARD_MIN_Y, minZ, maxX, frameTop, maxZ, scene.frameColor(), light);
		float rail = Math.min(0.024f, Math.min(scene.width() / scene.columns(), scene.depth() / scene.rows()) * 0.09f);
		box(pose, out, minX, frameTop, minZ, maxX, frameTop + rail, minZ + rail, 0xFFD1A64B, light);
		box(pose, out, minX, frameTop, maxZ - rail, maxX, frameTop + rail, maxZ, 0xFFD1A64B, light);
		box(pose, out, minX, frameTop, minZ, minX + rail, frameTop + rail, maxZ, 0xFFD1A64B, light);
		box(pose, out, maxX - rail, frameTop, minZ, maxX, frameTop + rail, maxZ, 0xFFD1A64B, light);

		float cellWidth = scene.width() / scene.columns();
		float cellDepth = scene.depth() / scene.rows();
		for (int row = 0; row < scene.rows(); row++) for (int column = 0; column < scene.columns(); column++) {
			int color = scene.tileColor(column, row);
			float gapX = Math.min(0.003f, cellWidth * 0.02f);
			float gapZ = Math.min(0.003f, cellDepth * 0.02f);
			float x0 = minX + column * cellWidth + gapX + (column == 0 ? rail : 0f);
			float x1 = minX + (column + 1) * cellWidth - gapX - (column == scene.columns() - 1 ? rail : 0f);
			float z0 = minZ + row * cellDepth + gapZ + (row == 0 ? rail : 0f);
			float z1 = minZ + (row + 1) * cellDepth - gapZ - (row == scene.rows() - 1 ? rail : 0f);
			box(pose, out, x0, frameTop, z0, x1, tileTop, z1, color, light);
		}

		BoardScene.Movement move = scene.movement();
		boolean animating = move != null && progress < 1f;
		for (BoardScene.Token token : scene.tokens()) {
			if (animating && token.column() == move.toColumn() && token.row() == move.toRow()) continue;
			drawToken(pose, out, scene, token, token.column(), token.row(), tileTop, light);
		}
		if (animating) {
			float column = move.fromColumn() + (move.toColumn() - move.fromColumn()) * eased;
			float row = move.fromRow() + (move.toRow() - move.fromRow()) * eased;
			float lift = (float) Math.sin(progress * Math.PI) * move.lift();
			drawToken(pose, out, scene, move.token(), column, row, tileTop + lift, light);
		}
	}

	private void drawToken(PoseStack.Pose pose, VertexConsumer out, BoardScene scene, BoardScene.Token token,
			float column, float row, float baseY, int light) {
		float cellWidth = scene.width() / scene.columns();
		float cellDepth = scene.depth() / scene.rows();
		float centerX = -scene.width() / 2f + (column + 0.5f) * cellWidth;
		float centerZ = -scene.depth() / 2f + (row + 0.5f) * cellDepth;
		for (BoardScene.Cuboid part : token.model()) {
			box(pose, out,
					centerX + part.minX() * cellWidth, baseY + part.minY() * TOKEN_HEIGHT, centerZ + part.minZ() * cellDepth,
					centerX + part.maxX() * cellWidth, baseY + part.maxY() * TOKEN_HEIGHT, centerZ + part.maxZ() * cellDepth,
					part.color(), light);
		}
	}

	private void box(PoseStack.Pose pose, VertexConsumer out, float x0, float y0, float z0,
			float x1, float y1, float z1, int color, int light) {
		face(pose, out, x0,y1,z0, x0,y1,z1, x1,y1,z1, x1,y1,z0, 0,1,0, color, light);
		face(pose, out, x0,y0,z0, x1,y0,z0, x1,y0,z1, x0,y0,z1, 0,-1,0, shade(color,0.48f), light);
		face(pose, out, x0,y0,z0, x0,y1,z0, x1,y1,z0, x1,y0,z0, 0,0,-1, shade(color,0.68f), light);
		face(pose, out, x0,y0,z1, x1,y0,z1, x1,y1,z1, x0,y1,z1, 0,0,1, shade(color,0.82f), light);
		face(pose, out, x0,y0,z0, x0,y0,z1, x0,y1,z1, x0,y1,z0, -1,0,0, shade(color,0.60f), light);
		face(pose, out, x1,y0,z0, x1,y1,z0, x1,y1,z1, x1,y0,z1, 1,0,0, shade(color,0.88f), light);
	}

	private void face(PoseStack.Pose pose, VertexConsumer out,
			float ax,float ay,float az, float bx,float by,float bz, float cx,float cy,float cz, float dx,float dy,float dz,
			float nx,float ny,float nz, int color, int light) {
		vertex(pose,out,ax,ay,az,nx,ny,nz,color,light);
		vertex(pose,out,bx,by,bz,nx,ny,nz,color,light);
		vertex(pose,out,cx,cy,cz,nx,ny,nz,color,light);
		vertex(pose,out,dx,dy,dz,nx,ny,nz,color,light);
	}

	private void vertex(PoseStack.Pose pose, VertexConsumer out, float x,float y,float z,
			float nx,float ny,float nz, int color, int light) {
		out.addVertex(pose, x, y, z).setColor(color).setUv(0f,0f).setLight(light).setNormal(pose,nx,ny,nz);
	}

	private static int shade(int argb, float factor) {
		int r = Math.min(255, (int)(((argb >> 16) & 255) * factor));
		int g = Math.min(255, (int)(((argb >> 8) & 255) * factor));
		int b = Math.min(255, (int)((argb & 255) * factor));
		return (argb & 0xFF000000) | r << 16 | g << 8 | b;
	}
}
