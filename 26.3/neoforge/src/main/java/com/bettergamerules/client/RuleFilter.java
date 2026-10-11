package com.bettergamerules.client;

import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleType;
import net.minecraft.world.level.gamerules.GameRules;

public enum RuleFilter {
	ALL,
	BOOLEAN,
	INTEGER,
	MODIFIED;

	public String langKey() {
		return switch (this) {
			case ALL -> "bettergamerules.filter.all";
			case BOOLEAN -> "bettergamerules.filter.boolean";
			case INTEGER -> "bettergamerules.filter.integer";
			case MODIFIED -> "bettergamerules.filter.modified";
		};
	}

	public boolean matches(GameRules gameRules, GameRule<?> rule) {
		return switch (this) {
			case ALL -> true;
			case BOOLEAN -> rule.gameRuleType() == GameRuleType.BOOL;
			case INTEGER -> rule.gameRuleType() == GameRuleType.INT;
			case MODIFIED -> {
				String current = serialize(gameRules, rule);
				String defaultVal = defaultToString(rule);
				yield !current.equals(defaultVal);
			}
		};
	}

	@SuppressWarnings("unchecked")
	private static <T> String serialize(GameRules gameRules, GameRule<T> rule) {
		return rule.serialize((T) gameRules.get(rule));
	}

	@SuppressWarnings("unchecked")
	private static <T> String defaultToString(GameRule<T> rule) {
		return rule.serialize((T) rule.defaultValue());
	}
}
