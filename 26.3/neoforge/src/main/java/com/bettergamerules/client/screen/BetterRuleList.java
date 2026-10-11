package com.bettergamerules.client.screen;

import com.bettergamerules.client.GameRuleUI;
import com.bettergamerules.client.PinManager;
import com.bettergamerules.client.RuleFilter;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.level.gamerules.GameRuleType;
import net.minecraft.world.level.gamerules.GameRules;

public class BetterRuleList extends ContainerObjectSelectionList<BetterRuleList.Entry> {
	public static final int RULE_ROW_HEIGHT = 30;
	public static final int HEADER_ROW_HEIGHT = 20;
	private static final int VALUE_WIDGET_WIDTH = 64;
	private static final int PIN_BUTTON_WIDTH = 18;
	private static final int RESET_BUTTON_WIDTH = 20;

	private final GameRules gameRules;
	private final Listener listener;
	private List<GameRule<?>> ruleSet = List.of();
	private String filter = "";
	public static volatile RuleFilter typeFilter = RuleFilter.ALL;

	public interface Listener {
		void markInvalid(Entry entry);

		void clearInvalid(Entry entry);
	}

	public BetterRuleList(Minecraft minecraft, int width, int contentHeight, int y0, GameRules gameRules, Listener listener) {
		super(minecraft, width, contentHeight, y0, RULE_ROW_HEIGHT);
		this.gameRules = gameRules;
		this.listener = listener;
	}

	@Override
	public int getRowWidth() {
		return Math.max(220, Math.min(360, this.getWidth() - 16));
	}

	public void setRules(List<GameRule<?>> rules) {
		this.ruleSet = new ArrayList<>(rules);
		this.rebuild();
	}

	public void setFilter(String filter) {
		this.filter = filter;
		this.rebuild();
		this.setScrollAmount(0.0);
	}

	public void refreshValues() {
		double scroll = this.scrollAmount();
		this.rebuild();
		this.setScrollAmount(scroll);
	}

	public void refreshStates() {
		for (Entry entry : this.children()) {
			if (entry instanceof RuleEntry ruleEntry) {
				ruleEntry.refreshState();
			}
		}
	}

	private void rebuild() {
		this.clearEntries();
		String lowerFilter = this.filter.toLowerCase(Locale.ROOT);
		RuleFilter typeFilter = BetterRuleList.typeFilter;

		Map<GameRuleCategory, Map<GameRule<?>, RuleEntry>> byCategory = new TreeMap<>(Comparator.comparing(c -> c.getDescriptionId().toString()));
		List<RuleEntry> pinned = new ArrayList<>();
		for (GameRule<?> rule : this.ruleSet) {
			if (!typeFilter.matches(this.gameRules, rule)) {
				continue;
			}
			if (!GameRuleUI.matchesSearch(rule, lowerFilter)) {
				continue;
			}
			RuleEntry entry = RuleEntry.create(this, rule);
			if (PinManager.isPinned(rule.id())) {
				pinned.add(entry);
			} else {
				byCategory.computeIfAbsent(rule.category(), k -> new TreeMap<>(Comparator.comparing(GameRule::getDescriptionId))).put(rule, entry);
			}
		}
		pinned.sort(Comparator.comparing(e -> e.rule.getDescriptionId()));
		if (!pinned.isEmpty()) {
			this.addEntry(new SectionEntry(Component.translatable("bettergamerules.pinned", pinned.size()), true), HEADER_ROW_HEIGHT);
			for (RuleEntry entry : pinned) {
				this.addEntry(entry, RULE_ROW_HEIGHT);
			}
		}
		for (Map.Entry<GameRuleCategory, Map<GameRule<?>, RuleEntry>> category : byCategory.entrySet()) {
			MutableComponent label = category.getKey().label();
			label.append(" (" + category.getValue().size() + ")");
			this.addEntry(new SectionEntry(label, false), HEADER_ROW_HEIGHT);
			for (RuleEntry entry : category.getValue().values()) {
				this.addEntry(entry, RULE_ROW_HEIGHT);
			}
		}
		if (this.children().isEmpty()) {
			this.addEntry(new SectionEntry(Component.translatable("bettergamerules.no_match").withStyle(ChatFormatting.GRAY), false),
				HEADER_ROW_HEIGHT);
		}
	}

	@Override
	public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractWidgetRenderState(graphics, mouseX, mouseY, partialTick);
		Entry hovered = this.getHovered();
		if (hovered != null && hovered.tooltip != null && !hovered.isMouseOverWidget(mouseX, mouseY)) {
			graphics.setTooltipForNextFrame(hovered.tooltip, mouseX, mouseY);
		}
	}

	public abstract static class Entry extends ContainerObjectSelectionList.Entry<Entry> {
		protected List<FormattedCharSequence> tooltip;

		protected Entry(List<FormattedCharSequence> tooltip) {
			this.tooltip = tooltip;
		}

		public boolean isMouseOverWidget(int mouseX, int mouseY) {
			return false;
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return List.of();
		}

		@Override
		public List<? extends NarratableEntry> narratables() {
			return List.of();
		}
	}

	public static class SectionEntry extends Entry {
		private final Component label;
		private final boolean pinnedSection;

		public SectionEntry(Component label, boolean pinnedSection) {
			super(null);
			this.label = label;
			this.pinnedSection = pinnedSection;
		}

		@Override
		public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
			Font font = Minecraft.getInstance().font;
			Component styled = this.pinnedSection
				? this.label.copy().withStyle(ChatFormatting.BOLD, ChatFormatting.YELLOW)
				: this.label.copy().withStyle(ChatFormatting.BOLD, ChatFormatting.YELLOW, ChatFormatting.UNDERLINE);
			int w = font.width(styled);
			graphics.text(font, styled, this.getContentXMiddle() - w / 2, this.getContentYMiddle() - 4, 0xFFFFFFFF);
		}
	}

	public static class RuleEntry extends Entry {
		private final BetterRuleList list;
		private final GameRule<?> rule;
		private final List<AbstractWidget> widgets = Lists.newArrayList();
		private final Button pinButton;
		private final Button resetButton;
		private final AbstractWidget valueWidget;
		private String line1;
		private String line2;
		private boolean modified;

		private RuleEntry(BetterRuleList list, GameRule<?> rule, Button pinButton, Button resetButton, AbstractWidget valueWidget) {
			super(GameRuleUI.buildTooltip(rule, Minecraft.getInstance().font));
			this.list = list;
			this.rule = rule;
			this.pinButton = pinButton;
			this.resetButton = resetButton;
			this.valueWidget = valueWidget;
			this.widgets.add(pinButton);
			this.widgets.add(resetButton);
			this.widgets.add(valueWidget);
			this.refreshState();
		}

		public static RuleEntry create(BetterRuleList list, GameRule<?> rule) {
			Minecraft minecraft = Minecraft.getInstance();
			GameRules gameRules = list.gameRules;
			Button pinButton = Button.builder(pinLabel(rule), b -> {
				PinManager.toggle(rule.id());
				list.rebuildPreserveScroll();
			}).size(PIN_BUTTON_WIDTH, 20).build();
			Button resetButton = Button.builder(Component.literal("R"), b -> {
				Entry entry = RuleEntryRef.get(list, rule);
				if (entry instanceof RuleEntry ruleEntry) {
					ruleEntry.resetToDefault();
				} else {
					GameRuleUI.resetToDefault(gameRules, rule);
					list.refreshStates();
				}
			}).size(RESET_BUTTON_WIDTH, 20).build();
			resetButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("bettergamerules.reset_rule")));
			AbstractWidget valueWidget;
			if (rule.gameRuleType() == GameRuleType.BOOL) {
				valueWidget = boolWidget(minecraft, gameRules, boolRule(rule), list);
			} else {
				valueWidget = intWidget(minecraft, gameRules, intRule(rule), list);
			}
			return new RuleEntry(list, rule, pinButton, resetButton, valueWidget);
		}

		@SuppressWarnings("unchecked")
		private static GameRule<Boolean> boolRule(GameRule<?> rule) {
			return (GameRule<Boolean>) rule;
		}

		@SuppressWarnings("unchecked")
		private static GameRule<Integer> intRule(GameRule<?> rule) {
			return (GameRule<Integer>) rule;
		}

		private static Component pinLabel(GameRule<?> rule) {
			return Component.literal(PinManager.isPinned(rule.id()) ? "★" : "☆").withStyle(ChatFormatting.GOLD);
		}

		private static AbstractWidget boolWidget(Minecraft minecraft, GameRules gameRules, GameRule<Boolean> rule, BetterRuleList list) {
			Component name = Component.translatable(rule.getDescriptionId());
			String narration = GameRuleUI.narrationSuffix(rule);
			return CycleButton.onOffBuilder(gameRules.get(rule))
				.displayOnlyValue()
				.withCustomNarration(button -> button.createDefaultNarrationMessage().append(Component.literal("\n" + narration)))
				.create(0, 0, VALUE_WIDGET_WIDTH, 20, name, (button, value) -> {
					gameRules.set(rule, value, null);
					list.refreshStates();
				});
		}

		private static AbstractWidget intWidget(Minecraft minecraft, GameRules gameRules, GameRule<Integer> rule, BetterRuleList list) {
			Component label = Component.translatable(rule.getDescriptionId());
			String narration = GameRuleUI.narrationSuffix(rule);
			EditBox input = new EditBox(minecraft.font, 0, 0, VALUE_WIDGET_WIDTH, 20,
				label.copy().append(Component.literal("\n" + narration + "\n")));
			input.setValue(GameRuleUI.serializeCurrent(gameRules, rule));
			input.setResponder(value -> {
				DataResult<Integer> parsed = rule.deserialize(value);
				if (parsed.isSuccess()) {
					input.setTextColor(0xFFE0E0E0);
					list.listener.clearInvalid(RuleEntryRef.get(list, rule));
					gameRules.set(rule, parsed.getOrThrow(), null);
					list.refreshStates();
				} else {
					input.setTextColor(0xFFFF5555);
					list.listener.markInvalid(RuleEntryRef.get(list, rule));
				}
			});
			return input;
		}

		public void resetToDefault() {
			GameRuleUI.resetToDefault(this.list.gameRules, this.rule);
			this.syncValueWidget();
			this.list.listener.clearInvalid(this);
			this.list.refreshStates();
		}

		@SuppressWarnings("unchecked")
		private void syncValueWidget() {
			if (this.valueWidget instanceof CycleButton && this.rule.gameRuleType() == GameRuleType.BOOL) {
				((CycleButton<Boolean>) this.valueWidget).setValue(this.list.gameRules.get((GameRule<Boolean>) this.rule));
			} else if (this.valueWidget instanceof EditBox input) {
				input.setValue(GameRuleUI.serializeCurrent(this.list.gameRules, this.rule));
			}
		}

		public void refreshState() {
			this.modified = GameRuleUI.isModified(this.list.gameRules, this.rule);
			this.resetButton.active = this.modified;
			this.pinButton.setMessage(pinLabel(this.rule));
			this.tooltip = GameRuleUI.buildTooltip(this.rule, Minecraft.getInstance().font);
			Font font = Minecraft.getInstance().font;
			String name = Component.translatable(this.rule.getDescriptionId()).getString();
			String second = this.rule.id();
			int labelWidth = Math.max(60, this.list.getRowWidth() - PIN_BUTTON_WIDTH - RESET_BUTTON_WIDTH - VALUE_WIDGET_WIDTH - 16);
			Pair<String, String> lines = GameRuleUI.splitLabelLines(font, name, second, labelWidth);
			this.line1 = lines.getFirst();
			this.line2 = lines.getSecond();
		}

		@Override
		public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
			Font font = Minecraft.getInstance().font;
			int rowY = this.getContentY();
			int rowRight = this.getContentRight();
			int centerY = rowY + (RULE_ROW_HEIGHT - 20) / 2;

			this.pinButton.setX(this.getContentX());
			this.pinButton.setY(centerY);
			this.pinButton.extractRenderState(graphics, mouseX, mouseY, partialTick);

			this.valueWidget.setX(rowRight - VALUE_WIDGET_WIDTH);
			this.valueWidget.setY(centerY);
			this.valueWidget.extractRenderState(graphics, mouseX, mouseY, partialTick);

			this.resetButton.setX(rowRight - VALUE_WIDGET_WIDTH - 2 - RESET_BUTTON_WIDTH);
			this.resetButton.setY(centerY);
			this.resetButton.extractRenderState(graphics, mouseX, mouseY, partialTick);

			int labelX = this.getContentX() + PIN_BUTTON_WIDTH + 4;
			int color = this.modified ? 0xFFFFFF55 : 0xFFFFFFFF;
			graphics.text(font, this.line1, labelX, rowY + 4, color);
			graphics.text(font, this.line2, labelX, rowY + 15, 0xFF808080);
		}

		@Override
		public boolean isMouseOverWidget(int mouseX, int mouseY) {
			for (AbstractWidget widget : this.widgets) {
				if (mouseX >= widget.getX()
					&& mouseX < widget.getX() + widget.getWidth()
					&& mouseY >= widget.getY()
					&& mouseY < widget.getY() + widget.getHeight()) {
					return true;
				}
			}
			return false;
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return this.widgets;
		}

		@Override
		public List<? extends NarratableEntry> narratables() {
			return this.widgets;
		}
	}

	private void rebuildPreserveScroll() {
		double scroll = this.scrollAmount();
		this.rebuild();
		this.setScrollAmount(scroll);
	}

	private static final class RuleEntryRef {
		static Entry get(BetterRuleList list, GameRule<?> rule) {
			for (Entry entry : list.children()) {
				if (entry instanceof RuleEntry ruleEntry && ruleEntry.rule == rule) {
					return ruleEntry;
				}
			}
			return null;
		}
	}
}
