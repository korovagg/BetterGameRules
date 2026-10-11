package com.bettergamerules.client.screen;

import com.bettergamerules.client.PresetManager;
import com.bettergamerules.client.RuleFilter;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;

public final class ScreenChrome implements BetterRuleList.Listener {
	private static final Component TITLE = Component.translatable("editGamerule.title");
	private static final Component SEARCH_HINT = Component.translatable("bettergamerules.search_hint").withStyle(EditBox.SEARCH_HINT_STYLE);
	private static final int HEADER_HEIGHT = 62;

	public interface Host {
		Minecraft minecraft();

		Font font();

		GameRules gameRules();

		HeaderAndFooterLayout layout();

		int screenWidth();

		void addRenderable(AbstractWidget widget);

		void setSearchBox(EditBox box);

		EditBox getSearchBox();

		void setRuleList(BetterRuleList list);

		BetterRuleList getRuleList();

		void reposition();

		void onDonePressed();

		void onCancelPressed();

		void openPresets();
	}

	private final Host host;
	private final Set<BetterRuleList.Entry> invalidEntries = Sets.newHashSet();
	private Button doneButton;

	public ScreenChrome(Host host) {
		this.host = host;
	}

	public void init(Consumer<ScreenChrome> contentBuilder) {
		this.host.layout().setHeaderHeight(HEADER_HEIGHT);
		LinearLayout header = this.host.layout().addToHeader(LinearLayout.vertical().spacing(4));
		header.defaultCellSetting().alignHorizontallyCenter();
		header.addChild(new StringWidget(TITLE, this.host.font()));
		EditBox search = header.addChild(new EditBox(this.host.font(), 200, 15, Component.empty()));
		search.setHint(SEARCH_HINT);
		this.host.setSearchBox(search);

		LinearLayout row = header.addChild(LinearLayout.horizontal().spacing(4));
		row.addChild(
			CycleButton.builder((RuleFilter f) -> Component.translatable(f.langKey()), RuleFilter.ALL)
				.withValues(RuleFilter.values())
				.create(0, 0, 120, 20, Component.translatable("bettergamerules.filter"), (button, value) -> {
					BetterRuleList.typeFilter = value;
					this.refilter();
				})
		);
		row.addChild(Button.builder(Component.translatable("bettergamerules.reset_all"), b -> {
			PresetManager.resetAll(this.host.gameRules());
			this.invalidEntries.clear();
			this.updateDoneButton();
			BetterRuleList list = this.host.getRuleList();
			if (list != null) {
				list.refreshValues();
			}
		}).width(80).build());
		row.addChild(Button.builder(Component.translatable("bettergamerules.presets"), b -> this.host.openPresets()).width(90).build());

		contentBuilder.accept(this);

		LinearLayout footer = this.host.layout().addToFooter(LinearLayout.horizontal().spacing(8));
		this.doneButton = footer.addChild(Button.builder(CommonComponents.GUI_DONE, b -> this.host.onDonePressed()).build());
		footer.addChild(Button.builder(CommonComponents.GUI_CANCEL, b -> this.host.onCancelPressed()).build());

		search.setResponder(text -> this.refilter());
		this.host.layout().visitWidgets(this.host::addRenderable);
		this.host.reposition();
		this.updateDoneButton();
	}

	private void refilter() {
		BetterRuleList list = this.host.getRuleList();
		EditBox search = this.host.getSearchBox();
		if (list != null && search != null) {
			list.setFilter(search.getValue());
			this.host.reposition();
		}
	}

	public void attachRuleList(List<GameRule<?>> rules) {
		BetterRuleList list = new BetterRuleList(this.host.minecraft(), this.host.screenWidth(), this.host.layout().getContentHeight(),
			this.host.layout().getHeaderHeight(), this.host.gameRules(), this);
		list.setRules(rules);
		this.host.layout().addToContents(list);
		this.host.setRuleList(list);
	}

	public void attachRuleListLate(List<GameRule<?>> rules) {
		this.attachRuleList(rules);
		this.host.addRenderable(this.host.getRuleList());
		this.host.reposition();
	}

	private void updateDoneButton() {
		if (this.doneButton != null) {
			this.doneButton.active = this.invalidEntries.isEmpty();
		}
	}

	@Override
	public void markInvalid(BetterRuleList.Entry entry) {
		if (entry != null) {
			this.invalidEntries.add(entry);
			this.updateDoneButton();
		}
	}

	@Override
	public void clearInvalid(BetterRuleList.Entry entry) {
		if (entry != null) {
			this.invalidEntries.remove(entry);
			this.updateDoneButton();
		}
	}
}
