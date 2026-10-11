package com.bettergamerules.client;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.GameRules;

public final class GameRuleUI {
	private GameRuleUI() {
	}

	public static Map<GameRules.Key<?>, GameRules.Type<?>> collectRules() {
		Map<GameRules.Key<?>, GameRules.Type<?>> result = Maps.newLinkedHashMap();
		GameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
			@Override
			public void visitBoolean(GameRules.Key<GameRules.BooleanValue> key, GameRules.Type<GameRules.BooleanValue> type) {
				result.put(key, type);
			}

			@Override
			public void visitInteger(GameRules.Key<GameRules.IntegerValue> key, GameRules.Type<GameRules.IntegerValue> type) {
				result.put(key, type);
			}
		});
		return result;
	}

	public static String readableName(GameRules.Key<?> key) {
		return I18n.get(key.getDescriptionId());
	}

	public static String categoryName(GameRules.Category category) {
		return I18n.get(category.getDescriptionId());
	}

	public static Optional<String> description(GameRules.Key<?> key) {
		String descKey = key.getDescriptionId() + ".description";
		return I18n.exists(descKey) ? Optional.of(I18n.get(descKey)) : Optional.empty();
	}

	public static boolean matchesSearch(GameRules.Key<?> key, String categoryName, String lowerCaseFilter) {
		if (lowerCaseFilter.isEmpty()) {
			return true;
		}
		if (containsLower(key.getId(), lowerCaseFilter)) {
			return true;
		}
		if (containsLower(readableName(key), lowerCaseFilter)) {
			return true;
		}
		if (containsLower(categoryName, lowerCaseFilter)) {
			return true;
		}
		return description(key).map(d -> containsLower(d, lowerCaseFilter)).orElse(false);
	}

	private static boolean containsLower(String text, String lowerCaseFilter) {
		return text.toLowerCase(Locale.ROOT).contains(lowerCaseFilter);
	}

	public static String serializeCurrent(GameRules rules, GameRules.Key<?> key) {
		return rules.getRule(key).serialize();
	}

	public static String serializeDefault(GameRules.Type<?> type) {
		return defaultString(type);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static String defaultString(GameRules.Type<?> type) {
		return ((GameRules.Type) type).createRule().serialize();
	}

	public static boolean isModified(GameRules rules, GameRules.Key<?> key, GameRules.Type<?> type) {
		return !serializeCurrent(rules, key).equals(defaultString(type));
	}

	public static List<FormattedCharSequence> buildTooltip(GameRules.Key<?> key, Font font) {
		Component id = Component.literal(key.getId()).withStyle(ChatFormatting.YELLOW);
		Optional<String> desc = description(key);
		if (desc.isPresent()) {
			ImmutableList.Builder<FormattedCharSequence> result = ImmutableList.builder();
			result.add(id.getVisualOrderText());
			font.split(Component.literal(desc.get()), 150).forEach(result::add);
			return result.build();
		}
		return ImmutableList.of(id.getVisualOrderText());
	}

	public static String narrationSuffix(GameRules.Key<?> key, GameRules.Type<?> type) {
		Optional<String> desc = description(key);
		String def = I18n.get("editGamerule.default", serializeDefault(type));
		return desc.map(d -> d + "\n" + def).orElse(def);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public static void resetToDefault(GameRules rules, GameRules.Key<?> key, GameRules.Type<?> type) {
		String def = defaultString(type);
		GameRules.Value<?> current = rules.getRule(key);
		if (current instanceof GameRules.BooleanValue bool) {
			bool.set(Boolean.parseBoolean(def), null);
		} else if (current instanceof GameRules.IntegerValue integer) {
			integer.tryDeserialize(def);
		}
	}

	public static String[] splitLabelLines(Font font, String name, String secondLine, int maxWidth) {
		String first = name;
		if (font.width(first) > maxWidth) {
			String ellipsis = "...";
			first = font.plainSubstrByWidth(first, maxWidth - font.width(ellipsis)) + ellipsis;
		}
		String second = secondLine;
		if (font.width(second) > maxWidth) {
			String ellipsis = "...";
			second = font.plainSubstrByWidth(second, maxWidth - font.width(ellipsis)) + ellipsis;
		}
		return new String[]{first, second};
	}
}
