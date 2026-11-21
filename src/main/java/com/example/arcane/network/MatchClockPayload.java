package com.example.arcane.network;

import com.example.arcane.Arcane;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

/** Small timer-only update that cannot reset a client's selected board square. */
public record MatchClockPayload(UUID matchId, long whiteMillis, long blackMillis) implements CustomPacketPayload {
	public static final Type<MatchClockPayload> TYPE = new Type<>(Arcane.id("chess_match_clock"));
	public static final StreamCodec<RegistryFriendlyByteBuf, MatchClockPayload> CODEC = StreamCodec.of(
			(buffer, payload) -> {
				buffer.writeUUID(payload.matchId());
				buffer.writeVarLong(payload.whiteMillis());
				buffer.writeVarLong(payload.blackMillis());
			}, buffer -> new MatchClockPayload(buffer.readUUID(), buffer.readVarLong(), buffer.readVarLong())
	);

	public MatchClockPayload {
		if (matchId == null || whiteMillis < 0 || whiteMillis > 86_400_000L || blackMillis < 0 || blackMillis > 86_400_000L)
			throw new IllegalArgumentException("Invalid match clock packet");
	}

	@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
