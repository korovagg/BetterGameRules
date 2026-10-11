package com.bettergamerules.client;

import net.minecraft.world.level.GameRules;

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

	public boolean matches(GameRules rules, GameRules.Key<?> key, GameRules.Type<?> type) {
		return switch (this) {
			case ALL -> true;
			case BOOLEAN -> rules.getRule(key) instanceof GameRules.BooleanValue;
			case INTEGER -> rules.getRule(key) instanceof GameRules.IntegerValue;
			case MODIFIED -> GameRuleUI.isModified(rules, key, type);
		};
	}
}
