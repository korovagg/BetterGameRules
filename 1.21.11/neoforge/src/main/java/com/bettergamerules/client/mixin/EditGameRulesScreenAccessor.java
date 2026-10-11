package com.bettergamerules.client.mixin;

import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.worldselection.EditGameRulesScreen;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EditGameRulesScreen.class)
public interface EditGameRulesScreenAccessor {
	@Accessor("layout")
	HeaderAndFooterLayout bettergamerules$getLayout();

	@Accessor("gameRules")
	GameRules bettergamerules$getGameRules();

	@Accessor("exitCallback")
	Consumer<Optional<GameRules>> bettergamerules$getExitCallback();
}
