package com.example.arcane.client;

import net.fabricmc.api.ClientModInitializer;
import com.example.arcane.network.MatchStatePayload;
import com.example.arcane.network.MatchClockPayload;
import com.example.arcane.network.OpenPracticePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import com.example.arcane.block.ModBlockEntities;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import com.example.arcane.client.board.BoardSceneRegistry;
import com.example.arcane.client.board.BoardSceneRenderer;
import com.example.arcane.client.board.ChessBoardSceneSource;

public class ArcaneClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		BoardSceneRegistry.register("arcane:chess", new ChessBoardSceneSource());
		BlockEntityRendererRegistry.register(ModBlockEntities.GAME_BOARD, BoardSceneRenderer::new);
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			boolean safeWelcomePoint = client.gui.screen() instanceof net.minecraft.client.gui.screens.TitleScreen
					|| (client.gui.screen() == null && client.player != null);
			if (!ChesscomPreferences.hasCompletedWelcome() && safeWelcomePoint) {
				SystemToast.add(client.gui.toastManager(), new SystemToast.SystemToastId(),
						Component.literal("Arcane Games"), Component.literal("Choose Chess.com or play as a guest"));
				client.gui.setScreen(new ChesscomWelcomeScreen(client.gui.screen()));
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(MatchStatePayload.TYPE, (payload, context) -> context.client().execute(() -> {
			if (context.client().gui.screen() instanceof ChessScreen screen && payload.matchId().equals(screen.matchId())) {
				screen.apply(payload);
			} else if (payload.open()) {
				context.client().gui.setScreen(new ChessScreen(payload));
			}
		}));
		ClientPlayNetworking.registerGlobalReceiver(MatchClockPayload.TYPE, (payload, context) -> context.client().execute(() -> {
			if (context.client().gui.screen() instanceof ChessScreen screen) screen.applyClock(payload);
		}));
		ClientPlayNetworking.registerGlobalReceiver(OpenPracticePayload.TYPE, (payload, context) -> context.client().execute(() ->
				context.client().gui.setScreen(new ChessPracticeScreen(context.client().gui.screen()))));
	}
}
