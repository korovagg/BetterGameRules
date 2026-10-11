package com.bettergamerules.client.mixin;

import com.bettergamerules.client.screen.BetterInWorldGameRulesScreen;
import com.bettergamerules.client.screen.BetterWorldCreationGameRulesScreen;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.InWorldGameRulesScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationGameRulesScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Gui.class)
public class GuiMixin {
	@ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
	private Screen bettergamerules$wrapGameRulesScreen(Screen screen) {
		if (screen == null
			|| screen instanceof BetterWorldCreationGameRulesScreen
			|| screen instanceof BetterInWorldGameRulesScreen) {
			return screen;
		}
		if (screen instanceof WorldCreationGameRulesScreen) {
			AbstractGameRulesScreenAccessor accessor = (AbstractGameRulesScreenAccessor) screen;
			return new BetterWorldCreationGameRulesScreen(accessor.bettergamerules$getGameRules(), accessor.bettergamerules$getExitCallback());
		}
		if (screen instanceof InWorldGameRulesScreen) {
			AbstractGameRulesScreenAccessor base = (AbstractGameRulesScreenAccessor) screen;
			InWorldGameRulesScreenAccessor inWorld = (InWorldGameRulesScreenAccessor) screen;
			return new BetterInWorldGameRulesScreen(inWorld.bettergamerules$getConnection(), base.bettergamerules$getExitCallback(),
				inWorld.bettergamerules$getLastScreen());
		}
		return screen;
	}
}
