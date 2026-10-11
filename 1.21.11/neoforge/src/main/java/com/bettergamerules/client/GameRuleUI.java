package com.bettergamerules.client;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;

public final class GameRuleUI {
	private GameRuleUI() {
	}

	public static String readableName(GameRule<?> rule) {
		return I18n.get(rule.getDescriptionId());
	}

	public static Optional<String> description(GameRule<?> rule) {
		String key = rule.getDescriptionId() + ".description";
		return I18n.exists(key) ? Optional.of(I18n.get(key)) : Optional.empty();
	}

	public static boolean matchesSearch(GameRule<?> rule, String lowerCaseFilter) {
		if (lowerCaseFilter.isEmpty()) {
			return true;
		}
		if (containsLower(rule.id(), lowerCaseFilter)) {
			return true;
		}
		if (containsLower(readableName(rule), lowerCaseFilter)) {
			return true;
		}
		if (containsLower(rule.category().label().getString(), lowerCaseFilter)) {
			return true;
		}
		return description(rule).map(d -> containsLower(d, lowerCaseFilter)).orElse(false);
	}

	private static boolean containsLower(String text, String lowerCaseFilter) {
		return text.toLowerCase(Locale.ROOT).contains(lowerCaseFilter);
	}

	public static String serializeCurrent(GameRules gameRules, GameRule<?> rule) {
		return serializeValue(gameRules, rule);
	}

	public static String serializeDefault(GameRule<?> rule) {
		return defaultToString(rule);
	}

	public static boolean isModified(GameRules gameRules, GameRule<?> rule) {
		return !serializeValue(gameRules, rule).equals(defaultToString(rule));
	}

	@SuppressWarnings("unchecked")
	private static <T> String serializeValue(GameRules gameRules, GameRule<T> rule) {
		return rule.serialize((T) gameRules.get(rule));
	}

	@SuppressWarnings("unchecked")
	private static <T> String defaultToString(GameRule<T> rule) {
		return rule.serialize((T) rule.defaultValue());
	}

	public static List<FormattedCharSequence> buildTooltip(GameRule<?> rule, Font font) {
		Component id = Component.literal(rule.id()).withStyle(ChatFormatting.YELLOW);
		Optional<String> desc = description(rule);
		if (desc.isPresent()) {
			ImmutableList.Builder<FormattedCharSequence> result = ImmutableList.builder();
			result.add(id.getVisualOrderText());
			font.split(Component.literal(desc.get()), 150).forEach(result::add);
			return result.build();
		}
		return ImmutableList.of(id.getVisualOrderText());
	}

	public static String narrationSuffix(GameRule<?> rule) {
		Optional<String> desc = description(rule);
		String def = I18n.get("editGamerule.default", serializeDefault(rule));
		return desc.map(d -> d + "\n" + def).orElse(def);
	}

	public static <T> void resetToDefault(GameRules gameRules, GameRule<T> rule) {
		gameRules.set(rule, rule.defaultValue(), null);
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
