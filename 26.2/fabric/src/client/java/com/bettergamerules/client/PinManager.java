package com.bettergamerules.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;

public final class PinManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PIN_FILE = FabricLoader.getInstance().getConfigDir().resolve("bettergamerules").resolve("pinned.json");
	private static final Set<String> PINNED = load();

	private static Set<String> load() {
		try {
			if (Files.exists(PIN_FILE)) {
				Set<String> read = GSON.fromJson(Files.readString(PIN_FILE), new TypeToken<LinkedHashSet<String>>() {
				}.getType());
				if (read != null) {
					return new LinkedHashSet<>(read);
				}
			}
		} catch (Exception ignored) {
		}
		return new LinkedHashSet<>();
	}

	private static void save() {
		try {
			Files.createDirectories(PIN_FILE.getParent());
			Files.writeString(PIN_FILE, GSON.toJson(PINNED));
		} catch (Exception ignored) {
		}
	}

	public static boolean isPinned(String ruleId) {
		return PINNED.contains(ruleId);
	}

	public static void toggle(String ruleId) {
		if (!PINNED.add(ruleId)) {
			PINNED.remove(ruleId);
		}
		save();
	}

	public static boolean hasPinned() {
		return !PINNED.isEmpty();
	}
}
