package com.example.arcane.client;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Local onboarding preference. This deliberately stores no Chess.com credentials or tokens. */
final class ChesscomPreferences {
	private static final Logger LOGGER = LoggerFactory.getLogger("arcane/client");
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("arcane-games-client.properties");
	private static boolean loaded;
	private static boolean welcomeCompleted;
	private static boolean chesscomLinksEnabled;
	private static String chesscomProfileUsername = "";
	private static String chesscomProfileDisplayName = "";

	private ChesscomPreferences() {}

	static boolean hasCompletedWelcome() {
		load();
		return welcomeCompleted;
	}

	static void completeWelcome() {
		load();
		welcomeCompleted = true;
		save();
	}

	static boolean areChesscomLinksEnabled() {
		load();
		return chesscomLinksEnabled;
	}

	static void setChesscomLinksEnabled(boolean enabled) {
		load();
		chesscomLinksEnabled = enabled;
		save();
	}

	static String chesscomProfileUsername() {
		load();
		return chesscomProfileUsername;
	}

	static void setChesscomProfileUsername(String username) {
		load();
		chesscomProfileUsername = username == null ? "" : username;
		save();
	}

	static String chesscomProfileDisplayName() {
		load();
		return chesscomProfileDisplayName;
	}

	static void setChesscomProfileDisplayName(String displayName) {
		load();
		chesscomProfileDisplayName = displayName == null ? "" : displayName;
		save();
	}

	private static void save() {
		Properties properties = new Properties();
		properties.setProperty("welcome_completed", Boolean.toString(welcomeCompleted));
		properties.setProperty("chesscom_links_enabled", Boolean.toString(chesscomLinksEnabled));
		properties.setProperty("chesscom_profile_username", chesscomProfileUsername);
		properties.setProperty("chesscom_profile_display_name", chesscomProfileDisplayName);
		try {
			Files.createDirectories(FILE.getParent());
			try (Writer writer = Files.newBufferedWriter(FILE)) {
				properties.store(writer, "Arcane Games client preferences; no account credentials are stored");
			}
		} catch (IOException error) {
			LOGGER.warn("Could not save Arcane Games first-run preference", error);
		}
	}

	private static void load() {
		if (loaded) return;
		loaded = true;
		if (!Files.isRegularFile(FILE)) return;
		Properties properties = new Properties();
		try (Reader reader = Files.newBufferedReader(FILE)) {
			properties.load(reader);
			welcomeCompleted = Boolean.parseBoolean(properties.getProperty("welcome_completed", "false"));
			chesscomLinksEnabled = Boolean.parseBoolean(properties.getProperty("chesscom_links_enabled", "false"));
			chesscomProfileUsername = properties.getProperty("chesscom_profile_username", "");
			if (!ChesscomProfileApi.isValidUsername(chesscomProfileUsername)) chesscomProfileUsername = "";
			chesscomProfileDisplayName = properties.getProperty("chesscom_profile_display_name", "").replaceAll("[\\p{Cntrl}]", "").strip();
			if (chesscomProfileDisplayName.length() > 64) chesscomProfileDisplayName = chesscomProfileDisplayName.substring(0, 64);
		} catch (IOException error) {
			LOGGER.warn("Could not read Arcane Games client preferences", error);
		}
	}
}
