package com.example.arcane.network;

import com.example.arcane.Arcane;
import com.example.arcane.chess.ChessMatch;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record MatchStatePayload(UUID matchId, int ply, String fen, ChessMatch.Status status, UUID whitePlayer,
		UUID blackPlayer, UUID drawOfferer, int whiteElo, int blackElo, boolean clockEnabled, long whiteClockMillis, long blackClockMillis,
		boolean yourTurn, boolean drawOfferToYou, boolean spectator, boolean open) implements CustomPacketPayload {
	public static final Type<MatchStatePayload> TYPE = new Type<>(Arcane.id("chess_match_state"));
	public static final StreamCodec<RegistryFriendlyByteBuf, MatchStatePayload> CODEC = StreamCodec.of(
			(buffer, payload) -> {
				buffer.writeUUID(payload.matchId());
				buffer.writeVarInt(payload.ply());
				buffer.writeUtf(payload.fen(), 100);
				buffer.writeByte(payload.status().ordinal());
				buffer.writeUUID(payload.whitePlayer());
				writeOptionalUuid(buffer, payload.blackPlayer());
				writeOptionalUuid(buffer, payload.drawOfferer());
				buffer.writeVarInt(payload.whiteElo());
				buffer.writeVarInt(payload.blackElo());
				buffer.writeBoolean(payload.clockEnabled());
				buffer.writeVarLong(payload.whiteClockMillis());
				buffer.writeVarLong(payload.blackClockMillis());
				buffer.writeBoolean(payload.yourTurn());
				buffer.writeBoolean(payload.drawOfferToYou());
				buffer.writeBoolean(payload.spectator());
				buffer.writeBoolean(payload.open());
			}, buffer -> {
				UUID id = buffer.readUUID();
				int ply = buffer.readVarInt();
				String fen = buffer.readUtf(100);
				int statusIndex = buffer.readUnsignedByte();
				if (statusIndex >= ChessMatch.Status.values().length) throw new IllegalArgumentException("Unknown match status");
				UUID white = buffer.readUUID();
				UUID black = readOptionalUuid(buffer);
				UUID drawOfferer = readOptionalUuid(buffer);
				int whiteElo = buffer.readVarInt();
				int blackElo = buffer.readVarInt();
				boolean clockEnabled = buffer.readBoolean();
				long whiteClockMillis = buffer.readVarLong();
				long blackClockMillis = buffer.readVarLong();
				return new MatchStatePayload(id, ply, fen, ChessMatch.Status.values()[statusIndex], white, black, drawOfferer, whiteElo, blackElo,
						clockEnabled, whiteClockMillis, blackClockMillis,
						buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean());
			}
	);

	private static void writeOptionalUuid(RegistryFriendlyByteBuf buffer, UUID id) {
		buffer.writeBoolean(id != null);
		if (id != null) buffer.writeUUID(id);
	}
	private static UUID readOptionalUuid(RegistryFriendlyByteBuf buffer) { return buffer.readBoolean() ? buffer.readUUID() : null; }
	public MatchStatePayload {
		if (matchId == null || ply < 0 || ply > 10000 || fen == null || fen.length() > 100 || status == null || whitePlayer == null
				|| whiteElo < 0 || whiteElo > 1_000_000 || blackElo < 0 || blackElo > 1_000_000
				|| whiteClockMillis < 0 || whiteClockMillis > 86_400_000L || blackClockMillis < 0 || blackClockMillis > 86_400_000L
				|| !clockEnabled && (whiteClockMillis != 0 || blackClockMillis != 0)) throw new IllegalArgumentException("Invalid match state packet");
	}
	@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
	public static MatchStatePayload from(com.example.arcane.chess.ChessMatch match, UUID player, boolean open, int whiteElo, int blackElo) {
		UUID drawOfferer = match.drawOfferer();
		return new MatchStatePayload(match.id(), match.ply(), match.fen(), match.status(), match.whitePlayer(), match.blackPlayer(), drawOfferer, whiteElo, blackElo,
				match.hasClock(), match.hasClock() ? match.clockRemainingMillis(com.example.arcane.chess.Side.WHITE, System.nanoTime() / 1_000_000L) : 0,
				match.hasClock() ? match.clockRemainingMillis(com.example.arcane.chess.Side.BLACK, System.nanoTime() / 1_000_000L) : 0,
				match.canMove(player), drawOfferer != null && !drawOfferer.equals(player), !match.isParticipant(player), open);
	}
}
