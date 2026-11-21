package com.example.arcane.client;

import com.example.arcane.chess.ChessPgn;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Downloads the most recent published monthly PGN archive; it never accesses live games. */
final class ChesscomArchiveApi {
	private static final int MAX_RESPONSE_BYTES = 8_000_000;
	private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(task -> {
		Thread thread = new Thread(task, "Arcane-Chesscom-Archive");
		thread.setDaemon(true);
		return thread;
	});
	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NORMAL).build();

	private ChesscomArchiveApi() {}

	static CompletableFuture<Archive> recentGames(String input) {
		String username = input == null ? "" : input.trim();
		if (!ChesscomProfileApi.isValidUsername(username)) return CompletableFuture.failedFuture(new IllegalArgumentException("Enter a valid Chess.com username first."));
		return CompletableFuture.supplyAsync(() -> download(username), WORKER);
	}

	private static Archive download(String username) {
		String encoded = URLEncoder.encode(username, StandardCharsets.UTF_8).replace("+", "%20");
		try {
			Response index = send("https://api.chess.com/pub/player/" + encoded + "/games/archives", 128_000);
			if (index.statusCode() == 404) throw new ArchiveException("No public game archive found for that username.");
			if (index.statusCode() == 429) throw new ArchiveException("Chess.com is rate limiting archive requests. Try later.");
			if (index.statusCode() < 200 || index.statusCode() >= 300) throw new ArchiveException("Archive lookup failed (HTTP " + index.statusCode() + ").");
			JsonObject json = JsonParser.parseString(new String(index.body(), StandardCharsets.UTF_8)).getAsJsonObject();
			JsonArray archives = json.getAsJsonArray("archives");
			if (archives == null || archives.isEmpty()) throw new ArchiveException("This profile has no published monthly games.");
			String archiveUrl = archives.get(archives.size() - 1).getAsString();
			URI archiveUri = URI.create(archiveUrl);
			if (!"https".equalsIgnoreCase(archiveUri.getScheme()) || !"api.chess.com".equalsIgnoreCase(archiveUri.getHost())
					|| !archiveUri.getPath().matches("/pub/player/[A-Za-z0-9_-]+/games/20[0-9]{2}/(0[1-9]|1[0-2])")) {
				throw new ArchiveException("Chess.com returned an invalid archive link.");
			}
			Response pgn = send(archiveUrl + "/pgn", MAX_RESPONSE_BYTES);
			if (pgn.statusCode() == 404) throw new ArchiveException("The latest public game archive is unavailable.");
			if (pgn.statusCode() == 429) throw new ArchiveException("Chess.com is rate limiting archive requests. Try later.");
			if (pgn.statusCode() < 200 || pgn.statusCode() >= 300) throw new ArchiveException("Game archive download failed (HTTP " + pgn.statusCode() + ").");
			String text = new String(pgn.body(), StandardCharsets.UTF_8);
			List<ChessPgn.Replay> games = ChessPgn.parseArchive(text);
			if (games.isEmpty()) throw new ArchiveException("The public archive did not contain completed games.");
			String month = archiveUri.getPath().substring(archiveUri.getPath().lastIndexOf("/games/") + 7);
			return new Archive(username, month, games);
		} catch (ArchiveException failure) {
			throw failure;
		} catch (IOException failure) {
			throw new ArchiveException("Could not reach the Chess.com public archive.", failure);
		} catch (InterruptedException failure) {
			Thread.currentThread().interrupt();
			throw new ArchiveException("Archive download was interrupted.", failure);
		} catch (RuntimeException failure) {
			throw new ArchiveException("Chess.com returned an unreadable public archive.", failure);
		}
	}

	private static Response send(String address, int limit) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder(URI.create(address)).timeout(Duration.ofSeconds(12))
				.header("Accept", address.endsWith("/pgn") ? "application/x-chess-pgn, text/plain" : "application/json")
				.header("User-Agent", "ArcaneGames/1.0 (Minecraft client; public game archive import)").GET().build();
		HttpResponse<InputStream> response = HTTP.send(request, HttpResponse.BodyHandlers.ofInputStream());
		try (InputStream stream = response.body()) {
			byte[] bytes = stream.readNBytes(limit + 1);
			if (bytes.length > limit) throw new ArchiveException("Chess.com public archive response exceeded the size limit.");
			return new Response(response.statusCode(), bytes);
		}
	}

	private record Response(int statusCode, byte[] body) {}

	record Archive(String username, String month, List<ChessPgn.Replay> games) {
		Archive { games = List.copyOf(games); }
	}

	static final class ArchiveException extends RuntimeException {
		ArchiveException(String message) { super(message); }
		ArchiveException(String message, Throwable cause) { super(message, cause); }
	}
}
