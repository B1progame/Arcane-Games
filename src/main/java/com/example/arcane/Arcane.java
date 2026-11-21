package com.example.arcane;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;
import com.example.arcane.block.ModBlocks;
import com.example.arcane.block.ModBlockEntities;
import com.example.arcane.network.ChessNetworking;
import com.example.arcane.network.ChessCommands;
import com.example.arcane.chess.ChessMinigame;
import com.example.arcane.game.MinigameRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Arcane implements ModInitializer {
	public static final String MOD_ID = "arcane";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		MinigameRegistry.register(ChessMinigame.INSTANCE);
		ChessNetworking.initialize();
		ChessCommands.initialize();
		ModBlocks.initialize();
		ModBlockEntities.initialize();
		LOGGER.info("Arcane Games initialized");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
