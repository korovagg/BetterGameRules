package com.bettergamerules.client.mixin;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.InWorldGameRulesScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(InWorldGameRulesScreen.class)
public interface InWorldGameRulesScreenAccessor {
	@Accessor("connection")
	ClientPacketListener bettergamerules$getConnection();

	@Accessor("lastScreen")
	Screen bettergamerules$getLastScreen();
}
