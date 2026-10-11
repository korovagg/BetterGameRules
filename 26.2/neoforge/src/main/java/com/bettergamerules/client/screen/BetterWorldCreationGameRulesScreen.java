package com.bettergamerules.client.screen;

import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.worldselection.WorldCreationGameRulesScreen;
import net.minecraft.world.level.gamerules.GameRules;

public class BetterWorldCreationGameRulesScreen extends WorldCreationGameRulesScreen implements ScreenChrome.Host {
	private final ScreenChrome chrome = new ScreenChrome(this);
	private EditBox searchField;
	private BetterRuleList ruleListWidget;

	public BetterWorldCreationGameRulesScreen(GameRules gameRules, Consumer<Optional<GameRules>> exitCallback) {
		super(gameRules, exitCallback);
	}

	@Override
	protected void init() {
		this.chrome.init(c -> c.attachRuleList(this.gameRules.availableRules().toList()));
	}

	@Override
	protected void initContent() {
	}

	@Override
	protected void onDone() {
		this.closeAndApplyChanges();
	}

	@Override
	public void onClose() {
		this.closeAndDiscardChanges();
	}

	@Override
	protected void repositionElements() {
		this.layout.arrangeElements();
		if (this.ruleListWidget != null) {
			this.ruleListWidget.updateSize(this.width, this.layout);
		}
	}

	@Override
	protected void setInitialFocus() {
		if (this.searchField != null) {
			this.setInitialFocus(this.searchField);
		}
	}

	@Override
	public Minecraft minecraft() {
		return this.minecraft;
	}

	@Override
	public Font font() {
		return this.font;
	}

	@Override
	public GameRules gameRules() {
		return this.gameRules;
	}

	@Override
	public HeaderAndFooterLayout layout() {
		return this.layout;
	}

	@Override
	public int screenWidth() {
		return this.width;
	}

	@Override
	public void addRenderable(AbstractWidget widget) {
		this.addRenderableWidget(widget);
	}

	@Override
	public void setSearchBox(EditBox box) {
		this.searchField = box;
	}

	@Override
	public EditBox getSearchBox() {
		return this.searchField;
	}

	@Override
	public void setRuleList(BetterRuleList list) {
		this.ruleListWidget = list;
	}

	@Override
	public BetterRuleList getRuleList() {
		return this.ruleListWidget;
	}

	@Override
	public void reposition() {
		this.repositionElements();
	}

	@Override
	public void onDonePressed() {
		this.closeAndApplyChanges();
	}

	@Override
	public void onCancelPressed() {
		this.closeAndDiscardChanges();
	}

	@Override
	public void openPresets() {
		this.minecraft.gui.setScreen(new PresetListScreen(this, this.gameRules, () -> {
			if (this.ruleListWidget != null) {
				this.ruleListWidget.refreshValues();
			}
		}));
	}
}
