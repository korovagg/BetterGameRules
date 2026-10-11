package com.bettergamerules.client.mixin;

import com.bettergamerules.client.screen.BetterWorldCreationGameRulesScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.EditGameRulesScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Minecraft.class)
public class MinecraftMixin {
	@ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
	private Screen bettergamerules$wrapGameRulesScreen(Screen screen) {
		if (screen == null || screen instanceof BetterWorldCreationGameRulesScreen) {
			return screen;
		}
		if (screen instanceof EditGameRulesScreen) {
			EditGameRulesScreenAccessor accessor = (EditGameRulesScreenAccessor) screen;
			return new BetterWorldCreationGameRulesScreen(accessor.bettergamerules$getGameRules(), accessor.bettergamerules$getExitCallback());
		}
		return screen;
	}
}
