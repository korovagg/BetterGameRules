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
import net.minecraft.world.level.GameRules;

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

	public static void save(String name, GameRules rules, Map<GameRules.Key<?>, GameRules.Type<?>> types) throws Exception {
		JsonObject obj = new JsonObject();
		for (GameRules.Key<?> key : types.keySet()) {
			obj.addProperty(key.getId(), rules.getRule(key).serialize());
		}
		Files.createDirectories(PRESET_DIR);
		Files.writeString(PRESET_DIR.resolve(name + ".json"), GSON.toJson(obj));
	}

	public static boolean load(String name, GameRules rules, Map<GameRules.Key<?>, GameRules.Type<?>> types) throws Exception {
		Path path = PRESET_DIR.resolve(name + ".json");
		if (!Files.exists(path)) {
			return false;
		}
		JsonObject obj = GSON.fromJson(Files.readString(path), JsonObject.class);
		for (GameRules.Key<?> key : types.keySet()) {
			if (obj.has(key.getId())) {
				applyString(rules, key, obj.get(key.getId()).getAsString());
			}
		}
		return true;
	}

	public static void applyString(GameRules rules, GameRules.Key<?> key, String value) {
		GameRules.Value<?> current = rules.getRule(key);
		if (current instanceof GameRules.BooleanValue bool) {
			bool.set(Boolean.parseBoolean(value), null);
		} else if (current instanceof GameRules.IntegerValue integer) {
			integer.tryDeserialize(value);
		}
	}

	public static void delete(String name) throws Exception {
		Files.deleteIfExists(PRESET_DIR.resolve(name + ".json"));
	}

	public static void resetAll(GameRules rules, Map<GameRules.Key<?>, GameRules.Type<?>> types) {
		for (Map.Entry<GameRules.Key<?>, GameRules.Type<?>> entry : types.entrySet()) {
			GameRuleUI.resetToDefault(rules, entry.getKey(), entry.getValue());
		}
	}
}
