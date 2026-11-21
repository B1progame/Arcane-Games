package com.example.arcane.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Read-only Chess.com public profile lookup. This does not authenticate or prove account ownership. */
final class ChesscomProfileApi {
	private static final int MAX_RESPONSE_BYTES = 64 * 1024;
	private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(task -> {
		Thread thread = new Thread(task, "Arcane-Chesscom-PubAPI");
		thread.setDaemon(true);
		return thread;
	});
	private static final HttpClient HTTP = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(5))
			.followRedirects(HttpClient.Redirect.NORMAL)
			.build();

	private ChesscomProfileApi() {}

	static boolean isValidUsername(String username) {
		return username != null && username.length() >= 3 && username.length() <= 25
				&& username.matches("[A-Za-z0-9_-]+");
	}

	static CompletableFuture<Profile> lookup(String input) {
		String username = input == null ? "" : input.trim();
		if (!isValidUsername(username)) return CompletableFuture.failedFuture(new IllegalArgumentException("Username must be 3–25 letters, digits, _ or -."));
		return CompletableFuture.supplyAsync(() -> requestProfile(username), WORKER);
	}

	private static Profile requestProfile(String username) {
		String pathName = URLEncoder.encode(username, StandardCharsets.UTF_8).replace("+", "%20");
		URI endpoint = URI.create("https://api.chess.com/pub/player/" + pathName);
		HttpRequest request = HttpRequest.newBuilder(endpoint)
				.timeout(Duration.ofSeconds(8))
				.header("Accept", "application/json")
				.header("User-Agent", "ArcaneGames/1.0 (Minecraft client; public profile lookup)")
				.GET().build();
		try {
			HttpResponse<byte[]> response = HTTP.send(request, HttpResponse.BodyHandlers.ofByteArray());
			if (response.statusCode() == 404) throw new ProfileLookupException("No public profile found for that username.");
			if (response.statusCode() == 429) throw new ProfileLookupException("Chess.com is rate limiting. Try again later.");
			if (response.statusCode() < 200 || response.statusCode() >= 300) throw new ProfileLookupException("Profile lookup failed (HTTP " + response.statusCode() + ").");
			byte[] bytes = response.body();
			if (bytes.length > MAX_RESPONSE_BYTES) throw new ProfileLookupException("Chess.com profile response was too large.");
			JsonObject json = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
			String confirmedName = json.has("username") && json.get("username").isJsonPrimitive() ? json.get("username").getAsString() : "";
			if (!confirmedName.equalsIgnoreCase(username)) throw new ProfileLookupException("Chess.com returned a different profile than requested.");
			String displayName = json.has("name") && json.get("name").isJsonPrimitive() ? json.get("name").getAsString() : confirmedName;
			displayName = displayName.replaceAll("[\\p{Cntrl}]", "").strip();
			if (displayName.length() > 64) displayName = displayName.substring(0, 64);
			String profileUrl = "https://www.chess.com/member/" + pathName;
			return new Profile(confirmedName, displayName, profileUrl);
		} catch (ProfileLookupException failure) {
			throw failure;
		} catch (IOException failure) {
			throw new ProfileLookupException("Could not reach Chess.com; check your connection.", failure);
		} catch (InterruptedException failure) {
			Thread.currentThread().interrupt();
			throw new ProfileLookupException("Chess.com lookup was interrupted.", failure);
		} catch (RuntimeException failure) {
			throw new ProfileLookupException("Chess.com sent an unreadable profile.", failure);
		}
	}

	record Profile(String username, String displayName, String profileUrl) {}

	static final class ProfileLookupException extends RuntimeException {
		ProfileLookupException(String message) { super(message); }
		ProfileLookupException(String message, Throwable cause) { super(message, cause); }
	}
}
