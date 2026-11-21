package com.example.arcane.block;

import com.example.arcane.Arcane;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class ModBlocks {
	private static final ResourceKey<CreativeModeTab> TOOLS_AND_UTILITIES = ResourceKey.create(
			Registries.CREATIVE_MODE_TAB, Arcane.id("tools_and_utilities"));
	private static final ResourceKey<Block> ARCANE_TOME_BLOCK_KEY = ResourceKey.create(Registries.BLOCK, Arcane.id("arcane_tome"));
	private static final ResourceKey<Item> ARCANE_TOME_ITEM_KEY = ResourceKey.create(Registries.ITEM, Arcane.id("arcane_tome"));
	public static final Block ARCANE_TOME = Registry.register(
			BuiltInRegistries.BLOCK,
			ARCANE_TOME_BLOCK_KEY,
			new ChessboardBlock(BlockBehaviour.Properties.of().setId(ARCANE_TOME_BLOCK_KEY).strength(2.5f).sound(SoundType.WOOD))
	);
	public static final Item ARCANE_TOME_ITEM = Registry.register(
			BuiltInRegistries.ITEM,
			ARCANE_TOME_ITEM_KEY,
			new BlockItem(ARCANE_TOME, new Item.Properties().useBlockDescriptionPrefix().setId(ARCANE_TOME_ITEM_KEY))
	);

	private ModBlocks() {}

	public static void initialize() {
		CreativeModeTabEvents.modifyOutputEvent(TOOLS_AND_UTILITIES).register(entries -> entries.accept(ARCANE_TOME_ITEM));
	}
}
