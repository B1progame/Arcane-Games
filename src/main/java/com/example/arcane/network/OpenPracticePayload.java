package com.example.arcane.network;

import com.example.arcane.Arcane;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Opens a local-only practice setup after the integrated server validates the command. */
public record OpenPracticePayload() implements CustomPacketPayload {
	public static final Type<OpenPracticePayload> TYPE = new Type<>(Arcane.id("open_chess_practice"));
	public static final StreamCodec<RegistryFriendlyByteBuf, OpenPracticePayload> CODEC = StreamCodec.unit(new OpenPracticePayload());
	@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
