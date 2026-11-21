package com.example.arcane.block;

import com.example.arcane.Arcane;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;

public final class ModBlockEntities {
	public static final BlockEntityType<GameBoardBlockEntity> GAME_BOARD = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			Arcane.id("chessboard"),
			FabricBlockEntityTypeBuilder.create(GameBoardBlockEntity::new, ModBlocks.ARCANE_TOME).build()
	);

	private ModBlockEntities() {}
	public static void initialize() {}
}
