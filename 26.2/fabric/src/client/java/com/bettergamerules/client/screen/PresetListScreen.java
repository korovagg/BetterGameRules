package com.bettergamerules.client.screen;

import com.bettergamerules.client.PresetManager;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.gamerules.GameRules;

public class PresetListScreen extends Screen {
	private static final Component TITLE = Component.translatable("bettergamerules.presets.title");
	private static final Component NAME_HINT = Component.translatable("bettergamerules.presets.name_hint").withStyle(EditBox.SEARCH_HINT_STYLE);

	private final Screen parent;
	private final GameRules gameRules;
	private final Runnable onApplied;
	private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 60, 33);
	private PresetList presetList;
	private EditBox nameBox;

	public PresetListScreen(Screen parent, GameRules gameRules, Runnable onApplied) {
		super(TITLE);
		this.parent = parent;
		this.gameRules = gameRules;
		this.onApplied = onApplied;
	}

	@Override
	protected void init() {
		LinearLayout header = this.layout.addToHeader(LinearLayout.vertical().spacing(4));
		header.defaultCellSetting().alignHorizontallyCenter();
		header.addChild(new StringWidget(TITLE, this.font));

		LinearLayout saveRow = header.addChild(LinearLayout.horizontal().spacing(4));
		this.nameBox = saveRow.addChild(new EditBox(this.font, 150, 20, Component.empty()));
		this.nameBox.setHint(NAME_HINT);
		this.nameBox.setMaxLength(48);
		saveRow.addChild(Button.builder(Component.translatable("bettergamerules.presets.save"), b -> {
			try {
				String name = this.nameBox.getValue().trim();
				if (!name.isEmpty()) {
					PresetManager.save(name, this.gameRules);
					this.presetList.rebuild();
				}
			} catch (Exception ignored) {
			}
		}).width(110).build());

		this.presetList = this.layout.addToContents(new PresetList(this.minecraft, this.width, this.layout.getContentHeight(), this.layout.getHeaderHeight()));
		this.presetList.rebuild();

		LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
		footer.addChild(Button.builder(CommonComponents.GUI_BACK, b -> this.onClose()).width(150).build());
		this.layout.visitWidgets(this::addRenderableWidget);
		this.repositionElements();
	}

	@Override
	protected void repositionElements() {
		this.layout.arrangeElements();
		if (this.presetList != null) {
			this.presetList.updateSize(this.width, this.layout);
		}
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(this.parent);
	}

	private void loadPreset(String name) {
		try {
			if (PresetManager.load(name, this.gameRules)) {
				this.onApplied.run();
				this.minecraft.gui.setScreen(this.parent);
			}
		} catch (Exception ignored) {
		}
	}

	private void deletePreset(String name) {
		try {
			PresetManager.delete(name);
			this.presetList.rebuild();
		} catch (Exception ignored) {
		}
	}

	private class PresetList extends ContainerObjectSelectionList<PresetBaseEntry> {
		public PresetList(Minecraft minecraft, int width, int contentHeight, int y0) {
			super(minecraft, width, contentHeight, y0, 26);
		}

		@Override
		public int getRowWidth() {
			return Math.max(220, Math.min(340, this.getWidth() - 16));
		}

		public void rebuild() {
			this.clearEntries();
			List<String> names = PresetManager.listPresets();
			names.sort(String::compareToIgnoreCase);
			for (String name : names) {
				this.addEntry(new PresetEntry(name), 26);
			}
			if (names.isEmpty()) {
				this.addEntry(new EmptyEntry(), 26);
			}
			this.setScrollAmount(0.0);
		}
	}

	private abstract static class PresetBaseEntry extends ContainerObjectSelectionList.Entry<PresetBaseEntry> {
		@Override
		public List<? extends GuiEventListener> children() {
			return List.of();
		}

		@Override
		public List<? extends NarratableEntry> narratables() {
			return List.of();
		}
	}

	private class PresetEntry extends PresetBaseEntry {
		private final String name;
		private final Button loadButton;
		private final Button deleteButton;

			public PresetEntry(String name) {
				this.name = name;
				this.loadButton = Button.builder(Component.translatable("bettergamerules.presets.load"), b -> loadPreset(this.name)).size(70, 20).build();
				this.deleteButton = Button.builder(Component.translatable("bettergamerules.presets.delete"), b -> deletePreset(this.name)).size(70, 20).build();
			}

		@Override
		public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
			Font font = Minecraft.getInstance().font;
			int rowY = this.getContentY() + 3;
			String label = this.name;
			int maxWidth = this.getContentWidth() - 150;
			if (font.width(label) > maxWidth) {
				label = font.plainSubstrByWidth(label, maxWidth - font.width("...")) + "...";
			}
				graphics.text(font, label, this.getContentX(), rowY + 5, 0xFFFFFFFF);
			this.deleteButton.setX(this.getContentRight() - 70);
			this.deleteButton.setY(rowY);
			this.deleteButton.extractRenderState(graphics, mouseX, mouseY, partialTick);
			this.loadButton.setX(this.getContentRight() - 144);
			this.loadButton.setY(rowY);
			this.loadButton.extractRenderState(graphics, mouseX, mouseY, partialTick);
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return List.of(this.loadButton, this.deleteButton);
		}

		@Override
		public List<? extends NarratableEntry> narratables() {
			return List.of(this.loadButton, this.deleteButton);
		}
	}

	private static class EmptyEntry extends PresetBaseEntry {
		@Override
		public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
			Font font = Minecraft.getInstance().font;
				Component text = Component.translatable("bettergamerules.presets.empty").withStyle(ChatFormatting.GRAY);
			graphics.text(font, text, this.getContentXMiddle() - font.width(text) / 2, this.getContentYMiddle() - 4, 0xFF808080);
		}
	}
}
