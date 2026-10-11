package com.bettergamerules.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.neoforged.fml.loading.FMLPaths;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;

public final class PresetManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PRESET_DIR = FMLPaths.CONFIGDIR.get().resolve("bettergamerules").resolve("presets");

	public static List<String> listPresets() {
		List<String> names = new ArrayList<>();
		try {
			if (Files.isDirectory(PRESET_DIR)) {
				try (var stream = Files.list(PRESET_DIR)) {
					stream.filter(p -> p.toString().endsWith(".json")).forEach(p -> names.add(p.getFileName().toString().replaceFirst("\\.json$", "")));
				}
			}
		} catch (Exception ignored) {
		}
		return names;
	}

	public static void save(String name, GameRules gameRules) throws Exception {
		JsonObject obj = new JsonObject();
		gameRules.availableRules().forEach(rule -> obj.addProperty(rule.id(), serialize(gameRules, rule)));
		Files.createDirectories(PRESET_DIR);
		Files.writeString(PRESET_DIR.resolve(name + ".json"), GSON.toJson(obj));
	}

	public static boolean load(String name, GameRules gameRules) throws Exception {
		Path path = PRESET_DIR.resolve(name + ".json");
		if (!Files.exists(path)) {
			return false;
		}
		JsonObject obj = GSON.fromJson(Files.readString(path), JsonObject.class);
		gameRules.availableRules().forEach(rule -> {
			if (obj.has(rule.id())) {
				String value = obj.get(rule.id()).getAsString();
				apply(gameRules, rule, value);
			}
		});
		return true;
	}

	public static void delete(String name) throws Exception {
		Files.deleteIfExists(PRESET_DIR.resolve(name + ".json"));
	}

	public static void resetAll(GameRules gameRules) {
		gameRules.availableRules().forEach(rule -> resetOne(gameRules, rule));
	}

	private static <T> void resetOne(GameRules gameRules, GameRule<T> rule) {
		gameRules.set(rule, rule.defaultValue(), null);
	}

	@SuppressWarnings("unchecked")
	private static <T> String serialize(GameRules gameRules, GameRule<T> rule) {
		return rule.serialize((T) gameRules.get(rule));
	}

	@SuppressWarnings("unchecked")
	private static <T> void apply(GameRules gameRules, GameRule<T> rule, String value) {
		rule.deserialize(value).ifSuccess(v -> gameRules.set(rule, v, null));
	}
}
