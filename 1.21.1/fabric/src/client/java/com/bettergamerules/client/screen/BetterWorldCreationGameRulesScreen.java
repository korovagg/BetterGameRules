package com.bettergamerules.client.screen;

import com.bettergamerules.client.GameRuleUI;
import com.bettergamerules.client.mixin.EditGameRulesScreenAccessor;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.worldselection.EditGameRulesScreen;
import net.minecraft.world.level.GameRules;
import org.jetbrains.annotations.Nullable;

public class BetterWorldCreationGameRulesScreen extends EditGameRulesScreen implements ScreenChrome.Host {
	private final ScreenChrome chrome = new ScreenChrome(this);
	private final Map<GameRules.Key<?>, GameRules.Type<?>> types = GameRuleUI.collectRules();
	private @Nullable EditBox searchField;
	private @Nullable BetterRuleList ruleListWidget;

	public BetterWorldCreationGameRulesScreen(GameRules gameRules, Consumer<Optional<GameRules>> exitCallback) {
		super(gameRules, exitCallback);
	}

	private EditGameRulesScreenAccessor accessor() {
		return (EditGameRulesScreenAccessor) this;
	}

	@Override
	protected void init() {
		this.chrome.init(c -> c.attachRuleList(this.types));
	}

	@Override
	public void onClose() {
		this.accessor().bettergamerules$getExitCallback().accept(Optional.empty());
	}

	@Override
	protected void repositionElements() {
		this.accessor().bettergamerules$getLayout().arrangeElements();
		if (this.ruleListWidget != null) {
			this.ruleListWidget.updateSize(this.width, this.accessor().bettergamerules$getLayout());
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
		return this.accessor().bettergamerules$getGameRules();
	}

	@Override
	public HeaderAndFooterLayout layout() {
		return this.accessor().bettergamerules$getLayout();
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
		this.accessor().bettergamerules$getExitCallback().accept(Optional.of(this.accessor().bettergamerules$getGameRules()));
	}

	@Override
	public void onCancelPressed() {
		this.accessor().bettergamerules$getExitCallback().accept(Optional.empty());
	}

	@Override
	public void openPresets() {
		this.minecraft.setScreen(new PresetListScreen(this, this.accessor().bettergamerules$getGameRules(), this.types, () -> {
			if (this.ruleListWidget != null) {
				this.ruleListWidget.refreshValues();
			}
		}));
	}
}
