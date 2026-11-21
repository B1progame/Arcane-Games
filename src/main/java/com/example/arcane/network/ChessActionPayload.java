package com.example.arcane.network;

import com.example.arcane.Arcane;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record ChessActionPayload(UUID matchId, int expectedPly, int action, String from, String to, int promotion) implements CustomPacketPayload {
	public static final int MOVE = 0, RESIGN = 1, OFFER_DRAW = 2, ACCEPT_DRAW = 3;
	public static final Type<ChessActionPayload> TYPE = new Type<>(Arcane.id("chess_action"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ChessActionPayload> CODEC = StreamCodec.of(
			(buffer, payload) -> {
				buffer.writeUUID(payload.matchId());
				buffer.writeVarInt(payload.expectedPly());
				buffer.writeByte(payload.action());
				buffer.writeUtf(payload.from(), 2);
				buffer.writeUtf(payload.to(), 2);
				buffer.writeByte(payload.promotion());
		}, buffer -> new ChessActionPayload(buffer.readUUID(), buffer.readVarInt(), buffer.readByte(), buffer.readUtf(2), buffer.readUtf(2), buffer.readByte())
	);

	public ChessActionPayload {
		if (matchId == null || expectedPly < 0 || expectedPly > 10000 || action < MOVE || action > ACCEPT_DRAW || promotion < 0 || promotion > 4) throw new IllegalArgumentException("Invalid chess action packet");
		if (from == null || to == null || from.length() > 2 || to.length() > 2) throw new IllegalArgumentException("Invalid chess move packet");
	}
	@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
