package com.bettergamerules.client.screen;

import com.bettergamerules.client.mixin.InWorldGameRulesScreenAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.LoadingDotsWidget;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.InWorldGameRulesScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundSetGameRulePacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleMap;
import net.minecraft.world.level.gamerules.GameRules;

public class BetterInWorldGameRulesScreen extends InWorldGameRulesScreen implements ScreenChrome.Host {
	private static final Component PENDING_TEXT = Component.translatable("editGamerule.inGame.downloadingGamerules");

	private final ScreenChrome chrome = new ScreenChrome(this);
	private final GameRuleMap initialValues = GameRuleMap.of();
	private final List<GameRule<?>> serverRules = new ArrayList<>();
	private EditBox searchField;
	private BetterRuleList ruleListWidget;
	private LoadingDotsWidget loadingDotsWidget;
	private boolean receivedServerValues = false;

	public BetterInWorldGameRulesScreen(ClientPacketListener connection, Consumer<Optional<GameRules>> exitCallback, Screen lastScreen) {
		super(connection, exitCallback, lastScreen);
	}

	@Override
	protected void init() {
		this.chrome.init(c -> {
			this.loadingDotsWidget = new LoadingDotsWidget(this.font, PENDING_TEXT);
			this.layout.addToContents(this.loadingDotsWidget);
			this.connection().send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.REQUEST_GAMERULE_VALUES));
		});
	}

	@Override
	protected void initContent() {
	}

	@Override
	protected void onDone() {
		List<ServerboundSetGameRulePacket.Entry> changed = new ArrayList<>();
		this.initialValues.keySet().forEach(rule -> this.collectChanged(rule, changed));
		if (!changed.isEmpty()) {
			this.connection().send(new ServerboundSetGameRulePacket(changed));
		}
		this.closeAndApplyChanges();
	}

	private <T> void collectChanged(GameRule<T> rule, List<ServerboundSetGameRulePacket.Entry> entries) {
		if (!this.gameRules.get(rule).equals(this.initialValues.get(rule))) {
			T current = this.gameRules.get(rule);
			BuiltInRegistries.GAME_RULE.getResourceKey(rule).ifPresent(key -> entries.add(new ServerboundSetGameRulePacket.Entry(key, rule.serialize(current))));
		}
	}

	@Override
	public void onClose() {
		if (this.hasPendingChanges()) {
			this.minecraft.gui.setScreen(new ConfirmScreen(confirmed -> {
				if (confirmed) {
					this.closeAndDiscardChanges();
				} else {
					this.minecraft.gui.setScreen(this);
				}
			}, Component.translatable("editGamerule.inGame.discardChanges.title"),
				Component.translatable("editGamerule.inGame.discardChanges.message")));
		} else {
			this.closeAndDiscardChanges();
		}
	}

	private boolean hasPendingChanges() {
		return this.initialValues.keySet().stream().anyMatch(this::ruleChanged);
	}

	private <T> boolean ruleChanged(GameRule<T> rule) {
		return !this.gameRules.get(rule).equals(this.initialValues.get(rule));
	}

	@Override
	public void onGameRuleValuesUpdated(Map<ResourceKey<GameRule<?>>, String> values) {
		if (this.receivedServerValues) {
			return;
		}
		this.receivedServerValues = true;
		values.forEach((key, valueStr) -> {
			GameRule<?> rule = BuiltInRegistries.GAME_RULE.getValue(key);
			if (rule != null) {
				this.serverRules.add(rule);
				this.initializeValue(rule, valueStr);
			}
		});
		if (this.loadingDotsWidget != null) {
			this.removeWidget(this.loadingDotsWidget);
			this.loadingDotsWidget = null;
		}
		this.chrome.attachRuleListLate(this.serverRules);
	}

	private <T> void initializeValue(GameRule<T> rule, String valueStr) {
		rule.deserialize(valueStr).result().ifPresent(value -> {
			this.initialValues.set(rule, value);
			this.gameRules.set(rule, value, null);
		});
	}

	private ClientPacketListener connection() {
		return ((InWorldGameRulesScreenAccessor) this).bettergamerules$getConnection();
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
		this.onDone();
	}

	@Override
	public void onCancelPressed() {
		this.onClose();
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
