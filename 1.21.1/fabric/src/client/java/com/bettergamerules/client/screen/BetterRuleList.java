package com.bettergamerules.client.screen;

import com.bettergamerules.client.GameRuleUI;
import com.bettergamerules.client.PinManager;
import com.bettergamerules.client.RuleFilter;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.GameRules;

public class BetterRuleList extends ContainerObjectSelectionList<BetterRuleList.Entry> {
	public static final int ROW_HEIGHT = 30;
	private static final int VALUE_WIDGET_WIDTH = 64;
	private static final int PIN_BUTTON_WIDTH = 18;
	private static final int RESET_BUTTON_WIDTH = 20;

	private final GameRules gameRules;
	private final Map<GameRules.Key<?>, GameRules.Type<?>> types;
	private final Listener listener;
	private String filter = "";
	public static volatile RuleFilter typeFilter = RuleFilter.ALL;

	public interface Listener {
		void markInvalid(Entry entry);

		void clearInvalid(Entry entry);
	}

	public BetterRuleList(Minecraft minecraft, int width, int contentHeight, int y0, GameRules gameRules,
			Map<GameRules.Key<?>, GameRules.Type<?>> types, Listener listener) {
		super(minecraft, width, contentHeight, y0, ROW_HEIGHT);
		this.gameRules = gameRules;
		this.types = types;
		this.listener = listener;
	}

	@Override
	public int getRowWidth() {
		return Math.max(220, Math.min(360, this.getWidth() - 16));
	}

	public void setFilter(String filter) {
		this.filter = filter;
		this.rebuild();
		this.setScrollAmount(0.0);
	}

	public void rebuildInitial() {
		this.rebuild();
	}

	public void refreshValues() {
		double scroll = this.getScrollAmount();
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

		Map<GameRules.Category, Map<GameRules.Key<?>, RuleEntry>> byCategory = new TreeMap<>();
		List<RuleEntry> pinned = new ArrayList<>();
		for (Map.Entry<GameRules.Key<?>, GameRules.Type<?>> rule : this.types.entrySet()) {
			GameRules.Key<?> key = rule.getKey();
			GameRules.Type<?> type = rule.getValue();
			if (!typeFilter.matches(this.gameRules, key, type)) {
				continue;
			}
			if (!GameRuleUI.matchesSearch(key, GameRuleUI.categoryName(key.getCategory()), lowerFilter)) {
				continue;
			}
			RuleEntry entry = RuleEntry.create(this, key, type);
			if (PinManager.isPinned(key.getId())) {
				pinned.add(entry);
			} else {
				byCategory.computeIfAbsent(key.getCategory(), k -> new TreeMap<>(Comparator.comparing(GameRules.Key::getId))).put(key, entry);
			}
		}
		pinned.sort(Comparator.comparing(e -> e.key.getId()));
		if (!pinned.isEmpty()) {
			this.addEntry(new SectionEntry(Component.translatable("bettergamerules.pinned", pinned.size()), true));
			for (RuleEntry entry : pinned) {
				this.addEntry(entry);
			}
		}
		for (Map.Entry<GameRules.Category, Map<GameRules.Key<?>, RuleEntry>> category : byCategory.entrySet()) {
			MutableComponent label = Component.translatable(category.getKey().getDescriptionId());
			label.append(" (" + category.getValue().size() + ")");
			this.addEntry(new SectionEntry(label, false));
			for (RuleEntry entry : category.getValue().values()) {
				this.addEntry(entry);
			}
		}
		if (this.children().isEmpty()) {
			this.addEntry(new SectionEntry(Component.translatable("bettergamerules.no_match").withStyle(ChatFormatting.GRAY), false));
		}
	}

	@Override
	public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.renderWidget(graphics, mouseX, mouseY, partialTick);
		Entry hovered = this.getHovered();
		if (hovered != null && hovered.tooltip != null && !hovered.isMouseOverWidget(mouseX, mouseY)) {
			Minecraft.getInstance().screen.setTooltipForNextRenderPass(hovered.tooltip);
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
		public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered,
				float partialTick) {
			Component styled = this.pinnedSection
				? this.label.copy().withStyle(ChatFormatting.BOLD, ChatFormatting.YELLOW)
				: this.label.copy().withStyle(ChatFormatting.BOLD, ChatFormatting.YELLOW, ChatFormatting.UNDERLINE);
			graphics.drawCenteredString(Minecraft.getInstance().font, styled, left + width / 2, top + ROW_HEIGHT / 2 - 4, 0xFFFFFFFF);
		}

		@Override
		public List<? extends NarratableEntry> narratables() {
			return List.of(new NarratableEntry() {
				@Override
				public NarratableEntry.NarrationPriority narrationPriority() {
					return NarratableEntry.NarrationPriority.HOVERED;
				}

				@Override
				public void updateNarration(NarrationElementOutput output) {
					output.add(NarratedElementType.TITLE, SectionEntry.this.label);
				}
			});
		}
	}

	public static class RuleEntry extends Entry {
		private final BetterRuleList list;
		private final GameRules.Key<?> key;
		private final GameRules.Type<?> type;
		private final List<AbstractWidget> widgets = Lists.newArrayList();
		private final Button pinButton;
		private final Button resetButton;
		private final AbstractWidget valueWidget;
		private String line1;
		private String line2;
		private boolean modified;

		private RuleEntry(BetterRuleList list, GameRules.Key<?> key, GameRules.Type<?> type, Button pinButton, Button resetButton,
				AbstractWidget valueWidget) {
			super(GameRuleUI.buildTooltip(key, Minecraft.getInstance().font));
			this.list = list;
			this.key = key;
			this.type = type;
			this.pinButton = pinButton;
			this.resetButton = resetButton;
			this.valueWidget = valueWidget;
			this.widgets.add(pinButton);
			this.widgets.add(resetButton);
			this.widgets.add(valueWidget);
			this.refreshState();
		}

		public static RuleEntry create(BetterRuleList list, GameRules.Key<?> key, GameRules.Type<?> type) {
			Minecraft minecraft = Minecraft.getInstance();
			GameRules gameRules = list.gameRules;
			Button pinButton = Button.builder(pinLabel(key), b -> {
				PinManager.toggle(key.getId());
				list.rebuildPreserveScroll();
			}).size(PIN_BUTTON_WIDTH, 20).build();
			Button resetButton = Button.builder(Component.literal("R"), b -> {
				Entry entry = RuleEntryRef.get(list, key);
				if (entry instanceof RuleEntry ruleEntry) {
					ruleEntry.resetToDefault();
				} else {
					GameRuleUI.resetToDefault(gameRules, key, type);
					list.refreshStates();
				}
			}).size(RESET_BUTTON_WIDTH, 20).build();
			resetButton.setTooltip(Tooltip.create(Component.translatable("bettergamerules.reset_rule")));
			AbstractWidget valueWidget;
			if (gameRules.getRule(key) instanceof GameRules.BooleanValue bool) {
				valueWidget = boolWidget(minecraft, bool, key, type, list);
			} else {
				valueWidget = intWidget(minecraft, (GameRules.IntegerValue) gameRules.getRule(key), key, type, list);
			}
			return new RuleEntry(list, key, type, pinButton, resetButton, valueWidget);
		}

		private static Component pinLabel(GameRules.Key<?> key) {
			return Component.literal(PinManager.isPinned(key.getId()) ? "★" : "☆").withStyle(ChatFormatting.GOLD);
		}

		private static AbstractWidget boolWidget(Minecraft minecraft, GameRules.BooleanValue value, GameRules.Key<?> key, GameRules.Type<?> type,
				BetterRuleList list) {
			Component name = Component.translatable(key.getDescriptionId());
			String narration = GameRuleUI.narrationSuffix(key, type);
			return CycleButton.onOffBuilder(value.get())
				.displayOnlyValue()
				.withCustomNarration(button -> button.createDefaultNarrationMessage().append(Component.literal("\n" + narration)))
				.create(0, 0, VALUE_WIDGET_WIDTH, 20, name, (button, newValue) -> {
					value.set(newValue, null);
					list.refreshStates();
				});
		}

		private static AbstractWidget intWidget(Minecraft minecraft, GameRules.IntegerValue value, GameRules.Key<?> key, GameRules.Type<?> type,
				BetterRuleList list) {
			Component label = Component.translatable(key.getDescriptionId());
			String narration = GameRuleUI.narrationSuffix(key, type);
			EditBox input = new EditBox(minecraft.font, 0, 0, VALUE_WIDGET_WIDTH, 20,
				label.copy().append(Component.literal("\n" + narration + "\n")));
			input.setValue(Integer.toString(value.get()));
			input.setResponder(text -> {
				if (value.tryDeserialize(text)) {
					input.setTextColor(0xFFE0E0E0);
					list.listener.clearInvalid(RuleEntryRef.get(list, key));
					list.refreshStates();
				} else {
					input.setTextColor(0xFFFF5555);
					list.listener.markInvalid(RuleEntryRef.get(list, key));
				}
			});
			return input;
		}

		public void resetToDefault() {
			GameRuleUI.resetToDefault(this.list.gameRules, this.key, this.type);
			this.syncValueWidget();
			this.list.listener.clearInvalid(this);
			this.list.refreshStates();
		}

		private void syncValueWidget() {
			GameRules.Value<?> current = this.list.gameRules.getRule(this.key);
			if (this.valueWidget instanceof CycleButton && current instanceof GameRules.BooleanValue bool) {
				((CycleButton<Boolean>) this.valueWidget).setValue(bool.get());
			} else if (this.valueWidget instanceof EditBox input && current instanceof GameRules.IntegerValue integer) {
				input.setValue(Integer.toString(integer.get()));
			}
		}

		public void refreshState() {
			this.modified = GameRuleUI.isModified(this.list.gameRules, this.key, this.type);
			this.resetButton.active = this.modified;
			this.pinButton.setMessage(pinLabel(this.key));
			this.tooltip = GameRuleUI.buildTooltip(this.key, Minecraft.getInstance().font);
			Font font = Minecraft.getInstance().font;
			String name = GameRuleUI.readableName(this.key);
			String second = this.key.getId();
			int labelWidth = Math.max(60, this.list.getRowWidth() - PIN_BUTTON_WIDTH - RESET_BUTTON_WIDTH - VALUE_WIDGET_WIDTH - 16);
			String[] lines = GameRuleUI.splitLabelLines(font, name, second, labelWidth);
			this.line1 = lines[0];
			this.line2 = lines[1];
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
		public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered,
				float partialTick) {
			Font font = Minecraft.getInstance().font;
			int rowRight = left + width;
			int centerY = top + (ROW_HEIGHT - 20) / 2;

			this.pinButton.setX(left);
			this.pinButton.setY(centerY);
			this.pinButton.render(graphics, mouseX, mouseY, partialTick);

			this.valueWidget.setX(rowRight - VALUE_WIDGET_WIDTH);
			this.valueWidget.setY(centerY);
			this.valueWidget.render(graphics, mouseX, mouseY, partialTick);

			this.resetButton.setX(rowRight - VALUE_WIDGET_WIDTH - 2 - RESET_BUTTON_WIDTH);
			this.resetButton.setY(centerY);
			this.resetButton.render(graphics, mouseX, mouseY, partialTick);

			int labelX = left + PIN_BUTTON_WIDTH + 4;
			int color = this.modified ? 0xFFFFFF55 : 0xFFFFFFFF;
			graphics.drawString(font, this.line1, labelX, top + 4, color, false);
			graphics.drawString(font, this.line2, labelX, top + 15, 0xFF808080, false);
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
		double scroll = this.getScrollAmount();
		this.rebuild();
		this.setScrollAmount(scroll);
	}

	private static final class RuleEntryRef {
		static Entry get(BetterRuleList list, GameRules.Key<?> key) {
			for (Entry entry : list.children()) {
				if (entry instanceof RuleEntry ruleEntry && ruleEntry.key.equals(key)) {
					return ruleEntry;
				}
			}
			return null;
		}
	}
}
